package com.connto.backend.repository;

import com.connto.backend.domain.WalletTopupOffer;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTopupOfferRepository extends JpaRepository<WalletTopupOffer, UUID> {

    List<WalletTopupOffer> findByActiveTrueOrderBySortOrderAscAmountPaiseAsc();
}
