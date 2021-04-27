package com.litesoftwares.coingecko.service;

//@Service
public class PullSymbolPriceService {

//    @Autowired
//    private SymbolPriceRepository symbolPriceRepository;
//
//    private final CoinGeckoApiClient client = new CoinGeckoApiClientImpl();
//
//
//
//    public void refresh() {
//        Pageable pageable = PageRequest.of(0, 100, Sort.Direction.DESC, "curr_date");
//        Page<SymbolPrice> symbolPrices = symbolPriceRepository.findAll(pageable);
//        MarketChart btcCoin = client.getCoinMarketChartById("bitcoin", "usd", 10);
//        btcCoin.getPrices().stream().forEach(i -> {
//            if (btcCoin.getPrices().lastIndexOf(i) == btcCoin.getPrices().size() - 1) return;
//            Calendar instance = Calendar.getInstance();
//            instance.setTime(new Date(Long.parseLong(i.get(0))));
//            List<SymbolPrice> collect = symbolPrices.getContent().stream().filter(j -> j.getCurrDate().compareTo(instance) == 0).collect(Collectors.toList());
//            if (CollectionUtils.isEmpty(collect)) {
//                SymbolPrice symbolPrice = new SymbolPrice();
//                symbolPrice.setBtc(new BigDecimal(i.get(1)));
//                symbolPriceRepository.save(symbolPrice);
//            }
//        });
//        MarketChart ethCoin = client.getCoinMarketChartById("ethereum", "usd", 10);
//        ethCoin.getPrices().stream().forEach(i -> {
//            if (ethCoin.getPrices().lastIndexOf(i) == ethCoin.getPrices().size() - 1) return;
//            Calendar instance = Calendar.getInstance();
//            instance.setTime(new Date(Long.parseLong(i.get(0))));
//            List<SymbolPrice> collect = symbolPrices.getContent().stream().filter(j -> j.getCurrDate().compareTo(instance) == 0).collect(Collectors.toList());
//            if (CollectionUtils.isEmpty(collect)) {
//                return;
//            }
//            SymbolPrice symbolPrice = collect.get(0);
//            symbolPrice.setEth(new BigDecimal(i.get(1)));
//            symbolPriceRepository.save(symbolPrice);
//        });
//    }

}
