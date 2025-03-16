**How to run it locally?**

Make sure - using Linux machine

Build the project

generate messages using task `generation/generateSbeCode`

**Run the seq class** 
`AeronSequencerStarter`

**Run the order book subscriber class**
`BinanceMarketDataSubscriber`

**Run the GW app to bring in market data class**
`BinanceDepthMarketDataGW`


all the files (log buffers, confis) are created under dir `/dev/shm/aeron-<machine_name>/`


All the best!