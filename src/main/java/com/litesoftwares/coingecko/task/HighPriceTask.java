package com.litesoftwares.coingecko.task;

import com.litesoftwares.coingecko.domain.CoinPriceOrder;
import com.litesoftwares.coingecko.domain.PriceOrder;
import com.litesoftwares.coingecko.repository.CoinPriceOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class HighPriceTask {

    @Autowired
    private CoinPriceOrderRepository coinPriceOrderRepository;

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    private int corePoolSize;

    private CountDownLatch countDownLatch;

    private Vector<CoinPriceOrder> highPriceList = new Vector<>();

    @Autowired
    private AsyncService asyncService;
    @Autowired
    private MailService mailService;

    @PostConstruct
    public void init() {
        corePoolSize = threadPoolTaskExecutor.getCorePoolSize();
        countDownLatch = new CountDownLatch(corePoolSize);
    }

    @Scheduled(cron = "0 0/15 0/1 * * ?")
    public void getCurrentPrice() throws InterruptedException {
        highPriceList.clear();
        List<CoinPriceOrder> all = coinPriceOrderRepository.findAll();
        int size = all.size() / corePoolSize;
        for (int i = 0; i < corePoolSize; i++) {
            if (i == corePoolSize - 1) {
                asyncService.getOverHighPrice(all.subList(i * size, all.size()), countDownLatch, highPriceList);
                break;
            }
            asyncService.getOverHighPrice(all.subList(i * size, (i + 1) * size), countDownLatch, highPriceList);
        }
        countDownLatch.await();
        log.info("所有任务结束");
        if (highPriceList.size() > 0) {
            List<PriceOrder> orders = buildNewOrder(highPriceList);
            orders = orders.parallelStream().filter(i -> i.getInterval() > 0L).collect(Collectors.toList());
            orders.sort((a, b) -> (int) (a.getMarkerOrder() - b.getMarkerOrder()));
            mailService.sendMail(buildListString(orders));
            log.info(orders.toString());
            if (orders.size() > 0) {
                highPriceList.parallelStream().forEach(i -> coinPriceOrderRepository.save(i));
            }
        }
    }

    private String buildListString(List<PriceOrder> highPriceList) {
        StringBuilder stringBuilder = new StringBuilder();
        for (PriceOrder order : highPriceList) {
            stringBuilder.append(order.toString()).append("\n");
        }
        return stringBuilder.toString();
    }

    private List<PriceOrder> buildNewOrder(List<CoinPriceOrder> coinPriceOrders) {
        List<PriceOrder> orders = new ArrayList<>();
        for (CoinPriceOrder order : coinPriceOrders) {
            PriceOrder priceOrder = new PriceOrder();
            BeanUtils.copyProperties(order, priceOrder);
            BigDecimal rate = priceOrder.getPrice().subtract(priceOrder.getOldPrice())
                    .multiply(priceOrder.getOldPrice()).multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_UP);
            priceOrder.setIncreaseRate(rate);
            priceOrder.setOldPriceDate(order.getUpdateTime());
            priceOrder.setCoingeckUrl("https://www.coingecko.com/zh/%E6%95%B0%E5%AD%97%E8%B4%A7%E5%B8%81/" + order.getCoinId());
            orders.add(priceOrder);
        }
        return orders;
    }
}
