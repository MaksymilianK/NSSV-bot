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
        long x = position.getX();
        long y = position.getY();
        long z = position.getZ();

        if (x < 0) {
            x |= 0b0000000000000000000000000000000000000010000000000000000000000000;
            x &= 0b0000000000000000000000000000000000000011111111111111111111111111;
        }

        if (y < 0) {
            y |= 0b0000000000000000000000000000000000000000000000000000100000000000;
            y &= 0b0000000000000000000000000000000000000000000000000000111111111111;
        }

        if (z < 0) {
            z |= 0b0000000000000000000000000000000000000010000000000000000000000000;
            z &= 0b0000000000000000000000000000000000000011111111111111111111111111;
        }

        long data = x;
        data <<= 12;
        data |= y;
        data <<= 26;
        data |= z;
        out.writeLong(data);
    }
}
