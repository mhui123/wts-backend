package com.wts.scheduler;

import com.wts.api.service.PythonServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class PortfolioScheduler {
    private final PythonServerService pythonServerService;
    // 매일 새벽 2시
    @Scheduled(cron = "0 0 2 * * *")
    public void updatePortfolio() {
        log.info("[Scheduler] portfolio update started");
        // 작업 내용 : portfolio 업데이트 로직 구현
        pythonServerService.syncSymbolNameAndTicker();
        log.info("[Scheduler] portfolio update finished");
    }
//
//    // 5분마다
//    @Scheduled(fixedRate = 5 * 60 * 1000)
//    public void syncPrices() {
//    }
}
