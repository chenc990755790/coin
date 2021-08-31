package com.litesoftwares.coingecko.task;

import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.constant.Currency;
import com.litesoftwares.coingecko.constant.ErrorCode;
import com.litesoftwares.coingecko.constant.ResultCode;
import com.litesoftwares.coingecko.domain.Btc60DayIncrease;
import com.litesoftwares.coingecko.domain.ChartEntity;
import com.litesoftwares.coingecko.domain.CoinPriceOrder;
import com.litesoftwares.coingecko.domain.Coins.CoinMarkets;
import com.litesoftwares.coingecko.domain.Coins.MarketChart;
import com.litesoftwares.coingecko.domain.PriceOrder;
import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;
import com.litesoftwares.coingecko.repository.Btc60DayIncreaseRepository;
import com.litesoftwares.coingecko.repository.CoinPriceOrderRepository;
import com.litesoftwares.coingecko.service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.mail.MessagingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AsyncService {

    private final BigDecimal HUNDRED = new BigDecimal(100);

    @Autowired
    private MailService mailService;
    @Autowired
    private CoinPriceOrderRepository coinPriceOrderRepository;
    @Autowired
    private Btc60DayIncreaseRepository btc60DayIncreaseRepository;

    private List<CoinMarkets> coinList = new ArrayList<>(600);

    private CoinGeckoApiClient client = new CoinGeckoApiClientImpl();

    private final List<CoinPriceOrder> markOrder = new ArrayList<>();

    public void setCoinList(List<CoinMarkets> coinList) {
        this.coinList = coinList;
    }

    private final ThreadLocal<SimpleDateFormat> sdf = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS Z");
        }
    };
    private ThreadLocal<SimpleDateFormat> simpleDateFormat = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd");
        }
    };

    @Async
    public void getOverHighPrice(List<CoinPriceOrder> coinPriceOrders, CountDownLatch latch, Vector<CoinPriceOrder> vector) throws MessagingException {
//        log.info(Thread.currentThread().getName() + " 任务开始");
        for (CoinPriceOrder order : coinPriceOrders) {
            try {
                List<CoinMarkets> newList = coinList.parallelStream().filter(i -> i.getId().equalsIgnoreCase(order.getCoinId())).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(newList)) continue;
                BigDecimal newPrice = new BigDecimal(newList.get(0).getAth()).setScale(6, RoundingMode.HALF_UP);
                if (newPrice.compareTo(order.getPrice()) > 0) {
                    order.setOldPrice(order.getPrice());
                    if (order.getNewMarkerOrder() != 0) {
                        order.setMarkerOrder(order.getNewMarkerOrder());
                    } else {
                        order.setNewMarkerOrder(order.getMarkerOrder());
                    }
                    order.setNewMarkerOrder((int)newList.get(0).getMarketCapRank());
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
        for (int i = 1; i < 3; i++) {
            coinList.addAll(client.getCoinMarkets(Currency.USD, null, null, 200, i, false, ""));
        }
        return coinList;
    }

    public List<CoinMarkets> getCoinMarkertTop50() {
        return client.getCoinMarkets(Currency.USD, null, null, 50, 1, false, "");
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
//            BigDecimal rate = priceOrder.getPrice().subtract(priceOrder.getOldPrice())
//                    .divide(priceOrder.getOldPrice(), 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100));
//            priceOrder.setIncreaseRate(rate);
//            priceOrder.setCoingeckUrl("https://www.coingecko.com/en/coins/" + order.getCoinId());
//            priceOrder.setCoinmarketcapUrl("https://coinmarketcap.com/currencies/" + order.getCoinId());
//            priceOrder.setFeixiaohaoUrl("https://www.feixiaohao.com/currencies/" + order.getCoinId());
            orders.add(priceOrder);
        }
        return orders;
    }

    public void sendMailCheck(Vector<CoinPriceOrder> highPriceList, boolean isOnlyOwn) throws MessagingException {
        if (highPriceList.size() > 0) {
            List<PriceOrder> orders = buildNewOrder(highPriceList);
            orders = orders.parallelStream().filter(i -> i.getInterval() > 0L).collect(Collectors.toList());
            orders.sort((a, b) -> (int) (a.getMarkerOrder() - b.getMarkerOrder()));
            if (orders.size() > 0) {
                if (isOnlyOwn) return;
                mailService.sendhtmlmail(orders);
                log.info(orders.toString());
            }
            highPriceList.parallelStream().forEach(i -> {
                coinPriceOrderRepository.save(i);
            });
        }
    }

    public ChartEntity getLastedData(String firstCoinId, String secondCoinId) {
        ChartEntity entity = new ChartEntity();
        try {
            MarketChart first = client.getCoinMarketChartById(firstCoinId, "usd", 365);
            MarketChart second = client.getCoinMarketChartById(secondCoinId, "usd", 365);
            List<String> dateList = first.getPrices().stream().map(i ->
                    simpleDateFormat.get().format(new Date(Long.parseLong(i.get(0))))
            ).collect(Collectors.toList());
            if (first.getPrices().size() < 366 || second.getPrices().size() < 366) {
                log.error("线上数据不够365条");
                entity.setStatus(new ResultCode(ErrorCode.DATA_NOT_FULL));
                return entity;
            }
            entity.setDateList(dateList);
            List<BigDecimal> rateList = new ArrayList<>();
            for (int i = 0; i < first.getPrices().size(); i++) {
                BigDecimal firstBig = new BigDecimal(first.getPrices().get(i).get(1));
                BigDecimal secondBig = new BigDecimal(second.getPrices().get(i).get(1));
                BigDecimal divide = firstBig.divide(secondBig, 4, RoundingMode.HALF_DOWN);
                rateList.add(divide);
            }
            entity.setStatus(new ResultCode(ErrorCode.SUCCESS));
            entity.setRateList(rateList);
        } finally {
            return entity;
        }
    }

    public ChartEntity getBtc60Increase() {
        ChartEntity entity = new ChartEntity();
        try {
            List<Btc60DayIncrease> collect = btc60DayIncreaseRepository.findAll().parallelStream().filter(i -> i.getDays60Rate() != null).collect(Collectors.toList());
            List<String> dateList = collect.stream().map(i -> simpleDateFormat.get().format(i.getCurrDate().getTime())).collect(Collectors.toList());
            entity.setDateList(dateList);
            List<BigDecimal> rateList = collect.stream().map(i -> i.getDays60Rate().multiply(HUNDRED)).collect(Collectors.toList());
            entity.setRateList(rateList);
            entity.setStatus(new ResultCode(ErrorCode.SUCCESS));
        } catch (Exception e) {
            entity.setStatus(new ResultCode(ErrorCode.FAIL));
        } finally {
            return entity;
        }
    }

}
