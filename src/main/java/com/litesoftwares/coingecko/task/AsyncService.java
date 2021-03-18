package com.litesoftwares.coingecko.task;

import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.constant.Currency;
import com.litesoftwares.coingecko.domain.CoinPriceOrder;
import com.litesoftwares.coingecko.domain.Coins.CoinMarkets;
import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;
import com.litesoftwares.coingecko.repository.CoinPriceOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AsyncService {

    private CoinGeckoApiClient client = new CoinGeckoApiClientImpl();

    private final ThreadLocal<SimpleDateFormat> sdf = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS Z");
        }
    };

    @Async
    public void getOverHighPrice(List<CoinPriceOrder> coinPriceOrders, CountDownLatch latch, Vector<CoinPriceOrder> vector) {
        log.info(Thread.currentThread().getName() + " 任务开始");
        List<CoinMarkets> coinList = client.getCoinMarkets(Currency.USD, null, null, 250, 1, false, "");
        System.out.println(coinList.size());
        for (CoinPriceOrder order : coinPriceOrders) {
            try {
                List<CoinMarkets> newList = coinList.parallelStream().filter(i -> i.getId().equalsIgnoreCase(order.getCoinId())).collect(Collectors.toList());
                BigDecimal newPrice = new BigDecimal(newList.get(0).getAth()).setScale(6, RoundingMode.HALF_UP);
                if (newPrice.compareTo(order.getPrice()) > 0) {
                    order.setOldPrice(order.getPrice());
                    order.setPrice(newPrice);
                    order.setOldPriceDate(order.getUpdateTime());
                    order.setUpdateTime(sdf.get().parse(newList.get(0).getAthDate().replace("Z", " UTC")));
                    vector.add(order);
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
