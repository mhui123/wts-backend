package com.wts.summary.controller;

import com.wts.summary.dto.DashboardSummaryDto;
import com.wts.summary.dto.StockDetailDto;
import com.wts.summary.enums.BrokerType;
import com.wts.summary.enums.Currency;
import com.wts.summary.service.CashflowService;
import com.wts.summary.service.DashboardService;
import com.wts.summary.service.TradeHistoryService;
import com.wts.auth.JwtUtil;
import com.wts.api.dto.ProcessResult;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dash")
@RequiredArgsConstructor
public class DashboardController {

    private final TradeHistoryService service;
    private final DashboardService dashboardService;
    private final CashflowService cashflowService;
    private final JwtUtil jwtUtil;

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

    @GetMapping("/syncLatestPortfolioItems")
    public ResponseEntity<ProcessResult> syncLatestPortfolioItems(
            @RequestParam(name = "broker", required = false) BrokerType brokerType,
            Authentication authentication) {
        Long actualUserId = jwtUtil.extractUserIdFromAuthentication(authentication);
        if (actualUserId == null) {
            return ResponseEntity.badRequest().build();
        }

        ProcessResult result = service.summarizeTradeHistoryAsEachStocks(actualUserId, brokerType);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/getStockDetailInfo")
    public ResponseEntity<StockDetailDto> getStockDetailInfo(
            @RequestParam(name = "ticker", required = false) String ticker,
            @RequestParam(name = "currency", required = false) Currency currency,
            @RequestParam(name = "broker", required = false) BrokerType brokerType,
            Authentication authentication
    ) {
        try {
            Long actualUserId = jwtUtil.extractUserIdFromAuthentication(authentication);
            if (actualUserId == null) {
                return ResponseEntity.badRequest().build();
            }

            StockDetailDto dto = dashboardService.callStockDetailInfo(actualUserId, ticker, currency, brokerType);
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

    @GetMapping("/getMoneyDetailInfo")
    public ResponseEntity<ProcessResult> getMoneyDetailInfo(
//            @RequestParam(name = "userId") Long userId,
            Authentication authentication
    ) {
        try {
            Long actualUserId = jwtUtil.extractUserIdFromAuthentication(authentication);
            if (actualUserId == null) {
                return ResponseEntity.badRequest().build();
            }

            ProcessResult result = dashboardService.getInOutcomeInfo(actualUserId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/getCashflows")
    public ResponseEntity<ProcessResult> getCashflows(
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication
    ) {
        try {
            Long actualUserId = jwtUtil.extractUserIdFromAuthentication(authentication);
            if (actualUserId == null) {
                return ResponseEntity.badRequest().build();
            }

            ProcessResult result = cashflowService.getCashFlow(actualUserId, startDate, endDate);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            ProcessResult errorResponse = ProcessResult.builder()
                    .success(false)
                    .message("현금흐름정보 조회 실패: " + e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/getCashflowDetails")
    public ResponseEntity<ProcessResult> getCashflowDetails(
            @RequestParam(name = "baseYm") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseYm,
            @RequestParam(name = "currency") Currency currency,
            Authentication authentication
    ) {
        try {
            Long actualUserId = jwtUtil.extractUserIdFromAuthentication(authentication);
            if (actualUserId == null) {
                return ResponseEntity.badRequest().build();
            }

            ProcessResult result = cashflowService.getFlowDetails(actualUserId, baseYm, currency);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

}
