package com.wts.api.service;

import com.wts.api.dto.PythonResponseDto;
import com.wts.api.dto.StockPriceResponseDto;
import com.wts.api.dto.TradeHistoryUploadDto;
import com.wts.auth.JwtUtil;
import com.wts.kiwoom.dto.KeyDto;
import com.wts.model.*;
import com.wts.util.MapCaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
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
    private final JwtUtil jwtUtil;
    private final MapCaster caster;

    @Value("${external.python-server.timeout:30}")
    private int timeoutSeconds;

    public PythonResponseDto executePostTask(String uri, Map<String, Object> params) {
        try {
            return pythonWebClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(params)
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

    public PythonResponseDto executeGetTask(String uri) {
        try {
            return pythonWebClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(PythonResponseDto.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .doOnError(error -> log.error("Python 서버 호출 실패: ", error))
                    .onErrorReturn(createErrorResponse("서버 통신 오류"))
                    .block();
        } catch (Exception e) {
            log.error("Python 서버 executeGetTask 오류: ", e);
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
                Map<String, Object> dataMap = caster.safeMapCast(response.getData());
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
                Map<String, Object> dataMap = caster.safeMapCast(response.getData());
                String jsonData = (String) dataMap.get("json_data");

                if (jsonData != null) {
                    // TradeHistory로 변환하여 저장
                    return tService.saveTradeHistoryFromJson(jsonData, Long.valueOf(userId));
                } else {
                    return createErrorProcess("거래내역 데이터가 없습니다.");
                }
            } else {
                String msg = response != null ? (response.getMessage().contains("rolled back") ? "거래내역 중복" : "알 수 없는 오류") : "알 수 없는 오류";
                return createErrorProcess("거래내역 업로드 실패: " + msg);
            }
        } catch (Exception e) {
            String msg = e.getMessage().contains("rolled back") ? "거래내역 중복" : "알 수 없는 오류";
            log.error("Python 서버 uploadTradeHistory 오류: ", e);
            return createErrorProcess("거래내역 업로드 실패: " + msg);
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

    public ProcessResult kiwoomLogin(KeyDto dto) {
        String uri = "/kiwoom/login";
        try {
            String appKey = dto.getAppKey();
            String appSec = dto.getAppSecret();
            // JSON 페이로드 구성
            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("appKey", appKey);
            payload.put("secret", appSec);

            PythonResponseDto response = executePostTask(uri, payload);

            if (response != null && response.isSuccess() && response.getData() != null) {
                Map<String, Object> dataMap = caster.safeMapCast(response.getData());
                String returnMsg = caster.safeMapGetString(dataMap, "return_msg");

                //키움 api요청은 이 토큰을 요청에 담아야만 권한이 있다.
                return ProcessResult.builder()
                        .success(true)
                        .message(returnMsg)
                        .data(response.getData())
                        .build();
            } else {
                assert response != null;
                return createErrorProcess("로그인 실패: " + response.getMessage());
            }
        } catch (Exception e) {
            log.error("Python 서버 kiwoomLogin 오류: ", e);
            return createErrorProcess("거래내역 업로드 실패: " + e.getMessage());
        }
    }

    public ProcessResult kiwoomLogout(KeyDto dto) {
        String uri = "/kiwoom/logout";
        try {
            String appKey = dto.getAppKey();
            String appSec = dto.getAppSecret();
            String token = dto.getToken();
            // JSON 페이로드 구성
            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("appKey", appKey);
            payload.put("secret", appSec);
            payload.put("token", token);

            PythonResponseDto response = executePostTask(uri, payload);

            if (response != null && response.isSuccess() && response.getData() != null) {
                Map<String, Object> dataMap = caster.safeMapCast(response.getData());

                return ProcessResult.builder()
                        .success(true)
                        .message(dataMap.get("return_msg").toString())
                        .data(response.getData())
                        .build();
            } else {
                return createErrorProcess("로그아웃 실패: " + (response != null ? response.getMessage() : "알 수 없는 오류"));
            }
        } catch (Exception e) {
            log.error("Python 서버 로그아웃 오류: ", e);
            return createErrorProcess("로그아웃 실패: " + e.getMessage());
        }
    }

    public String verifyKiwoomTokenAndCreateJwt(String kiwoomToken) {
        try {
            if (kiwoomToken == null || kiwoomToken.isBlank()) {
                log.warn("verifyKiwoomTokenAndCreateJwt called with blank token");
                return null;
            }

            // kiwoomAPI로부터 계좌상세정보를 불러올 수 있으면 유효한 토큰으로 간주한다.
            String uri = "/kiwoom/getBalanceDetail";
            PythonResponseDto dto = executePostTask(uri, Map.of("token", kiwoomToken));

            if (dto == null) {
                log.warn("Kiwoom probe response is null");
                return null;
            }

            if(dto.getData() != null) {
                Map<String, Object> data = caster.safeMapCast(dto.getData());
                if (data != null && data.containsKey("return_code")) {
                    try {
                        Integer code = Integer.parseInt(data.get("return_code").toString());
                        if (isSuccess(code)) {
                            String subject = "kiwoom-user";
                            String jwt = jwtUtil.createToken(subject);
                            log.info("Kiwoom token validated via probe. Issued internal JWT for sub={}", subject);
                            return jwt;
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Invalid return_code format: {}", data.get("return_code"));
                    }
                } else {
                    log.warn("Response data is not a valid Map or missing return_code");
                }
            }

            return null;
        } catch (WebClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            int http = status.value();
            if (http == 401 || http == 403) {
                log.info("Kiwoom token invalid by HTTP status {}", http);
                return null;
            }
            log.warn("Kiwoom probe HTTP error: {} - {}", http, e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("Kiwoom probe failed: {}", e.toString());
            return null;
        }
    }

    private boolean isSuccess(Integer returnCode) {
        if (returnCode == null) return false;
        // 기본 성공 코드는 "0"으로 가정. 필요 시 환경설정으로 치환 가능
        return 0 == returnCode;
    }
}
