package org.sokei.apps.commands;

import io.aeron.Aeron;
import io.aeron.ExclusivePublication;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PrintCommand {
    private static final String AERON_CHANNEL = "aeron:udp?endpoint=localhost:40123";
    private static final int STREAM_ID = 1002; // Separate stream ID for commands
    private static final int PRINT_DEPTH = 10; // Modify depth as needed

    public static void main(String[] args) {
        Aeron.Context ctx = new Aeron.Context();//standalone media driver
        Aeron aeron = Aeron.connect(ctx);

        // 2️⃣ Start ExclusivePublication
        ExclusivePublication publication = aeron.addExclusivePublication(AERON_CHANNEL, STREAM_ID);
        System.out.println("✅ Command Publisher Started. Sending 'print_btc_" + PRINT_DEPTH + "' every 30 sec...");

        // 3️⃣ Schedule periodic messages
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            sendPrintCommand(publication, PRINT_DEPTH);
        }, 0, 30, TimeUnit.SECONDS);
    }

    private static void sendPrintCommand(ExclusivePublication publication, int depth) {
        String command = "print_btc_" + depth;
        byte[] data = command.getBytes();
        UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(data.length));
        buffer.putBytes(0, data);

        while (publication.offer(buffer, 0, data.length) < 0) {
            System.out.println("⚠️ Backpressure - Retrying...");
            try {
                Thread.sleep(1);
            } catch (InterruptedException ignored) {}
        }

        System.out.println("📡 Sent: " + command);
    }
}
