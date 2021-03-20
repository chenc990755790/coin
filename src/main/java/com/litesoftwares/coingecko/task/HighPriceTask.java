package com.litesoftwares.coingecko.task;

import com.litesoftwares.coingecko.domain.CoinPriceOrder;
import com.litesoftwares.coingecko.repository.CoinPriceOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

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


    @PostConstruct
    public void init() {
        corePoolSize = threadPoolTaskExecutor.getCorePoolSize();
    }

    @Scheduled(cron = "0 10/15 0/1 * * ?")
    public void getCurrentPrice() throws InterruptedException {
        highPriceList.clear();
        countDownLatch = new CountDownLatch(corePoolSize);
        List<CoinPriceOrder> all = coinPriceOrderRepository.findAll();
        asyncService.getCoinMarkert();
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
        asyncService.sendMailCheck(highPriceList, false);
    }


}
