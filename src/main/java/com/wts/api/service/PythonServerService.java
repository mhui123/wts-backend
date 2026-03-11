package com.wts.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wts.api.dto.ProcessResult;
import com.wts.api.dto.PythonResponseDto;
import com.wts.api.dto.StockPriceResponseDto;
import com.wts.auth.JwtUtil;
import com.wts.kiwoom.dto.KeyDto;
import com.wts.kiwoom.dto.WatchListDto;
import com.wts.summary.dto.TradeHistoryJsonRecord;
import com.wts.summary.dto.TradeHistoryUploadDto;
import com.wts.summary.enums.BrokerType;
import com.wts.summary.jpa.entity.PortfolioItemEntity;
import com.wts.summary.jpa.entity.SymbolTicker;
import com.wts.summary.jpa.repository.JpaPortfolioItemRepository;
import com.wts.summary.jpa.repository.SymbolTickerRepository;
import com.wts.summary.service.TradeHistoryService;
import com.wts.util.MapCaster;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.*;
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

    private final SymbolTickerRepository symbolTickerRepository;
    private final JpaPortfolioItemRepository jpaPortfolioItemRepository;

    public PythonServerService(
            @Qualifier("pythonWebClient") WebClient pythonWebClient,
            @Qualifier("py32WebClient") WebClient py32WebClient,
            TradeHistoryService tService,
            JwtUtil jwtUtil,
            MapCaster caster, SymbolTickerRepository symbolTickerRepository, JpaPortfolioItemRepository jpaPortfolioItemRepository) {
        this.pythonWebClient = pythonWebClient;
        this.py32WebClient = py32WebClient;
        this.tService = tService;
        this.jwtUtil = jwtUtil;
        this.caster = caster;
        this.symbolTickerRepository = symbolTickerRepository;
        this.jpaPortfolioItemRepository = jpaPortfolioItemRepository;
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

    public void executePostTaskFireAndForget(String uri, Map<String, Object> params) {
        Duration fireAndForgetTimeout = Duration.ofSeconds(Math.max(timeoutSeconds,15));
        try {
            pythonWebClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(params != null ? params : Map.of())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(fireAndForgetTimeout)
                    .doOnSuccess(result -> log.debug("Fire-and-forget 요청 완료: uri={}", uri))
                    .onErrorResume(error -> {
                        log.warn("Fire-and-forget 요청 실패: uri={}, error={}", uri, error.getMessage());
                        return Mono.empty();
                    })
                    .subscribe(
                            success -> log.trace("Fire-and-forget 응답: uri={}", uri),
                            error -> log.debug("예상치 못한 에러 처리: uri={}, error={}", uri, error.getMessage())
                    );
        } catch (Exception e) {
            log.warn("Fire-and-forget 요청 예외: uri={}, error={}", uri, e.getMessage());
        }
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
                                    // List 타입은 ?key=a&key=b&key=c 형태의 다중 쿼리 파라미터로 직렬화
                                    if (value instanceof List<?> list) {
                                        list.forEach(item -> uriBuilder.queryParam(key, item));
                                    } else {
                                        uriBuilder.queryParam(key, value);
                                    }
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
    public ProcessResult uploadTradeHistory(TradeHistoryUploadDto uploadDto, BrokerType brokerType) {
        String userId = uploadDto.getUserId();
        Path tempFile = null;
        try {
            tempFile = downloadTradeHistoryNdjsonToTempFile(uploadDto, "/wpy/uploadTradeHistory");
            try (InputStream inputStream = Files.newInputStream(tempFile, StandardOpenOption.READ)) {
                return tService.saveTradeHistoryFromNdjsonStream(inputStream, Long.valueOf(userId), brokerType);
            }
        } catch (Exception e) {
            log.error("Python 서버 uploadTradeHistory 오류: ", e);
            return createErrorProcess("거래내역 업로드 실패: " + e.getMessage());
        } finally {
            deleteTempFile(tempFile);
        }
    }

    /**
     * 거래내역을 Python 서버로 업로드 (POST)
     */
    public ProcessResult uploadKiwoomTradeHistory(TradeHistoryUploadDto uploadDto, BrokerType brokerType) {
        String userId = uploadDto.getUserId();
        Path tempFile = null;
        try {
            tempFile = downloadTradeHistoryNdjsonToTempFile(uploadDto, "/wpy/uploadKiwoomTradeHistory");
            try (InputStream inputStream = Files.newInputStream(tempFile, StandardOpenOption.READ)) {
                return tService.saveTradeHistoryFromNdjsonStream(inputStream, Long.valueOf(userId), brokerType);
            }
        } catch (Exception e) {
            log.error("Python 서버 uploadTradeHistory 오류: ", e);
            return createErrorProcess("거래내역 업로드 실패: " + e.getMessage());
        } finally {
            deleteTempFile(tempFile);
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
            Map<String, Object> payload = new HashMap<>();
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
            Map<String, Object> payload = new HashMap<>();
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
            Map<String, Object> payload = new HashMap<>();
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
        Map<String, Object> payload = new HashMap<>();
        payload.put("token", kiwoomToken);
        payload.put("stockCodes", stockCodes);
        payload.put("userId", userId);
        executePostTaskFireAndForget(uri, payload);

        return ProcessResult.builder()
                .success(true)
                .message("실시간 데이터 구독 요청 완료")
                .build();

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

    @Transactional
    public void syncSymbolNameAndTicker(){
        Optional<List<SymbolTicker>> emptyTickers = symbolTickerRepository.findByTickerIsNull();

        if(emptyTickers.isPresent()){
            List<String> companyNames = emptyTickers.get().stream().map(SymbolTicker::getSymbolName).toList();
            Optional<List<PortfolioItemEntity>> l = jpaPortfolioItemRepository.findByCompanyNameInAndSymbolIsNull(companyNames);

            for(SymbolTicker st : emptyTickers.get()){
                // 티커 정보 업데이트 로직 구현
                ProcessResult r = getTicker(st.getIsin());
                String ticker = r.isSuccess() ? r.getMessage() : "";
                if(ticker != null && !ticker.isEmpty()) {
                    st.setTicker(ticker);

                    if(l.isPresent()){
                        l.get().stream()
                                .filter(item -> item.getCompanyName().equals(st.getSymbolName())).findFirst().ifPresent(pi -> pi.setSymbol(ticker));
                    }
                }
            }
        } else {
            Optional<List<PortfolioItemEntity>> symbolNulls = jpaPortfolioItemRepository.findBySymbolIsNull();
            if(symbolNulls.isPresent()){
                for(PortfolioItemEntity pi : symbolNulls.get()){
                    String companyName = pi.getCompanyName();
                    Optional<SymbolTicker> stOpt = symbolTickerRepository.findBySymbolName(companyName);
                    if(stOpt.isPresent()){
                        String isin = stOpt.get().getIsin();
                        ProcessResult r = getTicker(isin);
                        String ticker = r.isSuccess() ? r.getMessage() : "";
                        if(ticker != null && !ticker.isEmpty()) {
                            pi.setSymbol(ticker);
                            log.info("심볼 동기화 완료: 회사명='{}', 심볼='{}'", companyName, ticker);
                        }
                    }
                }
            }

        }
    }

    public ProcessResult processMultiplesTradeUpload(List<MultipartFile> files, long userId, BrokerType brokerType) throws JsonProcessingException {

        List<String> jsonDataList = files.stream().map(file -> {
            TradeHistoryUploadDto uploadDto = TradeHistoryUploadDto.builder()
                    .userId(String.valueOf(userId))
                    .file(file)
                    .build();
            return convertPDFToJson(uploadDto, brokerType);
        }).toList();

        ObjectMapper objectMapper = new ObjectMapper();
        List<TradeHistoryJsonRecord> uploadHistories = new ArrayList<>();
        for(String jsonData : jsonDataList){
            List<TradeHistoryJsonRecord> histories = objectMapper.readValue(jsonData, new TypeReference<>() {});
            uploadHistories.addAll(histories);
        }
        uploadHistories.sort(Comparator.comparing(TradeHistoryJsonRecord::tradeDate));

        return tService.saveTradeHistoryFromList(uploadHistories, userId, brokerType);

    }

    public String convertPDFToJson(TradeHistoryUploadDto uploadDto, BrokerType broker) {
        Path tempFile = null;
        String uri = broker.uploadUri();
        try {
            tempFile = downloadTradeHistoryNdjsonToTempFile(uploadDto, uri);
            try (InputStream inputStream = Files.newInputStream(tempFile, StandardOpenOption.READ)) {
                return convertNdjsonToJsonArray(inputStream);
            }

        } catch (Exception e) {
            log.error("Python 서버 uploadTradeHistory 오류: ", e);
            throw new RuntimeException("거래내역 json변환 실패: " + e.getMessage());
        } finally {
            deleteTempFile(tempFile);
        }
    }

    private Path downloadTradeHistoryNdjsonToTempFile(TradeHistoryUploadDto uploadDto, String uri) {
        String userId = uploadDto.getUserId();
        Path tempFile;
        try {
            tempFile = Files.createTempFile("trade-history-", ".ndjson");
        } catch (Exception e) {
            throw new RuntimeException("임시 파일 생성 실패: " + e.getMessage(), e);
        }

        try (OutputStream outputStream = Files.newOutputStream(
                tempFile, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {

            Flux<DataBuffer> dataBuffers = pythonWebClient.post()
                    .uri(uri)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.valueOf("application/x-ndjson"))
                    .body(BodyInserters.fromMultipartData("file", uploadDto.getFile().getResource())
                            .with("userId", userId))
                    .exchangeToFlux(response -> {
                        if (response.statusCode().is2xxSuccessful()) {
                            return response.bodyToFlux(DataBuffer.class);
                        }
                        return response.createException().flatMapMany(Flux::error);
                    })
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .doOnDiscard(DataBuffer.class, DataBufferUtils::release)
                    .retryWhen(reactor.util.retry.Retry.backoff(2, Duration.ofMillis(500))
                            .filter(ex -> ex instanceof reactor.netty.http.client.PrematureCloseException))
                    .doOnError(error -> log.error("Python 서버 uploadTradeHistory 호출 실패: ", error));

            DataBufferUtils.write(dataBuffers, outputStream)
                    .then()
                    .block();

            return tempFile;
        } catch (Exception e) {
            deleteTempFile(tempFile);
            throw new RuntimeException("거래내역 NDJSON 다운로드 실패");
        }
    }

    private String convertNdjsonToJsonArray(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            ObjectMapper mapper = new ObjectMapper()
                    .enable(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature());
            List<TradeHistoryJsonRecord> records = new ArrayList<>();

            String firstLine = reader.readLine();
            if (firstLine == null) {
                return "[]";
            }

            boolean firstLineConsumed = false;
            try {
                Map<String, Object> meta = mapper.readValue(firstLine, new TypeReference<>() {});
                if (meta != null && meta.containsKey("success")) {
                    firstLineConsumed = true;
                }
            } catch (Exception ignore) {
                // 첫 줄이 메타가 아닐 수 있으므로 아래에서 데이터로 재처리
            }

            if (!firstLineConsumed) {
                records.add(mapper.readValue(firstLine, TradeHistoryJsonRecord.class));
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                records.add(mapper.readValue(line, TradeHistoryJsonRecord.class));
            }

            return mapper.writeValueAsString(records);
        } catch (Exception e) {
            throw new RuntimeException("NDJSON 변환 실패: " + e.getMessage(), e);
        }
    }

    private void deleteTempFile(Path tempFile) {
        if (tempFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(tempFile);
        } catch (Exception e) {
            log.warn("임시 파일 삭제 실패: {}", tempFile);
        }
    }
}
