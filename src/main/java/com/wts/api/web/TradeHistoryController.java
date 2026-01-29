package com.wts.api.web;

import com.wts.api.dto.DashboardSummaryDto;
import com.wts.api.dto.StockDetailDto;
import com.wts.api.dto.TradeHistoryDto;
import com.wts.api.entity.User;
import com.wts.auth.JwtUtil;
import com.wts.model.*;
import com.wts.api.service.DashboardService;
import com.wts.api.service.TradeHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TradeHistoryController {

    private final TradeHistoryService service;
    private final DashboardService dashboardService;
    private final JwtUtil jwtUtil;

    @GetMapping("/getTradesHistoryRenew")
    public ResponseEntity<List<TradeHistoryDto>> getTradesHistoryRenew(
            @RequestParam(name = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(name = "tradeType", required = false) String tradeType,
            @RequestParam(name = "symbolName", required = false) String symbolName,
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(name = "size", required = false, defaultValue = "100") Integer size,
            Authentication authentication
    ) {
        // JWT 토큰에서 사용자 ID 추출 (게스트 포함)
        Long actualUserId = jwtUtil.extractUserIdFromAuthentication(authentication);
        if (actualUserId == null) {
            return ResponseEntity.badRequest().build();
        }
        List<TradeHistoryDto> list = service.getTrades_renew(actualUserId, fromDate, toDate, tradeType, symbolName, page, size);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/getDashSummary")
    public ResponseEntity<DashboardSummaryDto> getDashboardData(Authentication authentication) {
        // JWT 토큰에서 사용자 ID 추출 (게스트 포함)
        Long actualUserId = jwtUtil.extractUserIdFromAuthentication(authentication);

        if (actualUserId == null) {
            return ResponseEntity.badRequest().build();
        }

        DashboardSummaryDto summary = dashboardService.getDashboardData(actualUserId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/getDashTest")
    public ResponseEntity<String> getDashSummaryTest(Authentication authentication) {
        Long actualUserId = jwtUtil.extractUserIdFromAuthentication(authentication);
        if (actualUserId == null) {
            return ResponseEntity.badRequest().build();
        }
        dashboardService.setDataToPortfolioItem(actualUserId);
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/syncLatestPortfolioItems")
    public ResponseEntity<ProcessResult> syncLatestPortfolioItems(Authentication authentication) {
        Long actualUserId = jwtUtil.extractUserIdFromAuthentication(authentication);
        if (actualUserId == null) {
            return ResponseEntity.badRequest().build();
        }

        ProcessResult result = dashboardService.setDataToPortfolioItem(actualUserId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/getStockDetailInfo")
    public ResponseEntity<StockDetailDto> getStockDetailInfo(
            @RequestParam(name = "ticker", required = false) String ticker,
            Authentication authentication
    ) {
        try {
            Long actualUserId = jwtUtil.extractUserIdFromAuthentication(authentication);
            if (actualUserId == null) {
                return ResponseEntity.badRequest().build();
            }

            StockDetailDto dto = dashboardService.callStockDetailInfo(actualUserId, ticker);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/getOcilatorInfo")
    public ResponseEntity<ProcessResult> getOcilatorInfo(
            @RequestParam(name = "ticker", required = false) String ticker,
            @RequestParam(name = "period", required = false) String period,
            @RequestParam(name = "interval", required = false) String interval,
            Authentication authentication
    ) {
        try {
            Long actualUserId = jwtUtil.extractUserIdFromAuthentication(authentication);
            if (actualUserId == null) {
                return ResponseEntity.badRequest().build();
            }

            ProcessResult result = dashboardService.getOcilatorInfo(ticker, period, interval);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

}
