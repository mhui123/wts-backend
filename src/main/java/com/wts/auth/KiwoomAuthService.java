package com.wts.auth;

import com.wts.auth.dto.KiwoomApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Kiwoom 인증 서비스 (프루빙 방식)
 * - 클라이언트가 전달한 키움 접근토큰으로 무해한 API를 호출하여(return_code/return_msg) 유효성을 판별합니다.
 * - 성공 시 내부 JWT를 발급하여 반환합니다.
 */
@Service
public class KiwoomAuthService {

    private static final Logger log = LoggerFactory.getLogger(KiwoomAuthService.class);

    private final WebClient webClient;
    private final JwtUtil jwtUtil;

    private final String probePath;

    public KiwoomAuthService(@Value("${kiwoom.base.dev-url}") String kiwoomBaseUrl,
                             @Value("${kiwoom.probe.path:/api/dostk/acnt}") String probePath,
                             JwtUtil jwtUtil,
                             WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(kiwoomBaseUrl).build();
        this.jwtUtil = jwtUtil;
        this.probePath = probePath;
    }

    /**
     * 프루빙 호출로 토큰 유효성을 판단하고, 유효하면 내부 JWT를 반환합니다. 실패 시 null.
     */
    public String verifyKiwoomTokenAndCreateJwt(String kiwoomToken) {
        try {
            if (kiwoomToken == null || kiwoomToken.isBlank()) {
                log.warn("verifyKiwoomTokenAndCreateJwt called with blank token");
                return null;
            }

            Mono<KiwoomApiResponse> respMono = webClient.post()
                    .uri(probePath)
                    .headers(h -> {
                        h.setBearerAuth(kiwoomToken);
                        h.add("api-id", "kt00001"); // api id kt00001로 요청 (예수금상세현뢍요청)
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("qry_tp", "3"))
                    .retrieve()
                    .bodyToMono(KiwoomApiResponse.class)
                    .timeout(Duration.ofSeconds(5));

            KiwoomApiResponse resp = respMono.block();
            if (resp == null) {
                log.warn("Kiwoom probe response is null");
                return null;
            }

            String code = resp.getReturn_code();
            String msg = resp.getReturn_msg();

            if (isSuccess(code)) {
                String subject = "kiwoom-user"; // 필요 시 resp에서 사용자 식별자 추출로 개선
                String jwt = jwtUtil.createToken(subject);
                log.info("Kiwoom token validated via probe. Issued internal JWT for sub={}", subject);
                return jwt; //검증완료. jwt반환
            }

            log.info("Kiwoom token invalid by probe. code={} msg={}", code, msg);
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

    private boolean isSuccess(String returnCode) {
        if (returnCode == null) return false;
        // 기본 성공 코드는 "0"으로 가정. 필요 시 환경설정으로 치환 가능
        return "0".equals(returnCode) || "00000".equals(returnCode);
    }
}
