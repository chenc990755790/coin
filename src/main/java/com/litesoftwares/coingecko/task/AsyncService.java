package com.litesoftwares.coingecko.task;

import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.constant.Currency;
import com.litesoftwares.coingecko.domain.CoinPriceOrder;
import com.litesoftwares.coingecko.domain.Coins.CoinMarkets;
import com.litesoftwares.coingecko.domain.PriceOrder;
import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;
import com.litesoftwares.coingecko.repository.CoinPriceOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AsyncService {

    @Autowired
    private CoinPriceOrderRepository coinPriceOrderRepository;
    @Autowired
    private MailService mailService;

    private List<CoinMarkets> coinList = new ArrayList<>(600);

    private CoinGeckoApiClient client = new CoinGeckoApiClientImpl();

    public void setCoinList(List<CoinMarkets> coinList) {
        this.coinList = coinList;
    }

    private final ThreadLocal<SimpleDateFormat> sdf = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS Z");
        }
    };

    @Async
    public void getOverHighPrice(List<CoinPriceOrder> coinPriceOrders, CountDownLatch latch, Vector<CoinPriceOrder> vector) {
        log.info(Thread.currentThread().getName() + " 任务开始");
        for (CoinPriceOrder order : coinPriceOrders) {
            try {
                List<CoinMarkets> newList = coinList.parallelStream().filter(i -> i.getId().equalsIgnoreCase(order.getCoinId())).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(newList)) continue;
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
        log.info(Thread.currentThread().getName() + " 任务结束");
        latch.countDown();
    }

    public List<CoinMarkets> getCoinMarkert() {
        coinList.clear();
        for (int i = 1; i < 7; i++) {
            coinList.addAll(client.getCoinMarkets(Currency.USD, null, null, 100, i, false, ""));
        }
        return coinList;
    }

    public String buildListString(List<PriceOrder> highPriceList) {
        StringBuilder stringBuilder = new StringBuilder();
        for (PriceOrder order : highPriceList) {
            stringBuilder.append(order.toString()).append("\n");
        }
        return stringBuilder.toString();
    }

    public List<PriceOrder> buildNewOrder(List<CoinPriceOrder> coinPriceOrders) {
        List<PriceOrder> orders = new ArrayList<>();
        for (CoinPriceOrder order : coinPriceOrders) {
            PriceOrder priceOrder = new PriceOrder();
            BeanUtils.copyProperties(order, priceOrder);
            BigDecimal rate = priceOrder.getPrice().subtract(priceOrder.getOldPrice())
                    .multiply(priceOrder.getOldPrice()).multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_UP);
            priceOrder.setIncreaseRate(rate);
            priceOrder.setCoingeckUrl("https://www.coingecko.com/zh/%E6%95%B0%E5%AD%97%E8%B4%A7%E5%B8%81/" + order.getCoinId());
            orders.add(priceOrder);
        }
        return orders;
    }

    public void sendMailCheck(Vector<CoinPriceOrder> highPriceList, boolean isOnlyOwn) {
        if (highPriceList.size() > 0) {
            List<PriceOrder> orders = buildNewOrder(highPriceList);
            orders = orders.parallelStream().filter(i -> i.getInterval() > 0L).collect(Collectors.toList());
            orders.sort((a, b) -> (int) (a.getMarkerOrder() - b.getMarkerOrder()));
            if (orders.size() > 0) {
                mailService.sendMail(buildListString(orders), isOnlyOwn);
                log.info(orders.toString());
                if (isOnlyOwn) return;
                List<PriceOrder> finalOrders = orders;
                highPriceList.parallelStream().forEach(i -> {
                    List<PriceOrder> collect = finalOrders.parallelStream().filter(j -> j.getSymbol().equalsIgnoreCase(i.getSymbol())).collect(Collectors.toList());
                    if (collect.size() > 0)
                        coinPriceOrderRepository.save(i);
                });
            }
        }
    }
}
