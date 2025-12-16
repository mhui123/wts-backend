package com.wts.kiwoom.repository;

import com.wts.kiwoom.entity.KiwoomToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface KiwoomTokenRepository extends JpaRepository<KiwoomToken, Long> {

    /**
     * 사용자별 활성 토큰 조회
     */
    Optional<KiwoomToken> findByUserIdAndTokenIdAndIsActiveTrue(Long userId, String tokenId);

    /**
     * 사용자의 기존 활성 토큰들 비활성화
     */
    @Modifying
    @Query("UPDATE KiwoomToken kt SET kt.isActive = false WHERE kt.userId = :userId AND kt.isActive = true")
    void deactivateUserTokens(@Param("userId") Long userId);

    /**
     * 만료된 토큰들 정리
     */
    @Modifying
    @Query("DELETE FROM KiwoomToken kt WHERE kt.expiresAt < :now OR kt.isActive = false")
    void deleteExpiredAndInactiveTokens(@Param("now") LocalDateTime now);

    /**
     * 사용자의 활성 토큰 개수
     */
    int countByUserIdAndIsActiveTrue(Long userId);
}