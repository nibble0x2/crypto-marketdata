package org.sokei.apps.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class BinanceSnapshot {
    private static final String BINANCE_API_URL = "https://api.binance.com/api/v3/depth?symbol=BTCUSDT&limit=5000";

    public static JsonNode fetchOrderBookSnapshot() throws Exception {
        URL url = new URL(BINANCE_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        InputStreamReader reader = new InputStreamReader(conn.getInputStream());
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(reader);
    }
}
