package com.litesoftwares.coingecko.examples;

import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.constant.Currency;
import com.litesoftwares.coingecko.domain.CoinPriceOrder;
import com.litesoftwares.coingecko.domain.Coins.*;
import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;
import com.litesoftwares.coingecko.repository.CoinPriceOrderRepository;
import com.litesoftwares.coingecko.task.MailService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@SpringBootTest
@RunWith(SpringRunner.class)
@Slf4j
public class InitTest {
    @Autowired
    private CoinPriceOrderRepository coinPriceOrderRepository;
    private int threadNum = 5;
    @Autowired
    private MailService mailService;

    @Test
    public void initHighPrice() throws InterruptedException {
        Set<String> collect = coinPriceOrderRepository.findAll().parallelStream().map(i -> i.getCoinId()).collect(Collectors.toSet());
        CoinGeckoApiClient client = new CoinGeckoApiClientImpl();
        List<CoinMarkets> coinList = client.getCoinMarkets(Currency.USD, null, null, 200, 1, false, "");
//        List<CoinList> coinList = client.getCoinList();
        System.out.println("总条数； " + coinList.size());
        int size = coinList.size() / threadNum;
        CountDownLatch countDownLatch = new CountDownLatch(threadNum);
        for (int i = 1; i <= threadNum; i++) {
            if (i == threadNum) {
                new MyThread(coinList.subList((i - 1) * size, coinList.size()), collect, countDownLatch).start();
                break;
            }
            new MyThread(coinList.subList((i - 1) * size, i * size), collect, countDownLatch).start();
        }
        countDownLatch.await();
    }

    @Test
    public void initHighPrice2() {
        List<CoinPriceOrder> all = coinPriceOrderRepository.findAll();
        CoinGeckoApiClient client = new CoinGeckoApiClientImpl();
        List<CoinMarkets> coinList = client.getCoinMarkets(Currency.USD, null, null, 200, 1, false, "");
        int j=0;
        for (CoinPriceOrder order : all) {
            coinList = coinList.stream().filter(i -> i.getId().equals(order.getCoinId())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(coinList)){
                j++;

            }
        }
        System.out.println(j);
    }

    class MyThread extends Thread {
        List<CoinMarkets> coinList;
        Set<String> collect;
        CountDownLatch countDownLatch;

        MyThread(List<CoinMarkets> coinList, Set<String> collect, CountDownLatch countDownLatch) {
            this.coinList = coinList;
            this.collect = collect;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS Z");
//            CoinGeckoApiClient client = new CoinGeckoApiClientImpl();
            for (CoinMarkets list : coinList) {
                if (collect.contains(list.getId())) continue;
                try {
//                    MarketChart usd = client.getCoinMarketChartById(list.getId(), "usd", 200);
//                    BigDecimal high = new BigDecimal(usd.getPrices().get(0).get(1));
//                    BigDecimal newPrice;
//                    for (List<String> str : usd.getPrices()) {
//                        if (high.compareTo(newPrice = new BigDecimal(str.get(1))) < 0) {
//                            high = newPrice;
//                        }
//                    }
//                    if (collect.contains(list.getSymbol())) continue;
                    CoinPriceOrder coinPriceOrder = new CoinPriceOrder();
                    coinPriceOrder.setSymbol(list.getSymbol());
                    coinPriceOrder.setPrice(new BigDecimal(list.getAth()));
                    coinPriceOrder.setCoinId(list.getId());
                    coinPriceOrder.setUpdateTime(sdf.parse(list.getAthDate().replace("Z", " UTC")));
                    coinPriceOrder.setMarkerOrder(list.getMarketCapRank());
                    coinPriceOrderRepository.save(coinPriceOrder);
                    collect.add(list.getId());
                } catch (Exception e) {
                    log.info(e.getMessage() + "   " + list.toString());
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ex) {
                    }
                } finally {
                    continue;
                }
            }
            countDownLatch.countDown();
            log.info("子线程执行完毕" + Thread.currentThread().getName());
        }
    }

    @Test
    public void test() {
        CoinGeckoApiClient client = new CoinGeckoApiClientImpl();
        Map<String, Map<String, Double>> price = client.getPrice("bitcoin", "usd");
        System.out.println(price.get("bitcoin").get("usd") + price.get("bitcoin").get("usd").getClass().getTypeName());
    }

    @Test
    public void testmail() {
        mailService.sendMail("测试邮箱");
    }
}
