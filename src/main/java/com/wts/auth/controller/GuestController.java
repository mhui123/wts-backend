package com.wts.auth.controller;

import com.wts.summary.dto.DashboardSummaryDto;
import com.wts.summary.service.DashboardService;
import com.wts.auth.service.GuestService;
import com.wts.auth.dto.JwtResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/guest")
@RequiredArgsConstructor
public class GuestController {

    private final GuestService guestService;
    private final DashboardService dashboardService;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> guestLogin() {
        try {
            JwtResponse response = guestService.loginAsGuest();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/info")
    public ResponseEntity<String> guestInfo() {
        return ResponseEntity.ok("게스트 모드입니다. 모의 거래만 가능합니다.");
    }

    @GetMapping("/getDashSummary")
    public ResponseEntity<DashboardSummaryDto> getDashboardData(@RequestParam(required = false) Long userId) {
        DashboardSummaryDto summary = dashboardService.getDashboardData(userId); //service.getDashboardSummary(userId);
        return ResponseEntity.ok(summary);
    }
}