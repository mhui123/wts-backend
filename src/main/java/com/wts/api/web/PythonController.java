package com.wts.api.web;

import com.wts.api.dto.StockPriceResponseDto;
import com.wts.api.dto.TradeHistoryUploadDto;
import com.wts.auth.JwtUtil;
import com.wts.model.*;
import com.wts.api.service.DashboardService;
import com.wts.api.service.PythonServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/python")
@RequiredArgsConstructor
@Slf4j
public class PythonController {

    private final PythonServerService pythonServerService;
    private final DashboardService dService;
    private final JwtUtil jwtUtil;

    /**
     * Python 서버에서 티커 정보 조회
     */
    @GetMapping("/ticker")
    public ResponseEntity<ProcessResult> getTicker(@RequestParam(name = "isin", required = false) String isin) {
        log.info("티커 정보 조회 요청: isin={}", isin);

        try {
            ProcessResult response = pythonServerService.getTicker(isin);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("티커 정보 조회 실패: ", e);
            ProcessResult errorResponse = ProcessResult.builder()
                    .success(false)
                    .message("티커 정보 조회 실패: " + e.getMessage())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }


    /**
     * Python 서버에서 티커 정보 조회
     */
    @GetMapping("/stock/prices")
    public ResponseEntity<StockPriceResponseDto> getStockPrice(@RequestParam(required = false) String symbols) {
        log.info("주가정보 요청: symbol={}", symbols);

        try {
            StockPriceResponseDto response = pythonServerService.getStockPrice(symbols);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("주가정보 조회 실패: ", e);
            return ResponseEntity.internalServerError().body(new StockPriceResponseDto());
        }
    }
    /**
     * 거래 내역을 Python 서버로 업로드
     */
    @PostMapping(value = "/uploadTradeHistory", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProcessResult> uploadTradeHistory(
            @RequestPart("file") MultipartFile file,
            Authentication authentication) {

        try {
            // DTO 생성
            Long userId = jwtUtil.extractUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest().build();
            }
            log.info("거래내역 업로드 요청: userId={}, filename={}", userId, file.getOriginalFilename());
            TradeHistoryUploadDto uploadDto = TradeHistoryUploadDto.builder()
                    .userId(String.valueOf(userId))
                    .file(file)
                    .build();

            // Python 서버에서 JSON 데이터 받아오기
            ProcessResult response = pythonServerService.uploadTradeHistory(uploadDto);
            if(response.isSuccess()){
                dService.setDataToPortfolioItem(userId); //portfolioItem 최신화
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("거래내역 업로드 실패: ", e);
            ProcessResult errorResponse = ProcessResult.builder()
                    .success(false)
                    .message("거래내역 업로드 실패: " + e.getMessage())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping(value = "/getCandleData")
    public ResponseEntity<ProcessResult> getCandleData(
            @RequestParam(name = "ticker", required = false) String ticker,
            Authentication authentication) {

        try {
            Long userId = jwtUtil.extractUserIdFromAuthentication(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest().build();
            }

            ProcessResult response = pythonServerService.requestCandleData(ticker);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("캔들데이터 획득 실패: ", e);
            ProcessResult errorResponse = ProcessResult.builder()
                    .success(false)
                    .message("캔들데이터 획득 실패: " + e.getMessage())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

}
