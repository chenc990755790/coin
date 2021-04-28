package com.litesoftwares.coingecko.task;

import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.domain.Btc60DayIncrease;
import com.litesoftwares.coingecko.domain.Coins.MarketChart;
import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;
import com.litesoftwares.coingecko.repository.Btc60DayIncreaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Component
public class Btc60DayIncreaseTask {

    private final CoinGeckoApiClient client = new CoinGeckoApiClientImpl();
    @Autowired
    private Btc60DayIncreaseRepository btc60DayIncreaseRepository;

    @Scheduled(cron = "0 0 8 * * ?")
    public void getServerDataToDataBase() {
        MarketChart coinMarketChartById = client.getCoinMarketChartById("bitcoin", "usd", 2);
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
        update50DayIncrease();
    }

    private void update50DayIncrease() {
        Pageable pageable = PageRequest.of(0, 60, Sort.Direction.DESC, "currDate");
        Page<Btc60DayIncrease> all = btc60DayIncreaseRepository.findAll(pageable);
        double sum = all.getContent().stream().mapToDouble(i -> i.getCurrRate().doubleValue()).sum();
        Btc60DayIncrease increase = all.getContent().get(0);
        increase.setDays60Rate(new BigDecimal(sum));
        btc60DayIncreaseRepository.save(increase);
    }

}
