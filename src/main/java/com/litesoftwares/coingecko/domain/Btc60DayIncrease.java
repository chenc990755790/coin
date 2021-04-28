package com.litesoftwares.coingecko.domain;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Calendar;

@Data
@Entity
@Table
public class Btc60DayIncrease {

    @Id
    @Temporal(TemporalType.TIMESTAMP)
    private Calendar currDate;
    @Column(scale = 6, precision = 16)
    private BigDecimal open;
    @Column(scale = 6, precision = 16)
    private BigDecimal close;
    @Column(scale = 6, precision = 10)
    private BigDecimal currRate;
    @Column(scale = 6, precision = 10)
    private BigDecimal days60Rate;
}
