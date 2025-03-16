package org.sokei.apps.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.aeron.Aeron;
import io.aeron.ExclusivePublication;
import io.aeron.Publication;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.marketdata.sbe.MessageHeaderEncoder;
import org.marketdata.sbe.OrderbookMsgEncoder;
import org.marketdata.sbe.PrintOBEncoder;

import java.net.URI;
import java.nio.ByteBuffer;

public class BinanceDepthMarketDataGW {
    private static final String AERON_CHANNEL = "aeron:udp?endpoint=localhost:40123";
    private static final int STREAM_ID = 1001;
    private static final String BINANCE_DEPTH_WS_URL = "wss://stream.binance.com:9443/ws/btcusdt@depth@1000ms";
    private static ObjectMapper mapper = new ObjectMapper();
    private static int count = 0;

    public static void main(String[] args) {
        // 1️⃣ Start Media Driver + Archive
        Aeron.Context ctx = new Aeron.Context();//standalone
        Aeron aeron = Aeron.connect(ctx);

        // 2️⃣ Start Exclusive Publication
        ExclusivePublication publication = aeron.addExclusivePublication(AERON_CHANNEL, STREAM_ID);

        System.out.println("Started Aeron Pub");
        try {
            final JsonNode snapshot = BinanceSnapshot.fetchOrderBookSnapshot();
            if (snapshot != null) {
                // Encode order book into SBE format
                MutableDirectBuffer sbeDataBuffer = encodeOrderBookFromSnapshot(snapshot);
                // Publish data over Aeron
                sendCommand(sbeDataBuffer, publication);
                System.out.println(snapshot);
                System.out.println("Order book snapshot published successfully.");
            }

            // Step 3: Connect to WebSocket
            WebSocketClient client = new WebSocketClient(new URI(BINANCE_DEPTH_WS_URL)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    System.out.println("Connected to Binance WebSocket");
                }

                @Override
                public void onMessage(String message) {
                    try {
                        JsonNode update = mapper.readTree(message);
                        final MutableDirectBuffer sbeDataBuffer = encodeOrderBookFromRealTimeUpdates(update);
                        if (sbeDataBuffer == null) {
                            return;
                        }

                        sendCommand(sbeDataBuffer, publication);
                        System.out.println("Published real-time order book update.");
                        //send print commands time to time
                        if (count++ % 10 == 0) {
                            sendCommand(encodePrintOB(), publication);
                            System.out.println("Sent printOB command");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("WebSocket closed, reconnecting...");
                    reconnect();
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };

            client.connect();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("Exiting");
    }

    private static void sendCommand(final MutableDirectBuffer mutableDirectBuffer, final Publication publication) {
        while (true) {
            long x = publication.offer(mutableDirectBuffer, 0, mutableDirectBuffer.capacity());
            if (x >= 0) {
                break;
            }
            System.out.println("Backpressure, retrying..." + x);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static MutableDirectBuffer encodeOrderBook(final long firstUpdateId, final long lastUpdateId, final String symbol,
                                                       final JsonNode bids, final JsonNode asks) {
        //TODO right now we dont inject more than 10 bids & ask together to keep msg size small
        //leverage AddBid & AddAsk msgs to fragement it
        final int allowedSize = 10;
        final int bufferSize = calculateBufferSize(allowedSize, allowedSize);
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bufferSize);
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(byteBuffer);

        final OrderbookMsgEncoder encoder = new OrderbookMsgEncoder();
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();

        encoder.wrapAndApplyHeader(unsafeBuffer, 0, headerEncoder)
                .symbol(symbol)
                .firstUpdateId(firstUpdateId)
                .lastUpdateId(lastUpdateId);

        OrderbookMsgEncoder.BidsEncoder bidsEncoder = encoder.bidsCount(allowedSize);
        int count = 0;
        for (JsonNode bid : bids) {
            if (count++ >= allowedSize) {
                break;
            }
            bidsEncoder.next()
                    .price((long) (bid.get(0).asDouble() * 1e8))  // Convert to fixed-precision
                    .quantity((long) (bid.get(1).asDouble() * 1e8));
        }

        OrderbookMsgEncoder.AsksEncoder asksEncoder = encoder.asksCount(allowedSize);
        count = 0;
        for (JsonNode ask : asks) {
            if (count++ >= allowedSize) {
                break;
            }
            asksEncoder.next()
                    .price((long) (ask.get(0).asDouble() * 1e8))  // Convert to fixed-precision
                    .quantity((long) (ask.get(1).asDouble() * 1e8));
        }

        System.out.println(encoder.toString());
        return encoder.buffer();
    }

    private static MutableDirectBuffer encodeOrderBookFromSnapshot(JsonNode snapshot) {
        return encodeOrderBook(0, snapshot.get("lastUpdateId").asLong(), "BTCUSDT", snapshot.get("bids"), snapshot.get("asks"));
    }

    private static MutableDirectBuffer encodePrintOB() {
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(MessageHeaderEncoder.ENCODED_LENGTH);
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(byteBuffer);

        final PrintOBEncoder printOBEncoder = new PrintOBEncoder();
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();

        printOBEncoder.wrapAndApplyHeader(unsafeBuffer, 0, headerEncoder);
        return printOBEncoder.buffer();
    }

    private static MutableDirectBuffer encodeOrderBookFromRealTimeUpdates(JsonNode update) {
        String eventType = update.get("e").asText();
        if ("depthUpdate".equals(eventType)) {
            return encodeOrderBook(update.get("U").asLong(), update.get("u").asLong(), "BTCUSDT", update.get("b"), update.get("a"));
        }
        return null;
    }

    private static int calculateBufferSize(int numBids, int numAsks) {
        int headerSize = MessageHeaderEncoder.ENCODED_LENGTH;  // 8 bytes
        int fixedFieldsSize = 20 + 8 + 8;  // symbol (20 bytes) + FirstUpdateId + LastUpdateId

        int bidsSize = 4 + (numBids * 16);  // groupSizeEncoding (4 bytes) + (numBids * 16 bytes per entry)
        int asksSize = 4 + (numAsks * 16);  // groupSizeEncoding (4 bytes) + (numAsks * 16 bytes per entry)

        return headerSize + fixedFieldsSize + bidsSize + asksSize;
    }
}