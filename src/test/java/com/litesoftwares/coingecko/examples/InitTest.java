package com.litesoftwares.coingecko.examples;

import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.domain.CoinPriceOrder;
import com.litesoftwares.coingecko.domain.Coins.*;
import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;
import com.litesoftwares.coingecko.repository.CoinPriceOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
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

    @Test
    public void initHighPrice() throws InterruptedException {
        Set<String> collect = coinPriceOrderRepository.findAll().parallelStream().map(i -> i.getCoinId()).collect(Collectors.toSet());
        CoinGeckoApiClient client = new CoinGeckoApiClientImpl();
        List<CoinList> coinList = client.getCoinList();
        System.out.println("总条数； " + coinList.size());
        int size = coinList.size() / 10;
        CountDownLatch countDownLatch = new CountDownLatch(1);
        for (int i = 1; i <= 1; i++) {
            if (i == 1) {
                new MyThread(coinList.subList((i - 1) * size, coinList.size()), collect, countDownLatch).start();
                break;
            }
            new MyThread(coinList.subList((i - 1) * size, i * size), collect, countDownLatch).start();
        }
        countDownLatch.await();
    }

    @Test
    public void initHighPrice2() {
//        Set<String> collect = coinPriceOrderRepository.findAll().parallelStream().map(i -> i.getSymbol()).collect(Collectors.toSet());
        CoinGeckoApiClient client = new CoinGeckoApiClientImpl();
        List<CoinList> coinList = client.getCoinList();
        System.out.println("总条数； " + coinList.size());
        List<String> arrayList = new ArrayList<String>();
        Map<String, List<CoinList>> collect = coinList.stream().collect(Collectors.groupingBy(CoinList::getSymbol));
        for (String list : collect.keySet()) {
            if (collect.get(list).size() > 1) {
                arrayList.add(list);
                log.info(collect.get(list).toString());
            }
        }
        System.out.println(arrayList.size());
    }

    class MyThread extends Thread {
        List<CoinList> coinList;
        Set<String> collect;
        CountDownLatch countDownLatch;

        MyThread(List<CoinList> coinList, Set<String> collect, CountDownLatch countDownLatch) {
            this.coinList = coinList;
            this.collect = collect;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            CoinGeckoApiClient client = new CoinGeckoApiClientImpl();
            for (CoinList list : coinList) {
                if (collect.contains(list.getId())) continue;
                try {
                    MarketChart usd = client.getCoinMarketChartById(list.getId(), "usd", 200);
                    BigDecimal high = new BigDecimal(usd.getPrices().get(0).get(1));
                    BigDecimal newPrice;
                    for (List<String> str : usd.getPrices()) {
                        if (high.compareTo(newPrice = new BigDecimal(str.get(1))) < 0) {
                            high = newPrice;
                        }
                    }
                    if (collect.contains(list.getSymbol())) continue;
                    CoinPriceOrder coinPriceOrder = new CoinPriceOrder();
                    coinPriceOrder.setSymbol(list.getSymbol());
                    coinPriceOrder.setPrice(high);
                    coinPriceOrder.setCoinId(list.getId());
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
    public void test(){
        CoinGeckoApiClient client = new CoinGeckoApiClientImpl();
        Map<String, Map<String, Double>> price = client.getPrice("bitcoin", "usd");
        System.out.println(price.get("bitcoin").get("usd")+price.get("bitcoin").get("usd").getClass().getTypeName());
    }
}
