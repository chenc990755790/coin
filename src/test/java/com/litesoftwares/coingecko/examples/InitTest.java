package com.litesoftwares.coingecko.examples;

import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.domain.CoinPriceOrder;
import com.litesoftwares.coingecko.domain.Coins.CoinList;
import com.litesoftwares.coingecko.domain.Coins.MarketChart;
import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;
import com.litesoftwares.coingecko.repository.CoinPriceOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootTest
@RunWith(SpringRunner.class)
@Slf4j
public class InitTest {
    @Autowired
    private CoinPriceOrderRepository coinPriceOrderRepository;
    @Test
    public void initHighPrice(){
        Set<String> collect = coinPriceOrderRepository.findAll().parallelStream().map(i -> i.getSymbol()).collect(Collectors.toSet());
        CoinGeckoApiClient client = new CoinGeckoApiClientImpl();
        List<CoinList> coinList = client.getCoinList();
        System.out.println("总条数； "+coinList.size());
        for (CoinList list:coinList.subList(668,coinList.size())) {
            if (collect.contains(list.getSymbol())) continue;
            try {
                MarketChart usd = client.getCoinMarketChartById(list.getId(), "usd", 200);
                BigDecimal high = new BigDecimal(usd.getPrices().get(0).get(1));
                BigDecimal newPrice;
                for (List<String> str:usd.getPrices()){
                    if (high.compareTo(newPrice = new BigDecimal(str.get(1)))<0) {
                        high = newPrice;
                    }
                }
                if (collect.contains(list.getSymbol())) continue;
                CoinPriceOrder coinPriceOrder = new CoinPriceOrder();
                coinPriceOrder.setSymbol(list.getSymbol());
                coinPriceOrder.setPrice(high);
                coinPriceOrderRepository.save(coinPriceOrder);
            } catch (Exception e) {
                log.info(e.getMessage()+"   "+list.toString());
            } finally {
                continue;
            }
        }
    }
}
