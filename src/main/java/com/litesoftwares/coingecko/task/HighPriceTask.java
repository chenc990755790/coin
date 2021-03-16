package com.litesoftwares.coingecko.task;

import com.litesoftwares.coingecko.domain.CoinPriceOrder;
import com.litesoftwares.coingecko.repository.CoinPriceOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
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

    @Scheduled(cron = "0 20 0/1 * * ?")
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
        countDownLatch.await(30, TimeUnit.MINUTES);
        mailService.sendMail(highPriceList);
    }
}
