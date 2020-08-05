package pl.konradmaksymilian.nssvbot.session;

import pl.konradmaksymilian.nssvbot.config.DealerConfig;
import pl.konradmaksymilian.nssvbot.config.DealerConfigReader;
import pl.konradmaksymilian.nssvbot.connection.ConnectionManager;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketReader;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketWriter;
import pl.konradmaksymilian.nssvbot.utils.Timer;
import pl.konradmaksymilian.nssvbot.utils.ZlibCompressor;

public class SessionFactory {

    private DealerConfigReader dealerConfigReader = new DealerConfigReader();

    public Session createBasicAfk() {
        var zlib = new ZlibCompressor();
        return new BasicAfkSession(new ConnectionManager(500, new PacketReader(zlib), new PacketWriter(zlib)), new Timer());
    }

    public DealerSession createDealer() {
        var zlib = new ZlibCompressor();
        var config = dealerConfigReader.read();
        if (config.isEmpty()) {
            throw new SessionException("Cannot create dealer session - configuration has not been found");
        }

        return new DealerSession(new ConnectionManager(50, new PacketReader(zlib), new PacketWriter(zlib)), new Timer(),
                config.get());
    }

    public BuilderSession createBuilder() {
        var zlib = new ZlibCompressor();
        return new BuilderSession(new ConnectionManager(50, new PacketReader(zlib), new PacketWriter(zlib)), new Timer());
    }
}
