package com.litesoftwares.coingecko.controller;

import com.litesoftwares.coingecko.constant.ErrorCode;
import com.litesoftwares.coingecko.constant.ResultCode;
import com.litesoftwares.coingecko.domain.ChartEntity;
import com.litesoftwares.coingecko.domain.Coins.CoinMarkets;
import com.litesoftwares.coingecko.task.AsyncService;
import com.sun.istack.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@Slf4j
public class EchartController {

    @Autowired
    private AsyncService asyncService;

    @RequestMapping("/echarts")
    @ResponseBody
    public ChartEntity echarts(@RequestParam @NotNull String firstCoinid, @RequestParam @NotNull String secondCoinId) {
        ChartEntity lastedData = null;
        try {
            log.info("firstCoinid: " + firstCoinid + " secondCoinId: " + secondCoinId);
            lastedData = asyncService.getLastedData(firstCoinid, secondCoinId);
        } catch (Exception e) {
            lastedData.setStatus(new ResultCode(ErrorCode.FAIL));
        }
        return lastedData;
    }

    @RequestMapping("/")
    public String getSymbolList(Model model) {
        List<CoinMarkets> coinMarkertTop10 = asyncService.getCoinMarkertTop50();
        model.addAttribute("symbolList", asyncService.getCoinMarkertTop50());
        ChartEntity lastedData = asyncService.getLastedData(coinMarkertTop10.get(0).getId(), coinMarkertTop10.get(1).getId());
        model.addAttribute("data", lastedData);
        return "echarts";
    }

}
