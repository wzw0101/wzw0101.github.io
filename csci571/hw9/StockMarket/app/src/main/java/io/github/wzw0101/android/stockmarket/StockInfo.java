package io.github.wzw0101.android.stockmarket;

import java.util.Random;

public class StockInfo {

//    static StockInfo[] mock() {
//        final String tickerPrefix = "tk_";
//        final String desc = "Hello world";
//        final String delta = "$0.00(0.00%)";
//        Random random = new Random();
//
//        StockInfo[] infos = new StockInfo[10];
//        for (int i = 0; i < infos.length; i += 1) {
//            infos[i] = new StockInfo();
//            infos[i].ticker = tickerPrefix + i;
//            infos[i].desc = desc;
//            infos[i].price = "$" + String.format("%.2f", random.nextDouble() * 100);
//            infos[i].delta = delta;
//        }
//        return infos;
//    }

    public String ticker;
    public String desc;
    public double price;
    public double d;
    public double dp;
}
