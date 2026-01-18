package com.wts.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wts.api.dto.PythonResponseDto;
import com.wts.api.dto.StockPriceResponseDto;
import com.wts.api.dto.TradeHistoryUploadDto;
import com.wts.auth.JwtUtil;
import com.wts.kiwoom.dto.KeyDto;
import com.wts.kiwoom.dto.WatchListDto;
import com.wts.model.*;
import com.wts.util.MapCaster;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class PythonServerService {

    private final WebClient pythonWebClient;
    private final WebClient py32WebClient;
    private final TradeHistoryService tService;
    private final JwtUtil jwtUtil;
    private final MapCaster caster;

    public PythonServerService(
            @Qualifier("pythonWebClient") WebClient pythonWebClient,
            @Qualifier("py32WebClient") WebClient py32WebClient,
            TradeHistoryService tService,
            JwtUtil jwtUtil,
            MapCaster caster) {
        this.pythonWebClient = pythonWebClient;
        this.py32WebClient = py32WebClient;
        this.tService = tService;
        this.jwtUtil = jwtUtil;
        this.caster = caster;
    }

    @Value("${external.python-server.timeout:30}")
    private int timeoutSeconds;

    public PythonResponseDto execute32PostTask(String uri, Map<String, Object> params) {
        try {
            if(params == null) {
                params = Map.of();
            }
            return py32WebClient.post()
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

    public ProcessResult executeGetTask(String uri) {
        return executeGetTask(uri, null);
    }

    public ProcessResult executeGetTask(String uri, Map<String, Object> params) {
        try {
            // 먼저 JSON String으로 받아서 로그로 확인
            String jsonResponse = pythonWebClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(uri);
                        if (params != null) {
                            params.forEach((key, value) -> {
                                if (value != null) {
                                    uriBuilder.queryParam(key, value);
                                }
                            });
                        }
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .doOnError(error -> log.error("Python 서버 호출 실패: ", error))
                    .onErrorReturn("{\"success\": false, \"message\": \"서버 통신 오류\"}")
                    .block();

            log.debug("Python response JSON: {}", jsonResponse);

            // JSON String을 ProcessResult로 변환
            ObjectMapper mapper = new ObjectMapper();
            ProcessResult result = mapper.readValue(jsonResponse, ProcessResult.class);
            log.debug("Parsed ProcessResult: success={}, message={}, data={}, dataType={}",
                result.isSuccess(), result.getMessage(), result.getData(),
                result.getData() != null ? result.getData().getClass().getName() : "null");

            return result;
        } catch (Exception e) {
            log.error("Python 서버 executeGetTask 오류: ", e);
            return createErrorProcess("작업 실행 실패: " + e.getMessage());
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
            if (isin == null || isin.isEmpty()) {
                return createErrorProcess("ISIN 값이 필요합니다.");
            }

            Map<String, Object> params = Map.of("isin", isin);
            ProcessResult result = executeGetTask("/wpy/getTicker", params);

            if (result != null && result.isSuccess() && result.getData() != null) {
                Map<String, Object> dataMap = caster.safeMapCast(result.getData());
                String ticker = (String) dataMap.get("ticker");
                return ProcessResult.builder()
                        .success(true)
                        .message(ticker)
                        .build();
            } else {
                return createErrorProcess("티커 조회 실패: " + (result != null ? result.getMessage() : "알 수 없는 오류"));
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

    public ProcessResult getStockInfo(String stockCd) {
        String uri = "/kiwoom/stockInfo";
        try {
            // JSON 페이로드 구성
            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("stockCd", stockCd);

            PythonResponseDto response = executePostTask(uri, payload);

            if (response != null && response.isSuccess() && response.getData() != null) {
                Map<String, Object> dataMap = caster.safeMapCast(response.getData());

                return ProcessResult.builder()
                        .success(true)
                        .message("주식 정보 조회 성공")
                        .data(dataMap)
                        .build();
            } else {
                return createErrorProcess("주식 정보 조회 실패: " + (response != null ? response.getMessage() : "알 수 없는 오류"));
            }
        } catch (Exception e) {
            log.error("Python 서버 getStockInfo 오류: ", e);
            return createErrorProcess("주식 정보 조회 실패: " + e.getMessage());
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

    public ProcessResult getStockListInfo(String stockCodes, String token) {
        String uri = "/kiwoom/stockInfo";
        try {
            // JSON 페이로드 구성
            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("token", token);
            payload.put("stockCodes", stockCodes);

            PythonResponseDto response = executePostTaskAsync(uri, payload);

            if (response != null && response.isSuccess() && response.getData() != null) {
                Map<String, Object> dataMap = caster.safeMapCast(response.getData());

                return ProcessResult.builder()
                        .success(true)
                        .message(dataMap.get("return_msg").toString())
                        .data(response.getData())
                        .build();
            } else {
                return createErrorProcess("그룹별 관심종목 조회 실패: " + (response != null ? response.getMessage() : "알 수 없는 오류"));
            }
        } catch (Exception e) {
            log.error("Python 서버 그룹별 관심종목 조회 오류: ", e);
            return createErrorProcess("그룹별 관심종목 조회 실패: " + e.getMessage());
        }
    }

    public ProcessResult subscribeRealtimeData(String uri, WatchListDto dto, String kiwoomToken){
        long userId = dto.getUserId();
        List<String> stockCodes = dto.getStockCodes();
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("token", kiwoomToken);
        payload.put("stockCodes", stockCodes);
        payload.put("userId", userId);
        PythonResponseDto response = executePostTask(uri, payload);

        if (response != null && response.isSuccess() && response.getData() != null) {
            Map<String, Object> dataMap = caster.safeMapCast(response.getData());

            return ProcessResult.builder()
                    .success(true)
                    .message(dataMap.get("return_msg").toString())
                    .data(response.getData())
                    .build();
        } else {
            return createErrorProcess("구독신청 실패: " + (response != null ? response.getMessage() : "알 수 없는 오류"));
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

    public PythonResponseDto executePostTaskAsync(String uri, Map<String, Object> params) {
        try {
            CompletableFuture<PythonResponseDto> future = pythonWebClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(params)
                    .retrieve()
                    .bodyToMono(PythonResponseDto.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .doOnError(error -> log.error("Python 서버 호출 실패: ", error))
                    .onErrorReturn(createErrorResponse("서버 통신 오류"))
                    .toFuture();

            // 확실한 동기 대기 처리
            PythonResponseDto result = future.get(timeoutSeconds + 5, TimeUnit.SECONDS);

            // 데이터 유효성 검증 추가
            boolean hasValidData = result.isSuccess() && result.getData() != null;
            if (hasValidData && result.getData() instanceof Map) {
                Map<String, Object> dataMap = caster.safeMapCast(result.getData());
                // 주식 정보 조회의 경우 핵심 데이터 존재 여부 확인
                if (uri.contains("/kiwoom/stockInfo") && dataMap != null) {
                    boolean hasStockData = dataMap.containsKey("atn_stk_infr") && dataMap.get("atn_stk_infr") != null;
                    if (!hasStockData) {
                        log.warn("Python 서비스 응답 완료되었으나 주식 데이터 누락: uri={}, success={}, dataKeys={}",
                                uri, result.isSuccess(), dataMap.keySet());
                        // 데이터 누락 시 에러 응답으로 변환
                        return createErrorResponse("주식 데이터 누락으로 인한 응답 불완전");
                    }
                }
            }
            log.debug("Python 서비스 응답 완료: uri={}, success={}", uri, result.isSuccess());
            return result;

        } catch (TimeoutException e) {
            log.error("Python 서버 응답 타임아웃: uri={}, timeout={}초", uri, timeoutSeconds, e);
            return createErrorResponse("응답 시간 초과");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Python 서버 호출 중단: ", e);
            return createErrorResponse("작업 중단됨");
        } catch (Exception e) {
            log.error("Python 서버 executeTask 오류: ", e);
            return createErrorResponse("작업 실행 실패: " + e.getMessage());
        }
    }

    public ProcessResult requestCandleData(String ticker) {
        String uri = "/wpy/getCandleData";
        try {
            ProcessResult response = executeGetTask(uri, Map.of("ticker", ticker));
            log.debug("requestCandleData response: success={}, data={}, dataType={}",
                response != null ? response.isSuccess() : "null",
                response != null ? response.getData() : "null",
                response != null && response.getData() != null ? response.getData().getClass().getName() : "null");

            if (response != null && response.isSuccess() && response.getData() != null) {
                Map<String, Object> dataMap = caster.safeMapCast(response.getData());
                log.debug("requestCandleData dataMap after cast: {}", dataMap);

                if (dataMap != null) {
                    Object returnMsgObj = dataMap.get("return_msg");
                    String returnMsg = returnMsgObj != null ? returnMsgObj.toString() : "성공";

                    return ProcessResult.builder()
                            .success(true)
                            .message(returnMsg)
                            .data(response.getData())
                            .build();
                } else {
                    // dataMap이 null인 경우에도 원본 데이터를 반환
                    return ProcessResult.builder()
                            .success(true)
                            .message("캔들 데이터 조회 완료 (데이터 변환 경고)")
                            .data(response.getData())
                            .build();
                }
            } else {
                return createErrorProcess("캔들데이터 조회 실패: " + (response != null ? response.getMessage() : "알 수 없는 오류"));
            }
        } catch (Exception e) {
            log.error("Python 서버 캔들데이터 조회 오류: ", e);
            return createErrorProcess("캔들데이터 조회 실패: " + e.getMessage());
        }
    }
}
