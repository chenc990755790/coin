package com.litesoftwares.coingecko.repository;

import com.litesoftwares.coingecko.domain.CoinPriceOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoinPriceOrderRepository extends JpaRepository<CoinPriceOrder,Integer> {
}
