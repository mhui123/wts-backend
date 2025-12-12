package com.wts.api;

import com.wts.model.*;
import com.wts.service.DashboardService;
import com.wts.service.TradeHistoryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class TradeHistoryController {

    private final TradeHistoryService service;
    private final DashboardService dashboardService;

    public TradeHistoryController(TradeHistoryService service, DashboardService dashboardService) {
        this.dashboardService = dashboardService;
        this.service = service;
    }

    @GetMapping("/getTradesHistoryRenew")
    public ResponseEntity<List<TradeHistoryDto>> getTradesHistoryRenew(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String tradeType,
            @RequestParam(required = false) String symbolName,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "100") Integer size
    ) {
        List<TradeHistoryDto> list = service.getTrades_renew(userId, fromDate, toDate, tradeType, symbolName, page, size);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/getDashSummary")
    public ResponseEntity<DashboardSummaryDto> getDashboardData(@RequestParam(required = false) Long userId) {
        DashboardSummaryDto summary = dashboardService.getDashboardData(userId); //service.getDashboardSummary(userId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/getDashTest")
    public ResponseEntity<String> getDashSummaryTest(@RequestParam(name = "userId", required = false) Long userId) {
        dashboardService.setDataToPortfolioItem(userId);
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/syncLatestPortfolioItems")
    public ResponseEntity<ProcessResult> syncLatestPortfolioItems(@RequestParam(name = "userId", required = false) Long userId) {
        ProcessResult result = dashboardService.setDataToPortfolioItem(userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/getStockDetailInfo")
    public ResponseEntity<StockDetailDto> getStockDetailInfo(@RequestParam(name = "userId", required = false) Long userId,
                                                             @RequestParam(name = "ticker", required = false) String ticker) {
        try{
            StockDetailDto dto = dashboardService.callStockDetailInfo(userId, ticker);
            return ResponseEntity.ok(dto);
        } catch (Exception e){
            return ResponseEntity.badRequest().build();
        }


    }

}
