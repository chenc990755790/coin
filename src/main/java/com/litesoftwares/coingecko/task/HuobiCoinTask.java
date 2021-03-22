package com.litesoftwares.coingecko.task;

import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.domain.CoinPriceOrder;
import com.litesoftwares.coingecko.domain.Coins.CoinMarkets;
import com.litesoftwares.coingecko.domain.Exchanges.ExchangesTickersById;
import com.litesoftwares.coingecko.domain.Shared.Ticker;
import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;
import com.litesoftwares.coingecko.repository.CoinPriceOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@Component
@Slf4j
public class HuobiCoinTask {

    @Autowired
    private CoinPriceOrderRepository coinPriceOrderRepository;

    @Autowired
    private AsyncService asyncService;

    private CoinGeckoApiClient client = new CoinGeckoApiClientImpl();

    private Vector<CoinPriceOrder> highPriceList = new Vector<>();


    @Scheduled(cron = "0 5/15 0/1 * * ?")
    public void exchangeTask() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        ExchangesTickersById huobi = getExchangeTicker(client, "huobi");
        ExchangesTickersById okex = getExchangeTicker(client, "okex");
        ExchangesTickersById binance = getExchangeTicker(client, "binance");
        List<CoinMarkets> coinList = asyncService.getCoinMarkert();
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
            }
        });
//        List<CoinMarkets> sortList = coinMarkets.stream().sorted((i, j) -> (int) (i.getMarketCapRank() - j.getMarketCapRank())).collect(Collectors.toList());
        List<CoinMarkets> huobi_global = coinMarkets.parallelStream().filter(i -> i.getName().equalsIgnoreCase("Huobi Global")).collect(Collectors.toList());
        List<CoinPriceOrder> coinPriceOrder = getCoinPriceOrder(huobi_global);
        asyncService.getOverHighPrice(coinPriceOrder, countDownLatch, highPriceList);
        countDownLatch.await();
        asyncService.sendMailCheck(highPriceList, true);
    }

    private ExchangesTickersById getExchangeTicker(CoinGeckoApiClient client, String exchangeId) {
        ExchangesTickersById exchangesTickersById = client.getExchangesTickersById(exchangeId, null, 1, null);
        for (int i = 2; i < 7; i++) {
            exchangesTickersById.getTickers().addAll(client.getExchangesTickersById(exchangeId, null, i, null).getTickers());
        }
        return exchangesTickersById;
    }

    private List<CoinPriceOrder> getCoinPriceOrder(List<CoinMarkets> huobi_global) {
        List<CoinPriceOrder> all = coinPriceOrderRepository.findAll();
        List<CoinPriceOrder> onlyHuobi = new ArrayList<>();
        all.parallelStream().forEach(i -> {
            List<CoinMarkets> collect = huobi_global.parallelStream().filter(j -> j.getSymbol().equalsIgnoreCase(i.getSymbol())).collect(Collectors.toList());
            if (collect.size() > 0) {
                onlyHuobi.add(i);
            }
        });
        return onlyHuobi;
    }


}
