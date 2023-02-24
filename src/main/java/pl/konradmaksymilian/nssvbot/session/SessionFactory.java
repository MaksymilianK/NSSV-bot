package pl.konradmaksymilian.nssvbot.session;

import pl.konradmaksymilian.nssvbot.config.DealerConfigReader;
import pl.konradmaksymilian.nssvbot.connection.ConnectionManager;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketReader;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketWriter;
import pl.konradmaksymilian.nssvbot.session.v2.CactusBuilderSessionV2;
import pl.konradmaksymilian.nssvbot.session.v2.DiggerSessionV2;
import pl.konradmaksymilian.nssvbot.session.v2.SandBuilderSessionV2;
import pl.konradmaksymilian.nssvbot.session.v2.StringBuilderSessionV2;
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

    public StringBuilderSessionV2 createSlab() {
        var zlib = new ZlibCompressor();
        return new StringBuilderSessionV2(new ConnectionManager(50, new PacketReader(zlib), new PacketWriter(zlib)), new Timer());
    }

    public DiggerSessionV2 createDigger() {
        var zlib = new ZlibCompressor();
        return new DiggerSessionV2(new ConnectionManager(50, new PacketReader(zlib), new PacketWriter(zlib)), new Timer());
    }

    public GateBuilderSession createGate() {
        var zlib = new ZlibCompressor();
        return new GateBuilderSession(new ConnectionManager(50, new PacketReader(zlib), new PacketWriter(zlib)), new Timer());
    }

    public SandBuilderSessionV2 createSand() {
        var zlib = new ZlibCompressor();
        return new SandBuilderSessionV2(new ConnectionManager(50, new PacketReader(zlib), new PacketWriter(zlib)), new Timer());
    }

    public CactusBuilderSessionV2 createCactus() {
        var zlib = new ZlibCompressor();
        return new CactusBuilderSessionV2(new ConnectionManager(50, new PacketReader(zlib), new PacketWriter(zlib)), new Timer());
    }

    public FenceBuilderSession createFence() {
        var zlib = new ZlibCompressor();
        return new FenceBuilderSession(new ConnectionManager(50, new PacketReader(zlib), new PacketWriter(zlib)), new Timer());
    }
}
