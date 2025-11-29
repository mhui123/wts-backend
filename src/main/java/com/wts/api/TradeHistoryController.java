package com.wts.api;

import com.wts.model.TradeHistoryDto;
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

    public TradeHistoryController(TradeHistoryService service) {
        this.service = service;
    }

    @GetMapping("/getTradesHistory")
    public ResponseEntity<List<TradeHistoryDto>> getTradesHistory(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String tradeType,
            @RequestParam(required = false) String symbolName,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "100") Integer size
    ) {
        List<TradeHistoryDto> list = service.getTrades(userId, fromDate, toDate, tradeType, symbolName, page, size);
        return ResponseEntity.ok(list);
    }
}

