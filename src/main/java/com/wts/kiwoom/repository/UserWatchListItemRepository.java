package com.wts.kiwoom.repository;

import com.wts.kiwoom.entity.UserWatchListItem;
import com.wts.kiwoom.entity.UserWatchGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserWatchListItemRepository extends JpaRepository<UserWatchListItem, Long> {

    /**
     * 특정 그룹의 모든 아이템 조회 (표시순서대로)
     */
    List<UserWatchListItem> findByWatchGroupOrderByDisplayOrderAsc(UserWatchGroup watchGroup);

    /**
     * 특정 그룹의 특정 종목코드 아이템 조회
     */
    Optional<UserWatchListItem> findByWatchGroupAndStockCd(UserWatchGroup watchGroup, String stockCd);

    /**
     * 특정 그룹의 종목 존재 여부 확인
     */
    boolean existsByWatchGroupAndStockCd(UserWatchGroup watchGroup, String stockCd);

    /**
     * 특정 그룹의 모든 아이템 삭제
     */
    @Modifying
    @Transactional
    void deleteByWatchGroup(UserWatchGroup watchGroup);

    /**
     * 특정 그룹의 아이템 개수 조회
     */
    long countByWatchGroup(UserWatchGroup watchGroup);

    /**
     * 특정 그룹의 다음 표시순서 조회
     */
    @Query("SELECT COALESCE(MAX(i.displayOrder), 0) + 1 FROM UserWatchListItem i WHERE i.watchGroup = :watchGroup")
    Integer findNextDisplayOrder(@Param("watchGroup") UserWatchGroup watchGroup);

    /**
     * 특정 사용자의 모든 관심종목 조회 (그룹 정보와 함께)
     */
    @Query("SELECT i FROM UserWatchListItem i " +
            "JOIN FETCH i.watchGroup g " +
            "WHERE g.userId = :userId " +
            "ORDER BY g.displayOrder ASC, i.displayOrder ASC")
    List<UserWatchListItem> findAllByUserId(@Param("userId") long userId);
}