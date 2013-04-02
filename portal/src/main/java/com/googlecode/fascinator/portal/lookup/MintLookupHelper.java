package com.googlecode.fascinator.portal.lookup;

import java.net.URLEncoder;
import java.util.Map;

import com.googlecode.fascinator.common.BasicHttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import com.googlecode.fascinator.common.JsonSimple;

/**
 * Helper class for requesting mint lookup urls found in "proxy-url"
 * configuration.
 * 
 * @author Shilo Banihit
 * 
 */
public class MintLookupHelper {

    public static JsonSimple get(JsonSimple systemConfig, String urlName,
            Map<String, String> reqData) throws Exception {
        String url = systemConfig.getString("http://localhost:9001/mint",
                "proxy-urls", urlName);
        StringBuilder reqStr = new StringBuilder();
        reqStr.append(url);
        if (!urlName.endsWith("Detail") && !reqData.containsKey("id")) {
            throw new Exception(
                    "Invalid request - please provide ID(s) to lookup.");
        }
        for (String key : reqData.keySet()) {
            String data = reqData.get(key);
            if (reqStr.indexOf("?") == -1) {
                reqStr.append("?");
            } else {
                reqStr.append("&");
            }
            reqStr.append(key);
            reqStr.append("=");
            reqStr.append(URLEncoder.encode(data, "utf-8"));
        }
        url = reqStr.toString();
        BasicHttpClient client = new BasicHttpClient(url);
        GetMethod get = new GetMethod(url);
        client.executeMethod(get);

        JsonSimple mintResult = new JsonSimple(get.getResponseBodyAsString());
        return mintResult;
    }
}
