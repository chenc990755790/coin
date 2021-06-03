package com.litesoftwares.coingecko.task;

import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.domain.CoinPriceOrder;
import com.litesoftwares.coingecko.domain.Exchanges.ExchangesTickersById;
import com.litesoftwares.coingecko.domain.PriceOrder;
import com.litesoftwares.coingecko.domain.Shared.Ticker;
import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;
import com.litesoftwares.coingecko.repository.CoinPriceOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.mail.MessagingException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class HuobiCoinTask {

    @Autowired
    private CoinPriceOrderRepository coinPriceOrderRepository;

    @Autowired
    private AsyncService asyncService;
    @Autowired
    private MailService mailService;

    private CoinGeckoApiClient client = new CoinGeckoApiClientImpl();

    @Scheduled(cron = "0 1/5 * * * ?")
    public void exchangeTask() throws MessagingException {
        ExchangesTickersById huobi = getExchangeTicker(client, "huobi");
        ExchangesTickersById okex = getExchangeTicker(client, "okex");
        ExchangesTickersById binance = getExchangeTicker(client, "binance");
        ExchangesTickersById ftx = getExchangeTicker(client, "ftx_spot");
        ExchangesTickersById coinbaseExchange = getExchangeTicker(client, "gdax");
        ExchangesTickersById upbit = getExchangeTicker(client, "Upbit");
        List<CoinPriceOrder> all = coinPriceOrderRepository.findAll();
        all.parallelStream().forEach(i -> {
            StringBuffer stringBuffer = new StringBuffer();
            if (containExchange(huobi, i)) stringBuffer.append(huobi.getName()).append(",");
            if (containExchange(okex, i)) stringBuffer.append(okex.getName()).append(",");
            if (containExchange(binance, i)) stringBuffer.append(binance.getName()).append(",");
            if (containExchange(ftx, i)) stringBuffer.append(ftx.getName()).append(",");
            if (containExchange(coinbaseExchange, i)) stringBuffer.append(coinbaseExchange.getName()).append(",");
            if (containExchange(upbit, i)) stringBuffer.append(upbit.getName()).append(",");
            i.setAllExchanges(i.getAllExchanges() + stringBuffer.toString());
            i.setNewExchanges(stringBuffer.toString());
            coinPriceOrderRepository.save(i);
        });
        all = all.parallelStream().filter(i -> i.getNewExchanges() != null && i.getNewExchanges().length() > 0).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(all)) return;
        Collections.sort(all, new Comparator<CoinPriceOrder>() {
            @Override
            public int compare(CoinPriceOrder o1, CoinPriceOrder o2) {
                return o1.getMarkerOrder() - o2.getMarkerOrder();
            }
        });
        List<PriceOrder> orders = asyncService.buildNewOrder(all);
        mailService.sendexchangemail(orders);
    }

    private boolean containExchange(ExchangesTickersById exchanges, CoinPriceOrder coinPriceOrder) {
        List<Ticker> huobicollect = exchanges.getTickers().parallelStream().filter(i -> i.getBase().equalsIgnoreCase(coinPriceOrder.getSymbol())
        ).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(huobicollect)) {
            return false;
        }
        if (coinPriceOrder.getAllExchanges().contains(exchanges.getName()))
            return true;
        return false;
    }

    private ExchangesTickersById getExchangeTicker(CoinGeckoApiClient client, String exchangeId) {
        ExchangesTickersById exchangesTickersById = client.getExchangesTickersById(exchangeId, null, 1, null);
        for (int i = 1; i < 3; i++) {
            exchangesTickersById.getTickers().addAll(client.getExchangesTickersById(exchangeId, null, i, null).getTickers());
        }
        return exchangesTickersById;
    }

}
