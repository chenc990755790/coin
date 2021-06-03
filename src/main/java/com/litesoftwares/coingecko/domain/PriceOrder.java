package com.litesoftwares.coingecko.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class PriceOrder {

    private String symbol;

    private int markerOrder;

    private int newMarkerOrder;

    private BigDecimal price;

    private BigDecimal oldPrice;

    private BigDecimal increaseRate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date oldPriceDate;

    private Long interval;

    private String coingeckUrl;

    private String coinmarketcapUrl;

    private String feixiaohaoUrl;

    private String referrerName;

    private String referrerReason;

    private String allExchanges;

    private String newExchanges;

    public Long getInterval() {
        if (oldPriceDate == null) return 0L;
        return (System.currentTimeMillis() - oldPriceDate.getTime()) / 24 / 60 / 60 / 1000;
    }

    @Override
    public String toString() {
        return symbol + "\001" + markerOrder + "\001" + newMarkerOrder +"\001" + price +"\001" + oldPrice +
                        "\001" + increaseRate + "\001" + oldPriceDate +"\001" + interval +"\001" + coingeckUrl +
                        "\001" + coinmarketcapUrl +"\001" + feixiaohaoUrl +"\001" + referrerName +
                        "\001" + referrerReason +'\r';
    }
}
