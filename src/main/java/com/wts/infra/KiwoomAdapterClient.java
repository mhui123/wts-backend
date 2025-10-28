// 외부 Kiwoom 어댑터용 HTTP 클라이언트: WebClient를 사용해 어댑터의 REST/SSE 엔드포인트와 통신합니다.
// 주요 책임: health/balance/stock 조회, 주문 전송(placeOrder), SSE 기반 실시간 호가 구독을 제공 (Reactive types)
package com.wts.infra;

import com.wts.model.OrderRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class KiwoomAdapterClient {

    private final WebClient client;

    public KiwoomAdapterClient(@Value("${adapter.base-url}") String baseUrl, WebClient.Builder builder) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    public Mono<String> getHealth() {
        return client.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> getBalance() {
        return client.get()
                .uri("/account/balance")
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> getStock(String code) {
        return client.get()
                .uri("/stocks/{code}", code)
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> placeOrder(OrderRequest req) {
        return client.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .bodyToMono(String.class);
    }

    public Flux<String> subscribeQuotes(String codesCsv) {
        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/sse/quotes")
                        .queryParam("codes", codesCsv)
                        .build())
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class);
    }
}
