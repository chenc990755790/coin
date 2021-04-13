package com.litesoftwares.coingecko.repository;

import com.litesoftwares.coingecko.domain.CoinPriceOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoinPriceOrderRepository extends JpaRepository<CoinPriceOrder,Integer> {

    List<CoinPriceOrder> findBySymbol(String symbol);

}
