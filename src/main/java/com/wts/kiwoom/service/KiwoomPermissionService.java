package com.wts.kiwoom.service;

import com.wts.kiwoom.entity.KiwoomPermission;
import com.wts.kiwoom.entity.KiwoomPermissionLevel;
import com.wts.kiwoom.repository.KiwoomPermissionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class KiwoomPermissionService {

    private final KiwoomPermissionRepository permissionRepository;

    /**
     * @PreAuthorize에서 호출되는 권한 검증 메서드
     */
    public boolean hasPermission(Long userId, String requiredPermission) {
        KiwoomPermission permission = permissionRepository.findByUserId(userId);

        if (permission == null) {
            return false; // 권한 정보가 없음
        }

        return checkPermissionLevel(permission.getPermissionLevel(), requiredPermission);
    }

    private boolean checkPermissionLevel(KiwoomPermissionLevel userLevel, String requiredLevel) {
        // 권한 계층 구조
        return switch (requiredLevel) {
            case "BASIC_USER" -> userLevel != null; // 모든 등록된 사용자
            case "TRADING_USER" -> userLevel == KiwoomPermissionLevel.TRADING_USER ||
                    userLevel == KiwoomPermissionLevel.ADMIN_USER;
            case "ADMIN_USER" -> userLevel == KiwoomPermissionLevel.ADMIN_USER;
            default -> false;
        };
    }
}