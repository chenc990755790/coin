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
        log.info(Thread.currentThread().getName() + " 任务开始");
        for (CoinPriceOrder order : coinPriceOrders) {
            try {
                Map<String, Map<String, Double>> price = client.getPrice(order.getCoinId(), Currency.USD);
                BigDecimal newPrice = new BigDecimal(price.get("bitcoin").get("usd"));
                if (newPrice.compareTo(order.getPrice()) > 0) {
                    order.setPrice(newPrice);
                    vector.add(order);
                    coinPriceOrderRepository.save(order);
                    log.info(order.toString());
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            } finally {
                continue;
            }
        }
        latch.countDown();
        log.info(Thread.currentThread().getName() + " 任务结束");
    }
}
