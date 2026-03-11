package com.wts.api.web;

import com.wts.admin.service.AdminService;
import com.wts.api.dto.ProcessResult;
import com.wts.api.service.PythonServerService;
import com.wts.kiwoom.service.KiwoomApiService;
import com.wts.summary.enums.BrokerType;
import com.wts.summary.enums.Currency;
import com.wts.summary.service.CashflowService;
import com.wts.summary.service.DashboardService;
import com.wts.summary.service.TradeHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {
    private final TradeHistoryService service;
    private final DashboardService dashboardService;
    private final PythonServerService pythonServerService;
    private final CashflowService cashflowService;
    private final KiwoomApiService kiwoomApiService;
    private final AdminService adminService;

    @GetMapping("/updatePriceTest")
    public ResponseEntity<ProcessResult> updatePriceTest(@RequestParam(name = "userId") Long userId) {
        ProcessResult result = dashboardService.updateClosePriceInfo(userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/getInoutCom")
    public ResponseEntity<ProcessResult> getInoutCom(@RequestParam(name = "userId") Long userId) {
        ProcessResult result = dashboardService.getInOutcomeInfo(userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/summarizeThEach")
    public ResponseEntity<ProcessResult> summarizeThEach(
            @RequestParam(name = "userId") Long userId,
            @RequestParam(name = "brokerType", required = false) BrokerType brokerType
    ) {
        ProcessResult result = service.summarizeTradeHistoryAsEachStocks(userId, brokerType);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/syncTickerTest")
    public ResponseEntity<ProcessResult> syncTickerTest() {
        pythonServerService.syncSymbolNameAndTicker();
        return ResponseEntity.ok(ProcessResult.builder().success(true).message("테스트완료").build());
    }

    @GetMapping("/getFlowTest")
    public ResponseEntity<String> getFlowTest(@RequestParam(name = "userId") Long userId,
                                              @RequestParam(name = "brokerType") BrokerType brokerType
    ) {
        cashflowService.calculateCashFlow(userId, brokerType);
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/verifyFlowTest")
    public ResponseEntity<String> verifyFlowTest(@RequestParam(name = "userId") Long userId,
                                              @RequestParam(name = "currency") Currency currency
    ) {
        cashflowService.verifyData(userId, currency);
        return ResponseEntity.ok("OK");
    }

    @PostMapping(value = "/uploadMultiplesTradeHistory", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProcessResult> uploadMultiplesTradeHistory(
            @RequestPart("files") List<MultipartFile> files,
            @RequestParam(name = "userId") Long userId,
            @RequestParam(name = "brokerType") BrokerType brokerType) {

        try {

            ProcessResult response = pythonServerService.processMultiplesTradeUpload(files, userId, brokerType);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ProcessResult errorResponse = ProcessResult.builder()
                    .success(false)
                    .message("거래내역 업로드 실패: " + e.getMessage())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/getCashflows")
    public ResponseEntity<ProcessResult> getCashflows(
            @RequestParam(name = "userId") Long userId,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {

            ProcessResult result = cashflowService.getCashFlow(userId, startDate, endDate);
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
            @RequestParam(name = "userId") Long userId,
            @RequestParam(name = "baseYm") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseYm,
            @RequestParam(name = "currency") Currency currency
    ) {
        try {
            ProcessResult result = cashflowService.getFlowDetails(userId, baseYm, currency);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            ProcessResult errorResponse = ProcessResult.builder()
                    .success(false)
                    .message("현금흐름 상세 정보 조회 실패: " + e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/admin/setStockCodes")
    public ResponseEntity<?> syncStockCdsWithMarket() {
        ProcessResult result = kiwoomApiService.syncKiwoomStocks();
        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/collectDividendInfo")
    public ProcessResult collectDividendInfo(@RequestParam(name = "symbols", required = false) List<String> symbols) {
        return adminService.collectDividendInfo(symbols);
    }

    @GetMapping("/stock/tickers")
    public ProcessResult getStockCds() {
        return adminService.syncStockCodes();
    }
}
