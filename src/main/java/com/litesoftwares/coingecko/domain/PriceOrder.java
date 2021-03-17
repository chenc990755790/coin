package com.litesoftwares.coingecko.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class PriceOrder {

    private String symbol;

    private Long markerOrder;

    private BigDecimal price;

    private BigDecimal oldPrice;

    private BigDecimal increaseRate;

    private Date oldPriceDate;

    private Long interval;

    private String coingeckUrl;

    public Long getInterval() {
        if (oldPriceDate == null) return 0L;
        return (new Date().getTime() - oldPriceDate.getTime()) / 24 / 60 / 60 / 1000;
    }
}
