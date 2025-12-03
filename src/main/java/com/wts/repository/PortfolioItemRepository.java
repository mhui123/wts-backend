package com.wts.repository;

import com.wts.entity.DashboardDetail;
import com.wts.entity.PortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {

    Optional<PortfolioItem> findByUserIdAndCompanyName(Long userId, String companyName);

    List<PortfolioItem> findByUserId(Long userId);
}

