package pl.konradmaksymilian.nssvbot.protocol.utils;

import pl.konradmaksymilian.nssvbot.protocol.Position;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class PositionConverter {

    private PositionConverter() {}

    public static Position read(DataInputStream in) throws IOException {
        long position = in.readLong();
        int x = (int) ((position & 0b1111111111111111111111111100000000000000000000000000000000000000L) >> 38); //first 26 bits
        int y = (int) (((position << 26) & 0b1111111111110000000000000000000000000000000000000000000000000000L) >> 52); //middle 12 bits
        int z = (int) (((position << 38) & 0b1111111111111111111111111100000000000000000000000000000000000000L) >> 38); // last 26 bits

        return new Position(x, y, z);
    }

    public static void write(Position position, DataOutputStream out) throws IOException {
        long data = position.getX();
        data = data << 12;
        data = data | position.getY();
        data = data << 26;
        data = data | position.getZ();
        out.writeLong(data);
    }
}
