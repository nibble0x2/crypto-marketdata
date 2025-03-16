package org.sokei.apps.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BinanceOrderBookSnapshot {
    public List<List<String>> asks;
    public List<List<String>> bids;
}