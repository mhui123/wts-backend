package com.wts.service;

import com.wts.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PythonServerService {

    private final WebClient pythonWebClient;
    private final TradeHistoryService tService;

    @Value("${external.python-server.timeout:30}")
    private int timeoutSeconds;

    public PythonResponseDto executeTask(String uri, PythonRequestDto request) {
        try {
            return pythonWebClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(PythonResponseDto.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .doOnError(error -> log.error("Python 서버 호출 실패: ", error))
                    .onErrorReturn(createErrorResponse("서버 통신 오류"))
                    .block();
        } catch (Exception e) {
            log.error("Python 서버 executeTask 오류: ", e);
            return createErrorResponse("작업 실행 실패: " + e.getMessage());
        }
    }

    public String checkHealth() {
        try {
            return pythonWebClient.get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
        } catch (WebClientResponseException e) {
            throw new RuntimeException("Python 서버 응답 오류: " + e.getStatusCode());
        } catch (Exception e) {
            throw new RuntimeException("Python 서버 연결 실패: " + e.getMessage());
        }
    }

    private PythonResponseDto createErrorResponse(String message) {
        return PythonResponseDto.builder()
                .success(false)
                .message(message)
                .build();
    }

    private ProcessResult createErrorProcess(String message) {
        return ProcessResult.builder()
                .success(false)
                .message(message)
                .build();
    }

    /**
     * Python 서버에서 티커 정보 조회 (GET)
     */
    public ProcessResult getTicker(String isin) {
        try {
            String uri = "/wpy/getTicker";
            if (isin != null && !isin.isEmpty()) {
                uri += "?isin=" + isin;
            } else {
                return createErrorProcess("ISIN 값이 필요합니다.");
            }

            PythonResponseDto response = pythonWebClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(PythonResponseDto.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .doOnError(error -> log.error("Python 서버 getTicker 호출 실패: ", error))
                    .onErrorReturn(createErrorResponse("티커 조회 서버 통신 오류"))
                    .block();
            if (response != null && response.isSuccess() && response.getData() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) response.getData();
                String ticker = (String) dataMap.get("ticker");
                return ProcessResult.builder()
                        .success(true)
                        .message(ticker)
                        .build();
            } else {
                return createErrorProcess("티커 조회 실패: " + (response != null ? response.getMessage() : "알 수 없는 오류"));
            }
        } catch (Exception e) {
            log.error("Python 서버 getTicker 오류: ", e);
            return createErrorProcess("티커 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 거래내역을 Python 서버로 업로드 (POST)
     */
    public ProcessResult uploadTradeHistory(TradeHistoryUploadDto uploadDto) {
        try {
            String userId = uploadDto.getUserId();
            PythonResponseDto response = pythonWebClient.post()
                    .uri("/wpy/uploadTradeHistory")
                    .body(BodyInserters.fromMultipartData("file", uploadDto.getFile().getResource())
                            .with("userId", userId))
                    .retrieve()
                    .bodyToMono(PythonResponseDto.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .doOnError(error -> log.error("Python 서버 uploadTradeHistory 호출 실패: ", error))
                    .onErrorReturn(createErrorResponse("거래내역 업로드 서버 통신 오류"))
                    .block();

            if (response != null && response.isSuccess() && response.getData() != null) {
                // data를 Map으로 캐스팅
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) response.getData();
                String jsonData = (String) dataMap.get("json_data");

                if (jsonData != null) {
                    // TradeHistory로 변환하여 저장
                    return tService.saveTradeHistoryFromJson(jsonData, Long.valueOf(userId));
                } else {
                    return createErrorProcess("거래내역 데이터가 없습니다.");
                }
            } else {
                return createErrorProcess("거래내역 업로드 실패: " + (response != null ? response.getMessage() : "알 수 없는 오류"));
            }
        } catch (Exception e) {
            log.error("Python 서버 uploadTradeHistory 오류: ", e);
            return createErrorProcess("거래내역 업로드 실패: " + e.getMessage());
        }
    }

    public StockPriceResponseDto getStockPrice(String symbols) {
        try {
            String uri = "/wpy/stock/prices";
            if (symbols != null && !symbols.isEmpty()) {
                uri += "?symbols=" + symbols;
            }
            return pythonWebClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(StockPriceResponseDto.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .doOnError(error -> log.error("Python 서버 getTicker 호출 실패: ", error))
                    .onErrorReturn(new StockPriceResponseDto())
                    .block();
        } catch (Exception e) {
            log.error("Python 서버 주가 조회 오류: ", e);
            return new StockPriceResponseDto();  // 에러 시 빈 객체 반환
        }
    }
}
