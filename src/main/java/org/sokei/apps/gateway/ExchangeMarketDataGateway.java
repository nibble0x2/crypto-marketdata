package org.sokei.apps.gateway;

import io.aeron.Aeron;
import io.aeron.Publication;
import org.agrona.concurrent.UnsafeBuffer;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class ExchangeMarketDataGateway {
//    private static final String BINANCE_WS_URL = "wss://stream.binance.com:9443/ws/btcusdt@trade";
    private static final String BINANCE_WS_URL = "wss://stream.binance.com:9443/ws/btcusdt@depth20@1000ms";
    private static final String AERON_CHANNEL = "aeron:udp?endpoint=localhost:40123";
    private static final int STREAM_ID = 1001;

    private static final int FRAGMENT_SIZE = 1024 * 1024; // 1 MB fragment size

    public static void main(String[] args) {
        Aeron.Context ctx = new Aeron.Context();//standalone

        try {
            Aeron aeron = Aeron.connect(ctx);
            Publication publication = aeron.addPublication(AERON_CHANNEL, STREAM_ID);

            WebSocketClient client = new WebSocketClient(new URI(BINANCE_WS_URL)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    System.out.println("Connected to Binance WebSocket");
                }

                @Override
                public void onMessage(String message) {
                    System.out.println("Publishing to Aeron: " + message);
                    byte[] data = message.getBytes();
                    UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(data.length));
                    buffer.putBytes(0, data);

                    //pace control
//                    try {
//                        TimeUnit.SECONDS.sleep(1);
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }

                    while (true) {
                        long val = publication.offer(buffer, 0, data.length);
                        if (val > 0) {
                            break;
                        }
                        System.out.println("Retrying publish... and value was :: [" + val + "]");
                        try {
                            TimeUnit.MILLISECONDS.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("WebSocket Closed: " + reason);
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("Error: " + ex.getMessage());
                }
            };

            client.setConnectionLostTimeout(0);
            client.connect();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}

