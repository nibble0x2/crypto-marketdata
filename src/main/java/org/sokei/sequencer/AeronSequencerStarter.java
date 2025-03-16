package org.sokei.sequencer;

import io.aeron.driver.MediaDriver;
import org.agrona.concurrent.ShutdownSignalBarrier;

import static org.agrona.SystemUtil.loadPropertiesFiles;

public class AeronSequencerStarter {
    public static void main(String[] args) {
        loadPropertiesFiles(args);

        final ShutdownSignalBarrier barrier = new ShutdownSignalBarrier();
        final MediaDriver.Context ctx = new MediaDriver.Context()
                .clientLivenessTimeoutNs(300_000_000_000L)
                .publicationUnblockTimeoutNs(400_000_000_000L)
                .terminationHook(barrier::signal);

        try (MediaDriver ignore = MediaDriver.launch(ctx))
        {
            System.out.println("Started Aeron Sequencer");
            barrier.await();
            System.out.println("Shutdown Driver...");
        }
    }
}
