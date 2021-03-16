package com.litesoftwares.coingecko.domain;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "coin_price_order")
public class CoinPriceOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(length = 12)
    private String symbol;
    @Column(length = 12, scale = 6, precision = 12)
    private BigDecimal price;
    @Column(unique = true, length = 20)
    private String coinId;
}
