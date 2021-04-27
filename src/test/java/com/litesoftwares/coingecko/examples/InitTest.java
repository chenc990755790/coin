package com.litesoftwares.coingecko.examples;

import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.constant.Currency;
import com.litesoftwares.coingecko.domain.CoinPriceOrder;
import com.litesoftwares.coingecko.domain.Coins.CoinMarkets;
import com.litesoftwares.coingecko.domain.Exchanges.ExchangesTickersById;
import com.litesoftwares.coingecko.domain.PriceOrder;
import com.litesoftwares.coingecko.domain.Shared.Ticker;
import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;
import com.litesoftwares.coingecko.repository.CoinPriceOrderRepository;
import com.litesoftwares.coingecko.task.AsyncService;
import com.litesoftwares.coingecko.task.MailService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.CollectionUtils;

import javax.mail.MessagingException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@SpringBootTest
@RunWith(SpringRunner.class)
@Slf4j
public class InitTest {
    @Autowired
    private CoinPriceOrderRepository coinPriceOrderRepository;
//    @Autowired
//    private SymbolPriceRepository symbolPriceRepository;

    private int threadNum = 6;
    @Autowired
    private MailService mailService;
    @Autowired
    private AsyncService asyncService;

    @Test
    public void initHighPrice() throws InterruptedException {
        Set<String> collect = coinPriceOrderRepository.findAll().parallelStream().map(i -> i.getCoinId()).collect(Collectors.toSet());
        CoinGeckoApiClient client = new CoinGeckoApiClientImpl();
        List<CoinMarkets> coinList = client.getCoinMarkets(Currency.USD, null, null, 600, 3, false, "");
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
        int j = 0;
        for (CoinPriceOrder order : all) {
            coinList = coinList.stream().filter(i -> i.getId().equals(order.getCoinId())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(coinList)) {
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
            for (CoinMarkets list : coinList) {
                if (collect.contains(list.getId())) continue;
                try {
                    CoinPriceOrder coinPriceOrder = new CoinPriceOrder();
                    coinPriceOrder.setSymbol(list.getSymbol());
                    coinPriceOrder.setCoinId(list.getId());
                    coinPriceOrder.setPrice(new BigDecimal(list.getAth()));
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
    public void testexchange() {
        long start = System.currentTimeMillis();
        CoinGeckoApiClient client = new CoinGeckoApiClientImpl();
        ExchangesTickersById huobi = getExchangeTicker(client, "huobi");
        ExchangesTickersById okex = getExchangeTicker(client, "okex");
        ExchangesTickersById binance = getExchangeTicker(client, "binance");
        List<CoinMarkets> coinList = getCoinMarkert(client);
        final List<CoinMarkets> coinMarkets = new ArrayList<>();
        coinList.parallelStream().forEach(j -> {
            List<Ticker> huobicollect = huobi.getTickers().parallelStream().filter(i -> i.getBase().equalsIgnoreCase(j.getSymbol())
            ).collect(Collectors.toList());
            List<Ticker> okexcollect = okex.getTickers().parallelStream().filter(i -> i.getBase().equalsIgnoreCase(j.getSymbol())
            ).collect(Collectors.toList());
            List<Ticker> binanceollect = binance.getTickers().parallelStream().filter(i -> i.getBase().equalsIgnoreCase(j.getSymbol())
            ).collect(Collectors.toList());
            if (huobicollect.size() + okexcollect.size() + binanceollect.size() == 1) {
                String name = huobicollect.size() == 1 ? huobi.getName()
                        : okexcollect.size() == 1 ? okex.getName()
                        : binance.getName();
                j.setName(name);
                coinMarkets.add(j);
//                System.out.println(name + "      " + j.getSymbol());
            }
        });
        List<CoinMarkets> sortList = coinMarkets.stream().sorted((i, j) -> (int) (i.getMarketCapRank() - j.getMarketCapRank())).collect(Collectors.toList());
        sortList.stream().forEach(i -> {
            System.out.println(i.getName() + "      " + i.getSymbol() + "      " + i.getMarketCapRank());
        });
        System.out.println(sortList.size());
        System.out.println("花费时间： " + (System.currentTimeMillis() - start) / 1000);
    }

    private ExchangesTickersById getExchangeTicker(CoinGeckoApiClient client, String exchangeId) {
        ExchangesTickersById exchangesTickersById = client.getExchangesTickersById(exchangeId, null, 1, null);
        for (int i = 2; i < 7; i++) {
            exchangesTickersById.getTickers().addAll(client.getExchangesTickersById(exchangeId, null, i, null).getTickers());
        }
        return exchangesTickersById;
    }

    private List<CoinMarkets> getCoinMarkert(CoinGeckoApiClient client) {
        List<CoinMarkets> coinList = new ArrayList<>();
        for (int i = 1; i < 7; i++) {
            coinList.addAll(client.getCoinMarkets(Currency.USD, null, null, 100, i, false, ""));
        }
        return coinList;
    }

    @Test
    public void testMail() throws MessagingException {

        List<CoinPriceOrder> bySymbol = coinPriceOrderRepository.findBySymbol("eth");
        List<PriceOrder> orders = asyncService.buildNewOrder(bySymbol);
        mailService.sendhtmlmail(orders);
    }

//    @Test
//    public void testGetPrice() {
//        CoinGeckoApiClient client = new CoinGeckoApiClientImpl();
//        MarketChart coinMarketChartById = client.getCoinMarketChartById("bitcoin", "usd", 100);
//        coinMarketChartById.getPrices().stream().forEach(i -> {
//            if (coinMarketChartById.getPrices().lastIndexOf(i) == coinMarketChartById.getPrices().size() - 1) return;
//            SymbolPrice symbolPrice = new SymbolPrice();
//            Calendar instance = Calendar.getInstance();
//            instance.setTime(new Date(Long.parseLong(i.get(0))));
//            symbolPrice.setBtc(new BigDecimal(i.get(1)));
//            symbolPrice.setCurrDate(instance);
//            symbolPriceRepository.save(symbolPrice);
//        });
//    }

//    @Test
//    public void testGetOtherPrice() {
//        List<SymbolPrice> all = symbolPriceRepository.findAll();
//        CoinGeckoApiClient client = new CoinGeckoApiClientImpl();
//        MarketChart coinMarketChartById = client.getCoinMarketChartById("ethereum", "usd", 100);
//        coinMarketChartById.getPrices().stream().forEach(i -> {
//            Calendar instance = Calendar.getInstance();
//            instance.setTime(new Date(Long.parseLong(i.get(0))));
//            List<SymbolPrice> collect = all.stream().filter(j -> j.getCurrDate().compareTo(instance) == 0).collect(Collectors.toList());
//            if (CollectionUtils.isEmpty(collect)) {
//                System.out.println(new Date(Long.parseLong(i.get(0))));
//                return;
//            }
//            SymbolPrice symbolPrice = collect.get(0);
//            symbolPrice.setEth(new BigDecimal(i.get(1)));
//            symbolPriceRepository.save(symbolPrice);
//        });
//    }
//
//    @Test
//    public void testSymbolPrice(){
//        Pageable pageable = PageRequest.of(0, 100, Sort.Direction.DESC, "currDate");
//        Page<SymbolPrice> all = symbolPriceRepository.findAll(pageable);
//        System.out.println(all.getContent().size());
//    }
}
