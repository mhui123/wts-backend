// 호가(Quotes) 브리지: 어댑터의 SSE 스트림을 구독하여 받은 페이로드를 STOMP 구독자에게 전달합니다.
// 주요 책임: 애플리케이션 시작 시 구성된 종목 코드를 자동 구독하고 /topic/quotes로 팬아웃
package com.wts.infra;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class QuotesBridge {

    private static final Logger log = LoggerFactory.getLogger(QuotesBridge.class);

    private final KiwoomAdapterClient adapter;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${quotes.codes:}")
    private String codesCsv;

    public QuotesBridge(KiwoomAdapterClient adapter, SimpMessagingTemplate messagingTemplate) {
        this.adapter = adapter;
        this.messagingTemplate = messagingTemplate;
    }

    @PostConstruct
    public void start() {
        if (codesCsv == null || codesCsv.isBlank()) {
            // no auto-subscribe on startup if not configured
            log.info("QuotesBridge: no quotes.codes configured, skipping auto-subscribe");
            return;
        }

        log.info("QuotesBridge: subscribing to quotes for codes={}", codesCsv);
        Flux<String> stream = adapter.subscribeQuotes(codesCsv)
                .retry(); // reconnect on error

        stream.subscribe(payload -> {
            // fan-out to UI subscribers
            try {
                messagingTemplate.convertAndSend("/topic/quotes", payload);
            } catch (Exception e) {
                log.error("Failed to forward quote payload to /topic/quotes", e);
            }
        }, err -> {
            log.error("QuotesBridge: error in quote stream", err);
        });
    }
}
