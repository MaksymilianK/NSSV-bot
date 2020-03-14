package pl.konradmaksymilian.nssvbot.session;

import pl.konradmaksymilian.nssvbot.connection.ConnectionManager;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketReader;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketWriter;
import pl.konradmaksymilian.nssvbot.utils.ZlibCompressor;

public class SessionFactory {

    public Session create() {
        var zlib = new ZlibCompressor();
        return new Session(new ConnectionManager(new PacketReader(zlib), new PacketWriter(zlib)), new Timer());
    }
}
