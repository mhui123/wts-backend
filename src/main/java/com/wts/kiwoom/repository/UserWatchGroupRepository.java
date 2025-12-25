package com.wts.kiwoom.repository;

import com.wts.kiwoom.entity.UserWatchGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserWatchGroupRepository extends JpaRepository<UserWatchGroup, Long> {

    /**
     * 특정 사용자의 모든 관심종목 그룹 조회 (표시순서대로)
     */
    List<UserWatchGroup> findByUserIdOrderByDisplayOrderAsc(long userId);

    /**
     * 특정 사용자의 특정 그룹명 조회
     */
    Optional<UserWatchGroup> findByUserIdAndGroupName(long userId, String groupName);

    /**
     * 특정 사용자의 그룹 존재 여부 확인
     */
    boolean existsByUserIdAndGroupName(long userId, String groupName);

    /**
     * 특정 사용자의 그룹 개수 조회
     */
    long countByUserId(long userId);

    /**
     * 특정 사용자의 관심종목 그룹을 아이템과 함께 조회
     */
    @Query("SELECT DISTINCT g FROM UserWatchGroup g " +
            "LEFT JOIN FETCH g.watchItems i " +
            "WHERE g.userId = :userId " +
            "ORDER BY g.displayOrder ASC, i.displayOrder ASC")
    List<UserWatchGroup> findByUserIdWithItems(@Param("userId") long userId);

    /**
     * 특정 사용자의 다음 표시순서 조회
     */
    @Query("SELECT COALESCE(MAX(g.displayOrder), 0) + 1 FROM UserWatchGroup g WHERE g.userId = :userId")
    Integer findNextDisplayOrder(@Param("userId") long userId);

    // 그룹명 중복 검사 (자신 제외)
    boolean existsByUserIdAndGroupNameAndIdNot(Long userId, String groupName, Long id);
}