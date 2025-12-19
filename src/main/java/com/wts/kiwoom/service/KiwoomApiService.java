package com.wts.kiwoom.service;

import com.wts.api.dto.PythonResponseDto;
import com.wts.api.entity.PortfolioItem;
import com.wts.api.service.PythonServerService;
import com.wts.auth.JwtUtil;
import com.wts.kiwoom.dto.KiwoomApiRequest;
import com.wts.kiwoom.entity.KiwoomStock;
import com.wts.kiwoom.repository.KiwoomStockRepository;
import com.wts.model.ProcessResult;
import com.wts.util.MapCaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KiwoomApiService {
    private final PythonServerService pythonService;
    private final JwtUtil jwtUtil;
    private final KiwoomTokenManager kiwoomTokenManager;
    private final MapCaster caster;
    private final KiwoomStockRepository stockRepo;

    public ProcessResult getAccountInfo(KiwoomApiRequest req) {
        try {

            return null;
        } catch (Exception e) {
            log.error("키움 로그인 실패: ", e);
            return ProcessResult.builder()
                    .success(false)
                    .message("키움 로그인 실패: " + e.getMessage())
                    .build();
        }
    }

    public Optional<String> getKiwoomToken(String jwt) {
        long userId = Long.parseLong(jwtUtil.getSubject(jwt));
        String tokenId = jwtUtil.getKiwoomTokenRef(jwt);

        Optional<String> kiwoomToken = kiwoomTokenManager.getKiwoomToken(userId, tokenId);

        if (kiwoomToken.isEmpty()) {
            String msg = String.format("키움 토큰을 찾을 수 없음: userId=%d", userId);
            log.warn(msg);
            return Optional.empty();
        }

        return kiwoomToken;
    }

    public ProcessResult syncKiwoomStocks() {
        try {
            PythonResponseDto response = pythonService.execute32PostTask("/kiwoom/getMarketCodes", null);
            if (!response.isSuccess()) {
                return ProcessResult.builder()
                        .success(false)
                        .message("키움 종목 동기화 실패: " + response.getMessage())
                        .build();
            }

            // response.getData()를 안전하게 List로 변환
            Object dataObject = response.getData();
            if (dataObject == null) {
                return ProcessResult.builder()
                        .success(false)
                        .message("응답 데이터가 null입니다.")
                        .build();
            }

            // ArrayList인 경우 직접 캐스팅, 아닌 경우 MapCaster 활용
            List<Object> dataList;
            if (dataObject instanceof List) {
                dataList = (List<Object>) dataObject;
            } else {
                log.warn("응답 데이터가 List 타입이 아닙니다. 타입: {}", dataObject.getClass().getSimpleName());
                return ProcessResult.builder()
                        .success(false)
                        .message("응답 데이터 형식이 올바르지 않습니다.")
                        .build();
            }

            int savedCount = 0;
            for (Object dataObj : dataList) {
                try {
                    // 각 데이터 객체를 맵으로 캐스팅
                    Map<String, Object> dataMap = caster.safeMapCast(dataObj);
                    if (dataMap == null) {
                        log.warn("데이터 객체를 Map으로 변환할 수 없습니다: {}", dataObj);
                        continue;
                    }

                    String stockCode = caster.safeMapGetString(dataMap, "stockCd");
                    String stockName = caster.safeMapGetString(dataMap, "stockNm");
                    String market = caster.safeMapGetString(dataMap, "market");

                    // 필수 데이터 검증
                    if (stockCode == null || stockCode.trim().isEmpty()) {
                        log.warn("종목 코드가 없습니다: {}", dataMap);
                        continue;
                    }

                    if (stockName == null || stockName.trim().isEmpty()) {
                        log.warn("종목명이 없습니다: {}", dataMap);
                        continue;
                    }

                    Optional<KiwoomStock> existing = stockRepo.findByStockCd(stockCode);
                    if(existing.isPresent()) {
                        // 기존 엔티티 업데이트
                        KiwoomStock e = existing.get();
                        e.setStockNm(stockName);
                        e.setMarket(market);

                        // 다른 필드들도 필요시 업데이트
                        stockRepo.save(e); // UPDATE 실행
                    } else {
                        KiwoomStock stock = KiwoomStock.builder()
                                .stockCd(stockCode.trim())
                                .stockNm(stockName.trim())
                                .market(market != null ? market.trim() : null)
                                .build();
                        stockRepo.save(stock);
                    }

                    savedCount++;

                } catch (Exception e) {
                    log.error("종목 데이터 처리 중 오류 발생: {}", dataObj, e);
                    // 개별 데이터 처리 실패는 전체 프로세스를 중단시키지 않음
                }
            }

            return ProcessResult.builder()
                    .success(true)
                    .message(String.format("키움 종목 동기화 완료: 총 %d개 처리됨", savedCount))
                    .build();

        } catch (Exception e) {
            log.error("키움 종목 동기화 실패: ", e);
            return ProcessResult.builder()
                    .success(false)
                    .message("키움 종목 동기화 실패: " + e.getMessage())
                    .build();
        }
    }

}
