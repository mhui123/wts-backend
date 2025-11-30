package com.wts.service;

import com.wts.entity.DashboardDetail;
import com.wts.repository.DashboardDetailRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class DashboardDetailService {
    private final DashboardDetailRepository repo;

    public DashboardDetailService(DashboardDetailRepository repo) {
        this.repo = repo;
    }

    /**
     * 복합키(userId, tradeType, symbolName) 기준으로 존재하면 업데이트, 없으면 삽입.
     * 동시성으로 인한 제약 위반시 기존 엔티티 조회해서 반환.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DashboardDetail saveOrUpdate(DashboardDetail incoming) {
        Optional<DashboardDetail> existingOpt =
                repo.findByUserIdAndTradeTypeAndSymbolName(
                        incoming.getUserId(), incoming.getTradeType(), incoming.getSymbolName());

        if (existingOpt.isPresent()) {
            DashboardDetail existing = existingOpt.get();
            // 필요한 필드만 갱신(전체 교체 시에는 주의)
            existing.setQuantity(incoming.getQuantity());
            existing.setTotalAmountUsd(incoming.getTotalAmountUsd());
            existing.setTotalAmountKrw(incoming.getTotalAmountKrw());
            existing.setDividendUsd(incoming.getDividendUsd());
            existing.setDividendKrw(incoming.getDividendKrw());
            return repo.saveAndFlush(existing);
//            repo.saveAndFlush(existing);
        } else {
            try {
                return repo.saveAndFlush(incoming);
//                repo.saveAndFlush(incoming);
            } catch (DataIntegrityViolationException ex) {
                // 동시성으로 누군가 먼저 삽입한 경우, 다시 조회해서 반환
                return repo.findByUserIdAndTradeTypeAndSymbolName(
                                incoming.getUserId(), incoming.getTradeType(), incoming.getSymbolName())
                           .orElseThrow(() -> ex);
//                repo.findByUserIdAndTradeTypeAndSymbolName(
//                                incoming.getUserId(), incoming.getTradeType(), incoming.getSymbolName())
//                        .orElseThrow(() -> ex);
            }
        }
    }
}