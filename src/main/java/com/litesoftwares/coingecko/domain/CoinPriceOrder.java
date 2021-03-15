package com.litesoftwares.coingecko.domain;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "coin_price_order")
public class CoinPriceOrder {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private long id;

    private String symbol;

    private BigDecimal price;
}
