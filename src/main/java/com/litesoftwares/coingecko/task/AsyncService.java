package com.litesoftwares.coingecko.task;

import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.constant.Currency;
import com.litesoftwares.coingecko.domain.CoinPriceOrder;
import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;
import com.litesoftwares.coingecko.repository.CoinPriceOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

@Service
@Slf4j
public class AsyncService {

    private CoinGeckoApiClient client = new CoinGeckoApiClientImpl();
    @Autowired
    private CoinPriceOrderRepository coinPriceOrderRepository;

    @Async
    public void getOverHighPrice(List<CoinPriceOrder> coinPriceOrders, CountDownLatch latch, Vector<CoinPriceOrder> vector) {
        List<CoinPriceOrder> orders = new ArrayList<>();
        for (CoinPriceOrder order : coinPriceOrders) {
            try {
                Map<String, Map<String, Double>> price = client.getPrice(order.getCoinId(), Currency.USD);
                BigDecimal newPrice = new BigDecimal(price.get("bitcoin").get("usd"));
                if (newPrice.compareTo(order.getPrice()) > 0) {
                    order.setPrice(newPrice);
                    orders.add(order);
                    coinPriceOrderRepository.save(order);
                    log.info(order.toString());
                }
            } finally {
                continue;
            }
        }
        latch.countDown();
    }
}
