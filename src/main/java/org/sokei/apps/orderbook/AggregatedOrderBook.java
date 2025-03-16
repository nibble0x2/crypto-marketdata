package org.sokei.apps.orderbook;

import io.aeron.Aeron;
import io.aeron.FragmentAssembler;
import io.aeron.Subscription;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.NoOpIdleStrategy;

public class AggregatedOrderBook {
    private static final String AERON_CHANNEL = "aeron:udp?endpoint=localhost:40123";
    private static final int STREAM_ID = 1001;

    public static void main(String[] args) {
        Aeron.Context ctx = new Aeron.Context(); // Connect to standalone Media Driver

        try (Aeron aeron = Aeron.connect(ctx);
             Subscription subscription = aeron.addSubscription(AERON_CHANNEL, STREAM_ID)) {

            System.out.println("Listening for Binance market data...");
            IdleStrategy idleStrategy = new NoOpIdleStrategy();

            FragmentAssembler handler = new FragmentAssembler(
                    (buffer, offset, length, header) -> {
                        byte[] data = new byte[length];
                        buffer.getBytes(offset, data);
                        String message = new String(data);
                        System.out.println("Received: " + message);
                    }
            );

            while (true) {
                int fragments = subscription.poll(handler, 100);
                idleStrategy.idle(fragments);
            }
        }
    }
}

