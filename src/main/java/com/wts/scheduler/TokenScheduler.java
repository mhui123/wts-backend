package com.wts.scheduler;

import com.wts.auth.service.GuestService;
import com.wts.kiwoom.service.KiwoomTokenManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class TokenScheduler {
    private final KiwoomTokenManager kiwoomTokenManager;
    private final GuestService guestService;
    // 1시간마다
    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void cleanupExpiredTokens() {
        log.info("[Scheduler] cleanupExpiredTokens started");
        kiwoomTokenManager.cleanupExpiredTokens();
        log.info("[Scheduler] cleanupExpiredTokens finished");
    }
//
//    // 5분마다
//    @Scheduled(fixedRate = 5 * 60 * 1000)
//    public void syncPrices() {
//    }
}
