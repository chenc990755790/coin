package com.litesoftwares.coingecko.domain;

import com.litesoftwares.coingecko.constant.ResultCode;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
public class ChartEntity {

    private List<String> dateList;

    private List<BigDecimal> rateList;

    private ResultCode status;
}

