package org.sokei.apps.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BinanceOrderBookUpdate {
    @JsonProperty("e")
    public String eventType; // "depthUpdate"

    @JsonProperty("E")
    public long eventTime; // Timestamp in milliseconds

    @JsonProperty("s")
    public String symbol; // Market symbol, e.g., "BTCUSDT"

    @JsonProperty("U")
    public long firstUpdateId; // First update ID in batch

    @JsonProperty("u")
    public long lastUpdateId; // Last update ID in batch

    @JsonProperty("b")
    public List<List<String>> rawBids; // Raw bid updates from Binance JSON

    @JsonProperty("a")
    public List<List<String>> rawAsks; // Raw ask updates from Binance JSON

}
