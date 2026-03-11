package com.wts.auth.service;


import com.wts.auth.jpa.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {
    UserPermission findByUserId(Long userId);
}

