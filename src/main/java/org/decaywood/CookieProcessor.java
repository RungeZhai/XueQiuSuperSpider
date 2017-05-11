package org.decaywood;

import org.decaywood.utils.FileLoader;
import org.decaywood.utils.RequestParaBuilder;
import org.decaywood.utils.URLMapper;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author: decaywood
 * @date: 2016/04/10 20:01
 */
public interface CookieProcessor {


    /**
     * 其实不需要登录也可以刷新cookie, 只需要访问任何一个页面, 例如主页
     * 处理还是一样, 在返回的response header 中获取 set-cookie 字段
     * @param website
     * @throws Exception
     */
    default void updateCookie(String website) throws Exception {

        URL url = new URL(URLMapper.MAIN_PAGE.toString());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.connect();

            String cookie = connection.getHeaderFields().get("Set-Cookie")
                    .stream()
                    .map(x -> x.split(";")[0].concat(";"))
                    .reduce("", String::concat);
            FileLoader.updateCookie(cookie, website);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

//        return;
//
//        GlobalSystemConfigLoader.loadConfig();
//
//        String areacode = System.getProperty("areaCode");
//        String userID = System.getProperty("userID");
//        String passwd = System.getProperty("password");
//        boolean rememberMe = Boolean.parseBoolean(System.getProperty("rememberMe"));
//
//        HttpURLConnection connection = null;
//        if (userID != null && passwd != null) {
//            connection = login(areacode, userID, passwd, rememberMe);
//        }
//        try {
//            connection = connection == null ?
//                    (HttpURLConnection) new URL(website).openConnection() : connection;
//            connection.connect();
//
//            String cookie = connection.getHeaderFields().get("Set-Cookie")
//                    .stream()
//                    .map(x -> x.split(";")[0].concat(";"))
//                    .filter(x -> x.contains("token=") || x.contains("s="))
//                    .reduce("", String::concat);
//            FileLoader.updateCookie(cookie, website);
//        } finally {
//            if (connection != null) connection.disconnect();
//        }

    }

    default HttpURLConnection login(String areacode,
                                    String userID,
                                    String passwd,
                                    boolean rememberMe) throws Exception {

        areacode = areacode == null ? "86" : areacode;
        if (userID == null || passwd == null) {
            throw new IllegalArgumentException("null parameter: userID or password");
        }

        RequestParaBuilder builder = new RequestParaBuilder("http://xueqiu.com/user/login")
                .addParameter("areacode", areacode)
                .addParameter("telephone", userID)
                .addParameter("password", passwd)
                .addParameter("remember_me", rememberMe ? "on" : "off");

        URL url = new URL(builder.build());
        return (HttpURLConnection) url.openConnection();
    }


}
