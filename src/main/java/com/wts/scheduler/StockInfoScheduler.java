package com.wts.scheduler;

import com.wts.admin.service.AdminService;
import com.wts.summary.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class StockInfoScheduler {
    private final AdminService adminService;
    private final DashboardService dashboardService;
    // 매일 새벽 2시
    @Scheduled(cron = "0 0 2 * * *")
    public void updateDividendInfo() {
        log.info("[StockInfoScheduler] collectDividendInfo started");
        // 작업 내용 : portfolio 업데이트 로직 구현
        List<String> symbols = dashboardService.sendPortfolioSymbolList();
        adminService.collectDividendInfo(symbols);
        log.info("[StockInfoScheduler] collectDividendInfo finished");
    }

    @Scheduled(cron = "0 0 0 * * *") // 매일 자정
    public void processPendingDivInfo() {
        log.info("[StockInfoScheduler] processPendingDivInfo started");
        // 작업 내용 : portfolio 업데이트 로직 구현
        List<String> symbols = dashboardService.sendPendingList();
        adminService.collectDividendInfo(symbols);
        dashboardService.updatePendigList(symbols);
        log.info("[StockInfoScheduler] processPendingDivInfo finished");
    }

    @Scheduled(cron = "0 0 6 * * *") // 매일 6시
    public void syncStockCodes() {
        // stock_master 테이블에 거래소 상장 종목코드 정보 갱신
        log.info("[StockInfoScheduler] syncStockCodes started");
        adminService.syncStockCodes();
        log.info("[StockInfoScheduler] syncStockCodes finished");
    }
}
