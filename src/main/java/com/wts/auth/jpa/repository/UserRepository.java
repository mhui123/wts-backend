package com.wts.auth.jpa.repository;

import com.wts.auth.jpa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    Optional<User> findByEmail(String email);
    Optional<User> findByRoles(String roles);

    @Modifying
    @Query("DELETE FROM User u WHERE u.provider = :provider AND u.createdAt < :expiredTime")
    void deleteByProviderAndCreatedAtBefore(@Param("provider") String provider, @Param("expiredTime") LocalDateTime expiredTime);
}

