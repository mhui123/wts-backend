// 계좌 관련 REST 컨트롤러: 외부 Kiwoom 어댑터를 통해 계좌 잔액 정보를 조회하고 반환합니다.
// 주요 책임: /api/account 경로의 엔드포인트 제공, 비동기 반환(Mono)
package com.wts.api;

import com.wts.infra.KiwoomAdapterClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final KiwoomAdapterClient adapter;

    public AccountController(KiwoomAdapterClient adapter) {
        this.adapter = adapter;
    }

    @GetMapping("/balance")
    public Mono<ResponseEntity<String>> balance() {
        return adapter.getBalance().map(ResponseEntity::ok);
    }
}
