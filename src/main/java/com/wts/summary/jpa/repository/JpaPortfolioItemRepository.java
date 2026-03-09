package com.wts.summary.jpa.repository;

import com.wts.summary.jpa.entity.PortfolioItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaPortfolioItemRepository extends JpaRepository<PortfolioItemEntity, Long> {

    Optional<PortfolioItemEntity> findByUserIdAndCompanyName(Long userId, String companyName);

    List<PortfolioItemEntity> findByUserId(Long userId);

    Optional<List<PortfolioItemEntity>> findByCompanyNameInAndSymbolIsNull(List<String> companyName);
    Optional<List<PortfolioItemEntity>> findBySymbolIsNull();
}

