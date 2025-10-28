// 주문 관련 REST 컨트롤러: 주문 요청을 받아 외부 어댑터로 전달하고 결과를 비동기적으로 반환합니다.
// 주요 책임: /api/orders 엔드포인트, 요청 유효성 검사 및 idempotency 헤더(플래그) 처리 자리 확보
package com.wts.api;

import com.wts.infra.KiwoomAdapterClient;
import com.wts.model.OrderRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class OrdersController {

    private static final Logger log = LoggerFactory.getLogger(OrdersController.class);

    private final KiwoomAdapterClient adapter;

    public OrdersController(KiwoomAdapterClient adapter) {
        this.adapter = adapter;
    }

    @PostMapping("/orders")
    public Mono<ResponseEntity<String>> placeOrder(@Valid @RequestBody OrderRequest req,
                                                   @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            log.info("placeOrder called with Idempotency-Key={}", idempotencyKey);
        } else {
            log.info("placeOrder called without Idempotency-Key for symbol={} qty={}", req.getSymbol(), req.getQty());
        }
        // TODO: validate idempotencyKey and persist intent
        return adapter.placeOrder(req)
                .doOnNext(resp -> log.info("order placed, adapter response: {}", resp))
                .map(ResponseEntity::ok);
    }
}
