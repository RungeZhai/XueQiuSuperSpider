import entryFirst.UserInfoToDBAcceptor;
import mapperTest.TestCaseGenerator;
import org.decaywood.collector.*;
import org.decaywood.entity.*;
import org.decaywood.entity.trend.StockTrend;
import org.decaywood.filter.PageKeyFilter;
import org.decaywood.mapper.cubeFirst.CubeToCubeWithLastBalancingMapper;
import org.decaywood.mapper.cubeFirst.CubeToCubeWithTrendMapper;
import org.decaywood.mapper.dateFirst.DateToLongHuBangStockMapper;
import org.decaywood.mapper.industryFirst.IndustryToStocksMapper;
import org.decaywood.mapper.stockFirst.StockToLongHuBangMapper;
import org.decaywood.mapper.stockFirst.StockToStockWithAttributeMapper;
import org.decaywood.mapper.stockFirst.StockToStockWithStockTrendMapper;
import org.decaywood.mapper.stockFirst.StockToVIPFollowerCountEntryMapper;
import org.decaywood.utils.MathUtils;
import org.junit.Test;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URL;
import java.rmi.RemoteException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author: decaywood
 * @date: 2015/11/24 14:06
 */
public class StreamTest {

    static {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager(){
            public X509Certificate[] getAcceptedIssuers(){return null;}
            public void checkClientTrusted(X509Certificate[] certs, String authType){}
            public void checkServerTrusted(X509Certificate[] certs, String authType){}
        }};

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            ;
        }
    }


    //一阳穿三线个股
    @Test
    public void yiyinsanyang() throws RemoteException {

        List<Stock> stocks = TestCaseGenerator.generateStocks();

        StockToStockWithAttributeMapper attributeMapper = new StockToStockWithAttributeMapper();
        StockToStockWithStockTrendMapper trendMapper = new StockToStockWithStockTrendMapper();

        Predicate<Entry<String, Stock>> predicate = x -> {

            if(x.getValue().getStockTrend().getHistory().isEmpty()) return false;
            List<StockTrend.TrendBlock> history = x.getValue().getStockTrend().getHistory();
            StockTrend.TrendBlock block = history.get(history.size() - 1);
            double close = Double.parseDouble(block.getClose());
            double open = Double.parseDouble(block.getOpen());
            double ma5 = Double.parseDouble(block.getMa5());
            double ma10 = Double.parseDouble(block.getMa10());
            double ma30 = Double.parseDouble(block.getMa30());

            double max = Math.max(close, open);
            double min = Math.min(close, open);

            return close > open && max >= MathUtils.max(ma5, ma10, ma30) && min <= MathUtils.min(ma5, ma10, ma30);

        };

        List<String> filteredStocks = stocks.parallelStream()
                .map(x -> new Entry<>(x.getStockName(), attributeMapper.andThen(trendMapper).apply(x)))
                .filter(predicate)
                .map(Entry::getKey)
                .collect(Collectors.toList());

        if (filteredStocks.size() == 0) {
            System.out.println("\n没有满足条件的个股\n");
        } else {
            filteredStocks.parallelStream()
                    .forEach(System.out::println);
        }

//        stocks.parallelStream()
//                .map(x -> new Entry<>(x.getStockName(), attributeMapper.andThen(trendMapper).apply(x)))
//                .filter(predicate)
//                .map(Entry::getKey)
//                .forEach(System.out::println);

    }

    /**
     * 首先获取沪深板块热点新闻
     * 按关键字过滤页面
     * @throws RemoteException
     */
    @Test
    public void findNewsUcareAbout() throws RemoteException {
        List<URL> news = new HuShenNewsRefCollector(HuShenNewsRefCollector.Topic.TOTAL, 2).get();
        List<URL> res = news.parallelStream().filter(new PageKeyFilter("万孚生物", false)).collect(Collectors.toList());

        List<URL> regexRes = news.parallelStream().filter(new PageKeyFilter("万孚生物", true)).collect(Collectors.toList());
        for (URL re : regexRes) {
            System.out.println("Regex : " + re);
        }
        for (URL re : res) {
            System.out.println("nonRegex : " + re);
        }
    }


    //创业板股票大V统计 （耗时过长）
    @Test
    public void getMarketStockFundTrend() {
        try {
            MarketQuotationsRankCollector collector = new MarketQuotationsRankCollector(
                    MarketQuotationsRankCollector.StockType.GROWTH_ENTERPRISE_BOARD,
                    MarketQuotationsRankCollector.ORDER_BY_VOLUME, 500);
            StockToVIPFollowerCountEntryMapper mapper1 = new StockToVIPFollowerCountEntryMapper(3000, 300);//搜集每个股票的粉丝
            UserInfoToDBAcceptor acceptor = new UserInfoToDBAcceptor();//写入数据库
            collector.get()
                    .parallelStream() //并行流
                    .map(mapper1)
                    .forEach(acceptor);//结果写入数据库
        } catch (RemoteException e) {
            System.out.println(e.getMessage());
        }
    }


    //统计股票5000粉以上大V个数，并以行业分类股票 （耗时过长）
 /*   @Test
    public void getStocksWithVipFollowersCount() {
        CommissionIndustryCollector collector = new CommissionIndustryCollector();//搜集所有行业
        IndustryToStocksMapper mapper = new IndustryToStocksMapper();//搜集每个行业所有股票
        StockToVIPFollowerCountEntryMapper mapper1 = new StockToVIPFollowerCountEntryMapper(5000, 300);//搜集每个股票的粉丝
        UserInfoToDBAcceptor acceptor = new UserInfoToDBAcceptor();//写入数据库

        List<Entry<Stock, Integer>> res = collector.get()
                .parallelStream() //并行流
                .map(mapper)
                .flatMap(Collection::stream)
                .map(mapper1)
                .peek(acceptor)
                .collect(Collectors.toList());
        for (Entry<Stock, Integer> re : res) {
            System.out.println(re.getKey().getStockName() + " -> 5000粉丝以上大V个数  " + re.getValue());
        }
    }*/

    //最赚钱组合最新持仓以及收益走势、大盘走势
    @Test
    public void MostProfitableCubeDetail() throws RemoteException {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2015, Calendar.OCTOBER, 20);
        Date from = calendar.getTime();
        calendar.set(2015, Calendar.NOVEMBER, 25);
        Date to = calendar.getTime();
        MostProfitableCubeCollector cubeCollector = new MostProfitableCubeCollector( MostProfitableCubeCollector.Market.CN,
                MostProfitableCubeCollector.ORDER_BY.DAILY);
        CubeToCubeWithLastBalancingMapper mapper = null;
        try {
            mapper = new CubeToCubeWithLastBalancingMapper();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        CubeToCubeWithTrendMapper mapper1 = new CubeToCubeWithTrendMapper(from, to);
        List<Cube> cubes = cubeCollector.get().parallelStream().map(mapper.andThen(mapper1)).collect(Collectors.toList());
        for (Cube cube : cubes) {
            System.out.print(cube.getName() + " 总收益: " + cube.getTotal_gain());
            System.out.println(" 最新持仓 " + cube.getRebalancing().getHistory().get(1).toString());
        }
    }


    //获取热股榜股票信息
    @Test
    public void HotRankStockDetail() throws RemoteException {
        StockScopeHotRankCollector collector = new StockScopeHotRankCollector(StockScopeHotRankCollector.Scope.US_WITHIN_24_HOUR);
        StockToStockWithAttributeMapper mapper1 = new StockToStockWithAttributeMapper();
        StockToStockWithStockTrendMapper mapper2 = new StockToStockWithStockTrendMapper();
        List<Stock> stocks = collector.get().parallelStream().map(mapper1.andThen(mapper2)).collect(Collectors.toList());
        for (Stock stock : stocks) {
            System.out.print(stock.getStockName() + " -> ");
            System.out.print(stock.getAmplitude() + " " + stock.getOpen() + " " + stock.getHigh() + " and so on...");
            System.out.println(" trend size: " + stock.getStockTrend().getHistory().size());
        }
    }


    //获得某个行业所有股票的详细信息和历史走势 比如畜牧业
    @Test
    public void IndustryStockDetail() throws RemoteException {

        CommissionIndustryCollector collector = new CommissionIndustryCollector();
        IndustryToStocksMapper mapper = new IndustryToStocksMapper();
        StockToStockWithAttributeMapper mapper1 = new StockToStockWithAttributeMapper();
        StockToStockWithStockTrendMapper mapper2 = new StockToStockWithStockTrendMapper();
        Map<Industry, List<Stock>> res = collector.get()
                .parallelStream()
                .filter(x -> x.getIndustryName().equals("畜牧业"))
                .map(mapper)
                .flatMap(Collection::stream)
                .map(mapper1.andThen(mapper2))
                .collect(Collectors.groupingBy(Stock::getIndustry));

        for (Map.Entry<Industry, List<Stock>> entry : res.entrySet()) {
            for (Stock stock : entry.getValue()) {
                System.out.print(entry.getKey().getIndustryName() + " -> " + stock.getStockName() + " -> ");
                System.out.print(stock.getAmount() + " " + stock.getChange() + " " + stock.getDividend() + " and so on...");
                System.out.println(" trend size: " + stock.getStockTrend().getHistory().size());
            }
        }

    }


    //按行业分类获取所有股票
    @Test
    public void IndustryStockInfo() throws RemoteException {

        CommissionIndustryCollector collector = new CommissionIndustryCollector();
        IndustryToStocksMapper mapper = new IndustryToStocksMapper();
        Map<Industry, List<Stock>> res = collector.get()
                .parallelStream()
                .map(mapper)
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(Stock::getIndustry));

        for (Map.Entry<Industry, List<Stock>> entry : res.entrySet()) {
            for (Stock stock : entry.getValue()) {
                System.out.println(entry.getKey().getIndustryName() + " -> " + stock.getStockName());
            }
        }

    }

    @Test
    public void testStringOccurrence() {
        String str = "symbolundefined.com";
        System.out.println(str.indexOf("symbol"));
        boolean bool = str.contains("symbolundefined");
        int index = str.indexOf("symbol"); // 0

        int lastIndex = str.lastIndexOf("e");
        int indexFrom = str.indexOf("e", 11);
    }



    //游资追踪
    @Test
    public void LongHuBangTracking() throws RemoteException {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2017, Calendar.MAY, 1);
        Date from = calendar.getTime();
        calendar.set(2017, Calendar.MAY, 7);
        Date to = calendar.getTime();
        DateRangeCollector collector = new DateRangeCollector(from, to);
        DateToLongHuBangStockMapper mapper = new DateToLongHuBangStockMapper();
        StockToLongHuBangMapper mapper1 = new StockToLongHuBangMapper();
        List<LongHuBangInfo> s = collector.get()
                .parallelStream()
                .map(mapper)
                .flatMap(List::stream).map(mapper1)
                .filter(x -> x.bizsunitInBuyList("青岛", true))
                .sorted(Comparator.comparing(LongHuBangInfo::getDate))
                .collect(Collectors.toList());
        if (s.size() == 0) {
            System.out.println("无符合条件数据");
        } else {
            for (LongHuBangInfo info : s) {
                System.out.println(info.getDate() + " -> " + info.getStock().getStockName());
            }
        }

    }

}
