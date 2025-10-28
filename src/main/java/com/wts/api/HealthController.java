// 헬스 체크 컨트롤러: 어댑터(KiwoomAdapterClient)를 통해 서비스 상태(health)를 확인하여 /api/health로 노출합니다.
// 주요 책임: 외부 어댑터 호출을 프록시하고 비동기 Mono로 응답을 반환
package com.wts.api;

import com.wts.infra.KiwoomAdapterClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final KiwoomAdapterClient adapter;

    public HealthController(KiwoomAdapterClient adapter) {
        this.adapter = adapter;
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<String>> health() {
        return adapter.getHealth()
                .map(ResponseEntity::ok);
    }
}
