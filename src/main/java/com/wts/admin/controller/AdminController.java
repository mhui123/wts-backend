package com.wts.admin.controller;

import com.wts.admin.service.AdminService;
import com.wts.api.dto.ProcessResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/collectDividendInfo")
    @PreAuthorize("@AccountService.hasPermission(authentication, 'ADMIN_USER')")
    public ProcessResult collectDividendInfo(@RequestParam(name= "symbols") List<String> symbols) {
        return adminService.collectDividendInfo(symbols);
    }

    // 관리자만 호출 가능
    @PostMapping("/setStockCodes")
    @PreAuthorize("@kiwoomPermissionService.hasPermission(authentication, 'ADMIN_USER')")
    public ProcessResult syncStockCdsWithMarket() {
        return adminService.syncStockCodes();
    }
}
