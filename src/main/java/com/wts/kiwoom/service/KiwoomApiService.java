package com.wts.kiwoom.service;

import com.wts.api.dto.PythonResponseDto;
import com.wts.api.service.PythonServerService;
import com.wts.auth.JwtUtil;
import com.wts.kiwoom.dto.KiwoomApiRequest;
import com.wts.kiwoom.dto.StockDetailInfo;
import com.wts.kiwoom.dto.StockDto;
import com.wts.kiwoom.entity.KiwoomStock;
import com.wts.kiwoom.entity.UserWatchGroup;
import com.wts.kiwoom.entity.UserWatchListItem;
import com.wts.kiwoom.repository.KiwoomStockRepository;
import com.wts.kiwoom.repository.UserWatchGroupRepository;
import com.wts.kiwoom.repository.UserWatchListItemRepository;
import com.wts.model.ProcessResult;
import com.wts.util.MapCaster;
import com.wts.util.UtilsForRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KiwoomApiService {
    private final PythonServerService pythonService;
    private final JwtUtil jwtUtil;
    private final KiwoomTokenManager kiwoomTokenManager;
    private final MapCaster caster;
    private final KiwoomStockRepository stockRepo;
    private final UserWatchGroupRepository userWatchGroupRepository;
    private final UserWatchListItemRepository userWatchListItemRepository;
    private final UtilsForRequest uRequest;

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

    public ProcessResult getAllStockCodeName() {
        try {
            List<KiwoomStock> stocks = stockRepo.findAll();
            List<StockDto> stockDtos = stocks.stream()
                    .map(KiwoomStock::toDto)
                    .toList();
            return ProcessResult.builder()
                    .success(true)
                    .data(stocks)
                    .message("키움 종목 조회 완료")
                    .build();
        } catch (Exception e) {
            log.error("키움 종목 조회 실패: ", e);
            return ProcessResult.builder()
                    .success(false)
                    .message("키움 종목 조회 실패: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 사용자 관심종목 동기화 (그룹 기반)
     * @param userId 사용자 ID
     * @param groupName 그룹명
     * @param stockCodes 설정할 종목 코드 리스트
     * @return 처리 결과
     */
    @Transactional
    public ProcessResult syncUserWatchList(long userId, String stringGroupId, String groupName, List<String> stockCodes) {
        try {
            UserWatchGroup watchGroup;
            long groupId;
            //신규생성 그룹인지 구분.
            if(stringGroupId.startsWith("group_")){
                // 신규 그룹 생성 모드
                // 그룹명 중복 체크
                if (userWatchGroupRepository.existsByUserIdAndGroupName(userId, groupName)) {
                    return ProcessResult.builder()
                            .success(false)
                            .message("이미 존재하는 그룹명입니다: " + groupName)
                            .build();
                }

                Integer nextOrder = userWatchGroupRepository.findNextDisplayOrder(userId);
                watchGroup = UserWatchGroup.builder()
                        .userId(userId)
                        .groupName(groupName)
                        .displayOrder(nextOrder)
                        .build();

                log.info("신규 그룹 생성: userId={}, groupName='{}'", userId, groupName);
            } else {
                groupId = Long.parseLong(stringGroupId);
                // 기존 그룹 수정 모드
                Optional<UserWatchGroup> existingGroupOpt = userWatchGroupRepository.findById(groupId);

                if (existingGroupOpt.isPresent()) {
                    watchGroup = existingGroupOpt.get();

                    // 사용자 권한 체크
                    if (watchGroup.getUserId() != userId) {
                        return ProcessResult.builder()
                                .success(false)
                                .message("해당 그룹에 대한 권한이 없습니다.")
                                .build();
                    }

                    // 그룹명 변경 처리 (중복 검사)
                    if (!watchGroup.getGroupName().equals(groupName)) {
                        boolean groupNameExists = userWatchGroupRepository
                                .existsByUserIdAndGroupNameAndIdNot(userId, groupName, groupId);

                        if (groupNameExists) {
                            return ProcessResult.builder()
                                    .success(false)
                                    .message("이미 존재하는 그룹명입니다: " + groupName)
                                    .build();
                        }

                        // 그룹명 업데이트
                        log.info("그룹명 변경: groupId={}, 기존명='{}', 신규명='{}'",
                                groupId, watchGroup.getGroupName(), groupName);

                        watchGroup.setGroupName(groupName);
                    }
                } else {
                    return ProcessResult.builder()
                            .success(false)
                            .message("존재하지 않는 그룹입니다: groupId=" + groupId)
                            .build();
                }
            }

            // 그룹 저장 (신규 생성인 경우)
            watchGroup = userWatchGroupRepository.save(watchGroup);

            // 기존 아이템들과 새로운 종목 코드 세트 비교
            List<UserWatchListItem> existingItems = userWatchListItemRepository.findByWatchGroup(watchGroup);
            Set<String> newStockCodeSet = new HashSet<>(stockCodes);
            Set<String> existingStockCodeSet = existingItems.stream()
                    .map(UserWatchListItem::getStockCd)
                    .collect(Collectors.toSet());

            // 삭제할 아이템들 처리 (새 목록에 없는 기존 종목들)
            List<UserWatchListItem> itemsToDelete = existingItems.stream()
                    .filter(item -> !newStockCodeSet.contains(item.getStockCd()))
                    .toList();

            for (UserWatchListItem item : itemsToDelete) {
                userWatchListItemRepository.delete(item);
            }

            if (!itemsToDelete.isEmpty()) {
                userWatchListItemRepository.flush(); // 삭제 즉시 반영
            }

            int savedCount = 0;
            int displayOrder = 1;

            for (String stockCode : stockCodes) {
                // 종목 코드 유효성 검증
                Optional<KiwoomStock> stock = stockRepo.findByStockCd(stockCode);
                if (stock.isEmpty()) {
                    log.warn("유효하지 않은 종목 코드: {}", stockCode);
                    continue;
                }

                if (existingStockCodeSet.contains(stockCode)) {
                    // 기존 아이템 업데이트 (displayOrder만 조정)
                    Optional<UserWatchListItem> existingItem = existingItems.stream()
                            .filter(item -> item.getStockCd().equals(stockCode))
                            .findFirst();

                    if (existingItem.isPresent()) {
                        UserWatchListItem item = existingItem.get();
                        item.setDisplayOrder(displayOrder++);
                        userWatchListItemRepository.save(item);
                        savedCount++;
                    }
                } else {
                    // 새 아이템 추가
                    UserWatchListItem watchItem = UserWatchListItem.builder()
                            .watchGroup(watchGroup)
                            .stockCd(stockCode)
                            .displayOrder(displayOrder++)
                            .build();

                    userWatchListItemRepository.save(watchItem);
                    savedCount++;
                }
            }
            groupId = watchGroup.getId();
            return ProcessResult.builder()
                    .success(true)
                    .message(String.format("관심종목 동기화 완료: 그룹 '%s'에 총 %d개 처리됨", groupName, savedCount))
                    .data(Map.of("groupId", groupId, "groupName", groupName, "itemCount", savedCount))
                    .build();

        } catch (Exception e) {
            log.error("관심종목 동기화 실패: userId={}, groupName={}", userId, groupName, e);
            return ProcessResult.builder()
                    .success(false)
                    .message("관심종목 동기화 실패: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 사용자 관심종목 조회 (그룹 기반)
     * @param jwt jwt 토큰
     * @return 그룹별 관심종목 리스트
     */
    public ProcessResult getUserWatchList(String jwt) {
        long userId = Long.parseLong(jwtUtil.getSubject(jwt));
        try {
            String kiwoomToken = uRequest.getKiwoomTokenFromJwt(jwt);
            // 그룹별 관심종목 조회
            List<UserWatchGroup> watchGroups = userWatchGroupRepository.findByUserIdWithItems(userId);

            // 모든 종목코드 수집 (중복 제거)
            Set<String> allStockCodes = watchGroups.stream()
                    .flatMap(group -> group.getWatchItems().stream())
                    .map(UserWatchListItem::getStockCd)
                    .collect(Collectors.toSet());

            log.info("관심종목 조회 시작: userId={}, 그룹수={}, 종목수={}",
                    userId, watchGroups.size(), allStockCodes.size());

            // 종목 정보 한번에 조회 (N+1 쿼리 문제 해결)
            Map<String, KiwoomStock> stockMap = stockRepo.findByStockCdIn(new ArrayList<>(allStockCodes))
                    .stream()
                    .collect(Collectors.toMap(KiwoomStock::getStockCd, Function.identity()));

            // 파이썬 서비스에서 가격 정보 조회
            String stockCodes = String.join("|", allStockCodes);
            log.debug("파이썬 서비스 호출 시작: stockCodes={}", stockCodes);

            ProcessResult rr = pythonService.getStockListInfo(stockCodes, kiwoomToken);

            log.info("파이썬 서비스 응답 완료: success={}, dataExists={}", rr.isSuccess(), rr.getData() != null);

            // 종목코드별 가격 정보 맵 생성
            Map<String, StockDetailInfo> priceInfoMap = new HashMap<>();
            if (rr.isSuccess() && rr.getData() != null) {
                Map<String, Object> responseData = caster.safeMapCast(rr.getData());
                if (responseData != null && responseData.containsKey("atn_stk_infr")) {
                    Object stockInfoData = responseData.get("atn_stk_infr");

                    // 단일 종목인지 복수 종목인지 확인 후 처리
                    if (stockInfoData instanceof Map) {
                        // 단일 종목 처리
                        Map<String, Object> singleStock = caster.safeMapCast(stockInfoData);
                        StockDetailInfo info = buildStockDetailInfo(singleStock);
                        if (info != null && info.getStockCd() != null) {
                            priceInfoMap.put(info.getStockCd(), info);
                        }
                    } else if (stockInfoData instanceof List) {
                        // 복수 종목 처리
                        List<Object> stockList = (List<Object>) stockInfoData;
                        for (Object stockObj : stockList) {
                            Map<String, Object> stockData = caster.safeMapCast(stockObj);
                            if (stockData != null) {
                                StockDetailInfo info = buildStockDetailInfo(stockData);
                                if (info != null && info.getStockCd() != null) {
                                    priceInfoMap.put(info.getStockCd(), info);
                                }
                            }
                        }
                    }
                }
            }

            // 그룹별 데이터 구성
            List<Map<String, Object>> result = watchGroups.stream()
                    .map(group -> {
                        Map<String, Object> groupData = new HashMap<>();
                        groupData.put("groupId", group.getId());
                        groupData.put("groupName", group.getGroupName());
                        groupData.put("description", group.getDescription());
                        groupData.put("displayOrder", group.getDisplayOrder());
                        groupData.put("createdAt", group.getCreatedAt());

                        // 그룹 내 종목 리스트
                        List<Map<String, Object>> items = group.getWatchItems().stream()
                                .map(item -> {
                                    Map<String, Object> itemData = new HashMap<>();
                                    itemData.put("itemId", item.getId());
                                    itemData.put("stockCd", item.getStockCd());
                                    itemData.put("displayOrder", item.getDisplayOrder());
                                    itemData.put("memo", item.getMemo());
                                    itemData.put("createdAt", item.getCreatedAt());

                                    // 종목 정보 추가
                                    Optional<KiwoomStock> stock = stockRepo.findByStockCd(item.getStockCd());
                                    if (stock.isPresent()) {
                                        itemData.put("stockNm", stock.get().getStockNm());
                                        itemData.put("market", stock.get().getMarket());
                                    }

                                    // 가격 정보 추가
                                    StockDetailInfo priceInfo = priceInfoMap.get(item.getStockCd());
                                    if (priceInfo != null) {
                                        itemData.put("nowPrice", priceInfo.getNowPrice());
                                        itemData.put("yesterdayClosePrice", priceInfo.getYesterdayClosePrice());
                                        itemData.put("changePrice", priceInfo.getChangePrice());
                                        itemData.put("changeDirection", priceInfo.getChangeDirection());
                                        itemData.put("changeRate", priceInfo.getChangeRate());
                                        itemData.put("tradeVolume", priceInfo.getTradeVolume());
                                        itemData.put("tradeValue", priceInfo.getTradeValue());
                                        itemData.put("highPrice", priceInfo.getHighPrice());
                                        itemData.put("lowPrice", priceInfo.getLowPrice());
                                        itemData.put("openingPrice", priceInfo.getOpeningPrice());
                                        itemData.put("closePrice", priceInfo.getClosePrice());
                                        itemData.put("upLimitPrice", priceInfo.getUpLimitPrice());
                                        itemData.put("lowLimitPrice", priceInfo.getLowLimitPrice());
                                    }

                                    return itemData;
                                })
                                .toList();

                        groupData.put("items", items);
                        groupData.put("itemCount", items.size());

                        return groupData;
                    })
                    .toList();

            return ProcessResult.builder()
                    .success(true)
                    .message(String.format("관심종목 조회 완료: 총 %d개 그룹", result.size()))
                    .data(result)
                    .build();

        } catch (Exception e) {
            log.error("관심종목 조회 실패: userId={}", userId, e);
            return ProcessResult.builder()
                    .success(false)
                    .message("관심종목 조회 실패: " + e.getMessage())
                    .build();
        }
    }

    /**
     * StockDetailInfo 객체 생성 헬퍼 메서드
     */
    private StockDetailInfo buildStockDetailInfo(Map<String, Object> stockInfoData) {
        if (stockInfoData == null) return null;

        try {
            return StockDetailInfo.builder()
                    .stockCd(String.valueOf(stockInfoData.get("stk_cd")))
                    .stockNm(String.valueOf(stockInfoData.get("stk_nm")))
                    .nowPrice(BigDecimal.valueOf(Long.parseLong(String.valueOf(stockInfoData.get("cur_prc")))))
                    .yesterdayClosePrice(BigDecimal.valueOf(Long.parseLong(String.valueOf(stockInfoData.get("base_pric")))))
                    .changePrice(BigDecimal.valueOf(Long.parseLong(String.valueOf(stockInfoData.get("pred_pre")))))
                    .changeDirection(BigDecimal.valueOf(Long.parseLong(String.valueOf(stockInfoData.get("pred_pre_sig")))))
                    .changeRate(String.valueOf(stockInfoData.get("flu_rt")))
                    .tradeVolume(BigDecimal.valueOf(Long.parseLong(String.valueOf(stockInfoData.get("trde_qty")))))
                    .tradeValue(BigDecimal.valueOf(Long.parseLong(String.valueOf(stockInfoData.get("trde_prica")))))
                    .highPrice(BigDecimal.valueOf(Long.parseLong(String.valueOf(stockInfoData.get("high_pric")))))
                    .lowPrice(BigDecimal.valueOf(Long.parseLong(String.valueOf(stockInfoData.get("low_pric")))))
                    .openingPrice(BigDecimal.valueOf(Long.parseLong(String.valueOf(stockInfoData.get("open_pric")))))
                    .closePrice(BigDecimal.valueOf(Long.parseLong(String.valueOf(stockInfoData.get("close_pric")))))
                    .upLimitPrice(BigDecimal.valueOf(Long.parseLong(String.valueOf(stockInfoData.get("upl_pric")))))
                    .lowLimitPrice(BigDecimal.valueOf(Long.parseLong(String.valueOf(stockInfoData.get("lst_pric")))))
                    .build();
        } catch (Exception e) {
            log.warn("StockDetailInfo 생성 실패: {}", e.getMessage());
            return null;
        }
    }



    /**
     * 관심종목 그룹 생성
     */
    @Transactional
    public ProcessResult createWatchGroup(long userId, String groupName, String description) {
        try {
            // 그룹명 중복 체크
            if (userWatchGroupRepository.existsByUserIdAndGroupName(userId, groupName)) {
                return ProcessResult.builder()
                        .success(false)
                        .message("이미 존재하는 그룹명입니다: " + groupName)
                        .build();
            }

            Integer nextOrder = userWatchGroupRepository.findNextDisplayOrder(userId);
            UserWatchGroup watchGroup = UserWatchGroup.builder()
                    .userId(userId)
                    .groupName(groupName)
                    .description(description)
                    .displayOrder(nextOrder)
                    .build();

            watchGroup = userWatchGroupRepository.save(watchGroup);

            return ProcessResult.builder()
                    .success(true)
                    .message("관심종목 그룹이 생성되었습니다: " + groupName)
                    .data(Map.of("groupId", watchGroup.getId(), "groupName", groupName))
                    .build();

        } catch (Exception e) {
            log.error("관심종목 그룹 생성 실패: userId={}, groupName={}", userId, groupName, e);
            return ProcessResult.builder()
                    .success(false)
                    .message("관심종목 그룹 생성 실패: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 관심종목 그룹 삭제
     */
    @Transactional
    public ProcessResult deleteWatchGroup(long userId, long groupId) {
        try {
            Optional<UserWatchGroup> watchGroup = userWatchGroupRepository.findById(groupId);
            if (watchGroup.isEmpty()) {
                return ProcessResult.builder()
                        .success(false)
                        .message("존재하지 않는 그룹입니다: " + groupId)
                        .build();
            }

            userWatchGroupRepository.delete(watchGroup.get());

            return ProcessResult.builder()
                    .success(true)
                    .message("관심종목 그룹이 삭제되었습니다: " + watchGroup.get().getGroupName())
                    .build();

        } catch (Exception e) {
            log.error("관심종목 그룹 삭제 실패: userId={}, groupId={}", userId, groupId, e);
            return ProcessResult.builder()
                    .success(false)
                    .message("관심종목 그룹 삭제 실패: " + e.getMessage())
                    .build();
        }
    }

    public ProcessResult addUserWatchListItem(long userId, List<String> stockCodes, String groupName) {
        try {
            if(groupName == null || groupName.trim().isEmpty()) {
                return ProcessResult.builder()
                        .success(false)
                        .message("그룹명이 필요합니다.")
                        .build();
            }
            // 그룹 조회 또는 생성
            UserWatchGroup watchGroup = userWatchGroupRepository.findByUserIdAndGroupName(userId, groupName)
                    .orElseGet(() -> {
                        Integer nextOrder = userWatchGroupRepository.findNextDisplayOrder(userId);
                        return UserWatchGroup.builder()
                                .userId(userId)
                                .groupName(groupName)
                                .displayOrder(nextOrder)
                                .build();
                    });

            // 그룹 저장 (신규 생성인 경우)
            watchGroup = userWatchGroupRepository.save(watchGroup);

            int savedCount = 0;
            int displayOrder = userWatchListItemRepository.findNextDisplayOrder(watchGroup);

            for (String stockCode : stockCodes) {
                // 종목 코드 유효성 검증
                Optional<KiwoomStock> stock = stockRepo.findByStockCd(stockCode);
                if (stock.isEmpty()) {
                    log.warn("유효하지 않은 종목 코드: {}", stockCode);
                    continue;
                }

                // 관심종목 아이템 추가
                UserWatchListItem watchItem = UserWatchListItem.builder()
                        .watchGroup(watchGroup)
                        .stockCd(stockCode)
                        .displayOrder(displayOrder++)
                        .build();

                userWatchListItemRepository.save(watchItem);
                savedCount++;
            }

            return ProcessResult.builder()
                    .success(true)
                    .message(String.format("관심종목 추가 완료: 그룹 '%s'에 총 %d개 저장됨", groupName, savedCount))
                    .data(Map.of("groupName", groupName, "itemCount", savedCount))
                    .build();

        } catch (Exception e) {
            log.error("관심종목 추가 실패: userId={}, groupName={}", userId, groupName, e);
            return ProcessResult.builder()
                    .success(false)
                    .message("관심종목 추가 실패: " + e.getMessage())
                    .build();
        }
    }

    public ProcessResult removeUserWatchListItem(long userId, String groupName, String stockCode) {
        try {
            Optional<UserWatchGroup> watchGroupOpt = userWatchGroupRepository.findByUserIdAndGroupName(userId, groupName);
            if (watchGroupOpt.isEmpty()) {
                return ProcessResult.builder()
                        .success(false)
                        .message("존재하지 않는 그룹입니다: " + groupName)
                        .build();
            }
            UserWatchGroup watchGroup = watchGroupOpt.get();

            Optional<UserWatchListItem> watchItemOpt = userWatchListItemRepository.findByWatchGroupAndStockCd(watchGroup, stockCode);
            if (watchItemOpt.isEmpty()) {
                return ProcessResult.builder()
                        .success(false)
                        .message("그룹에 존재하지 않는 종목입니다: " + stockCode)
                        .build();
            }

            userWatchListItemRepository.delete(watchItemOpt.get());

            return ProcessResult.builder()
                    .success(true)
                    .message("관심종목이 삭제되었습니다: " + stockCode)
                    .build();

        } catch (Exception e) {
            log.error("관심종목 삭제 실패: userId={}, groupName={}, stockCode={}", userId, groupName, stockCode, e);
            return ProcessResult.builder()
                    .success(false)
                    .message("관심종목 삭제 실패: " + e.getMessage())
                    .build();
        }
    }

}
