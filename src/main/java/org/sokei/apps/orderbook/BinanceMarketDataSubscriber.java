package org.sokei.apps.orderbook;

import io.aeron.Aeron;
import io.aeron.FragmentAssembler;
import io.aeron.Subscription;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.NoOpIdleStrategy;
import org.marketdata.sbe.MessageHeaderDecoder;
import org.marketdata.sbe.OrderbookMsgDecoder;
import org.marketdata.sbe.PrintOBDecoder;

import java.util.Iterator;

public class BinanceMarketDataSubscriber {
    private static final String CHANNEL = "aeron:udp?endpoint=localhost:40123";
    private static final int STREAM_ID = 1001;

    private static final OrderBook orderBook = new OrderBook();

    private static void printOB() {
        orderBook.print(10000);
    }

    private static void processOrderBookMessage(OrderbookMsgDecoder orderbookMsg) {
        // Extract the relevant fields from the message and update the order book
        System.out.println(orderbookMsg.toString());
        String symbol = orderbookMsg.symbol();

        //TODO use firstUpdated & lastUpdated
        //long firstUpdateId = orderbookMsg.firstUpdateId();
        //long lastUpdateId = orderbookMsg.lastUpdateId();

        // Process the Bids group
//        System.out.println(orderbookMsg.bids().count());
        final Iterator<OrderbookMsgDecoder.BidsDecoder> bidsDecoder = orderbookMsg.bids().iterator();
        while (bidsDecoder.hasNext()) {
            OrderbookMsgDecoder.BidsDecoder bidDecoder = bidsDecoder.next();
            double bidPrice = bidDecoder.price() / 1e8;
            double bidQuantity = bidDecoder.quantity() / 1e8;
            orderBook.updateBid(bidPrice, bidQuantity);
        }

        // Process the Asks group
//        System.out.println(orderbookMsg.asks().count());
        final Iterator<OrderbookMsgDecoder.AsksDecoder> asksDecoderIterator = orderbookMsg.asks().iterator();
        while (asksDecoderIterator.hasNext()) {
            OrderbookMsgDecoder.AsksDecoder asksDecoder = asksDecoderIterator.next();
            double askPrice = asksDecoder.price() / 1e8;
            double askQuantity = asksDecoder.quantity() / 1e8;
            orderBook.updateAsk(askPrice, askQuantity);
        }
    }

    public static void main(String[] args) {
        Aeron.Context ctx = new Aeron.Context(); // Connect to standalone Media Driver

        try (Aeron aeron = Aeron.connect(ctx);
             Subscription subscription = aeron.addSubscription(CHANNEL, STREAM_ID)) {

            System.out.println("Listening for Binance market data...");
            IdleStrategy idleStrategy = new NoOpIdleStrategy();

            FragmentAssembler handler = new FragmentAssembler(
                    (buffer, offset, length, header) -> {

                        System.out.println("received msg");
                        MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
                        headerDecoder.wrap(buffer, offset);

                        switch (headerDecoder.templateId()) {
                            case OrderbookMsgDecoder.TEMPLATE_ID:
                            {
                                OrderbookMsgDecoder decoder = new OrderbookMsgDecoder();
                                decoder.wrap(buffer, offset + MessageHeaderDecoder.ENCODED_LENGTH, headerDecoder.blockLength(), headerDecoder.version());// Use the extracted schema version

                                // Update Bids and Asks
                                processOrderBookMessage(decoder);
                                break;
                            }
                            case PrintOBDecoder.TEMPLATE_ID:
                            {
                                printOB();
                                break;
                            }
                            default:
                                System.err.println("Unrecognized msg with schema Id :: " + headerDecoder.schemaId());
                        }
                    }

            );

            while (true) {
                int fragments = subscription.poll(handler, 100);
                idleStrategy.idle(fragments);
            }
        }
    }
}