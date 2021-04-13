package com.litesoftwares.coingecko.domain;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity
@Table(name = "simple_coin_price_order")
public class CoinPriceOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(length = 50)
    private String symbol;
    @Column(length = 18, scale = 6, precision = 18)
    private BigDecimal price;
    @Column(unique = true, length = 50)
    private String coinId;
    @Column(length = 6)
    private long markerOrder;
    @Column(length = 6)
    private long newMarkerOrder;

    private Date updateTime;
    @Transient
    private BigDecimal oldPrice;
    @Transient
    private Date oldPriceDate;
    @Column(length = 10)
    private String referrerName;
    @Column(length = 30)
    private String referrerReason;
}
