package org.decaywood.mapper.industryFirst;

import com.fasterxml.jackson.databind.JsonNode;
import org.decaywood.entity.Industry;
import org.decaywood.entity.Stock;
import org.decaywood.mapper.AbstractMapper;
import org.decaywood.timeWaitingStrategy.TimeWaitingStrategy;
import org.decaywood.utils.EmptyObject;
import org.decaywood.utils.RequestParaBuilder;
import org.decaywood.utils.URLMapper;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: decaywood
 * @date: 2015/11/23 14:04
 */

/**
 * 行业 -> 该行业股票 映射器
 * 根据行业获取该行业的所有股票
 * URL 示例:
 * https://xueqiu.com/industry/quote_order.json?page=1&size=500&order=desc&orderBy=percent&exchange=CN&plate=1_2_2&firstName=1&secondName=1_2&level2code=A03
 *
 */
public class IndustryToStocksMapper extends AbstractMapper<Industry, List<Stock>> {

    /**
     * @param strategy 超时等待策略（null则设置为默认等待策略）
     */
    public IndustryToStocksMapper(TimeWaitingStrategy strategy) throws RemoteException {
        super(strategy);
    }

    public IndustryToStocksMapper() throws RemoteException {
        this(null);
    }

    @Override
    public List<Stock> mapLogic(Industry industry) throws Exception {

        if(industry == null || industry == EmptyObject.emptyIndustry) return new ArrayList<>();

        String target = URLMapper.INDUSTRY_JSON.toString();
        RequestParaBuilder builder = new RequestParaBuilder(target);
        builder.addParameter("page", 1)
                .addParameter("size", 500)
                .addParameter("order", "desc")
                .addParameter("orderBy", "percent");
        String info = industry.getIndustryInfo();
        if (info.startsWith("#")) info = info.substring(1);
        for (String s : info.split("&")) {
            String[] keyAndVal = s.split("=");
            builder.addParameter(keyAndVal[0], keyAndVal[1]);
        }
        URL url = new URL(builder.build());

        String json = request(url);
        JsonNode jsonNode = mapper.readTree(json);

        List<Stock> stocks = parserJson(jsonNode);

        // stock 和行业(industry)做一个关联
        stocks.stream().forEach(x -> x.setIndustry(industry));

        return stocks;
    }


    private List<Stock> parserJson(JsonNode node) {

        List<Stock> stocks = new ArrayList<>();

        JsonNode data = node.get("data");
        for (JsonNode jsonNode : data) {
            Stock stock = new Stock(jsonNode.get("name").asText(), jsonNode.get("symbol").asText());
            stocks.add(stock);
        }
        return stocks;

    }


}
