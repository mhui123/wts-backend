package com.wts.api.repository;

import com.wts.api.entity.PortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {

    Optional<PortfolioItem> findByUserIdAndCompanyName(Long userId, String companyName);

    List<PortfolioItem> findByUserId(Long userId);
}

