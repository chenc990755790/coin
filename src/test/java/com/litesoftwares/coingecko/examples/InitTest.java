package com.litesoftwares.coingecko.examples;

import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.constant.Currency;
import com.litesoftwares.coingecko.domain.Btc60DayIncrease;
import com.litesoftwares.coingecko.domain.CoinPriceOrder;
import com.litesoftwares.coingecko.domain.Coins.CoinMarkets;
import com.litesoftwares.coingecko.domain.Coins.MarketChart;
import com.litesoftwares.coingecko.domain.Exchanges.ExchangesTickersById;
import com.litesoftwares.coingecko.domain.PriceOrder;
import com.litesoftwares.coingecko.domain.Shared.Ticker;
import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;
import com.litesoftwares.coingecko.repository.Btc60DayIncreaseRepository;
import com.litesoftwares.coingecko.repository.CoinPriceOrderRepository;
import com.litesoftwares.coingecko.task.AsyncService;
import com.litesoftwares.coingecko.service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.CollectionUtils;

import javax.mail.MessagingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    @Autowired
    private Btc60DayIncreaseRepository btc60DayIncreaseRepository;

    @Test
    public void initHighPrice() throws InterruptedException {
        Set<String> collect = coinPriceOrderRepository.findAll().parallelStream().map(i -> i.getCoinId()).collect(Collectors.toSet());
        CoinGeckoApiClient client = new CoinGeckoApiClientImpl();
        List<CoinMarkets> coinList = client.getCoinMarkets(Currency.USD, null, null, 600, 1, false, "");
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
    public void checkDatabaseData() {
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
                    coinPriceOrder.setMarkerOrder((int)list.getMarketCapRank());
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
    public void testexchange() throws MessagingException {
        long start = System.currentTimeMillis();
        CoinGeckoApiClient client = new CoinGeckoApiClientImpl();
        ExchangesTickersById huobi = getExchangeTicker(client, "huobi");
        ExchangesTickersById okex = getExchangeTicker(client, "okex");
        ExchangesTickersById binance = getExchangeTicker(client, "binance");
        ExchangesTickersById ftx = getExchangeTicker(client, "ftx_spot");
        ExchangesTickersById coinbaseExchange = getExchangeTicker(client, "gdax");
        ExchangesTickersById upbit = getExchangeTicker(client, "Upbit");
        List<CoinPriceOrder> all = coinPriceOrderRepository.findAll();
        all.parallelStream().forEach(i->{
            StringBuffer stringBuffer = new StringBuffer();
            if (containExchange(huobi,i.getSymbol())) stringBuffer.append("huobi").append(",");
            if (containExchange(okex,i.getSymbol())) stringBuffer.append("okex").append(",");
            if (containExchange(binance,i.getSymbol())) stringBuffer.append("binance").append(",");
            if (containExchange(ftx,i.getSymbol())) stringBuffer.append("ftx").append(",");
            if (containExchange(coinbaseExchange,i.getSymbol())) stringBuffer.append("Coinbase").append(",");
            if (containExchange(upbit,i.getSymbol())) stringBuffer.append("upbit").append(",");
            if (stringBuffer.length()>0)  stringBuffer.deleteCharAt(stringBuffer.length()-1);
            i.setAllExchanges(stringBuffer.toString());
            coinPriceOrderRepository.save(i);
        });
        Collections.sort(all, new Comparator<CoinPriceOrder>() {
            @Override
            public int compare(CoinPriceOrder o1, CoinPriceOrder o2) {
                return o1.getMarkerOrder()-o2.getMarkerOrder();
            }
        });
        List<PriceOrder> orders = asyncService.buildNewOrder(all);
        mailService.sendhtmlmail(orders);
        System.out.println(all);
        System.out.println("花费时间： " + (System.currentTimeMillis() - start) / 1000);
    }

    private boolean containExchange(ExchangesTickersById exchanges,String symbol){
        List<Ticker> huobicollect = exchanges.getTickers().parallelStream().filter(i -> i.getBase().equalsIgnoreCase(symbol)
        ).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(huobicollect)) {
            return false;
        }
        return true;
    }

    private ExchangesTickersById getExchangeTicker(CoinGeckoApiClient client, String exchangeId) {
        ExchangesTickersById exchangesTickersById = client.getExchangesTickersById(exchangeId, null, 1, null);
        for (int i = 1; i < 8; i++) {
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

    @Test
    public void day60Increase() {
        CoinGeckoApiClient client = new CoinGeckoApiClientImpl();
        MarketChart coinMarketChartById = client.getCoinMarketChartById("bitcoin", "usd", 2000);
        List<List<String>> prices = coinMarketChartById.getPrices();
        for (int i = 1; i < prices.size() - 1; i++) {
            Btc60DayIncrease increase = new Btc60DayIncrease();
            Calendar instance = Calendar.getInstance();
            instance.setTime(new Date(Long.parseLong(prices.get(i).get(0))));
            increase.setOpen(new BigDecimal(prices.get(i - 1).get(1)));
            increase.setClose(new BigDecimal(prices.get(i).get(1)));
            increase.setCurrDate(instance);
            BigDecimal divide = increase.getClose().subtract(increase.getOpen()).divide(increase.getOpen(), 4, RoundingMode.HALF_DOWN);
            increase.setCurrRate(divide);
            btc60DayIncreaseRepository.save(increase);
        }

    }

    @Test
    public void get60Increase() {
        List<Btc60DayIncrease> all = btc60DayIncreaseRepository.findAll();
        Collections.reverse(all);
        for (int i = 0; i < all.size() - 60; i++) {
            double sum = all.subList(i, i + 60).parallelStream().mapToDouble(j -> j.getCurrRate().doubleValue()).sum();
            Btc60DayIncrease increase = all.get(i);
            increase.setDays60Rate(new BigDecimal(sum));
            btc60DayIncreaseRepository.save(increase);
        }
    }

    @Test
    public void testUpdateLastedData(){
        Pageable pageable = PageRequest.of(0, 60, Sort.Direction.DESC, "currDate");
        Page<Btc60DayIncrease> all = btc60DayIncreaseRepository.findAll(pageable);
        double sum = all.getContent().stream().mapToDouble(i -> i.getCurrRate().doubleValue()).sum();
        Btc60DayIncrease increase = all.getContent().get(0);
        System.out.println(increase.getCurrDate().getTime());

    }

}
