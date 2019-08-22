package pl.konradmaksymilian.nssvbot.protocol.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import pl.konradmaksymilian.nssvbot.protocol.ReadField;

/**
 * Using algorithms from {@link https://wiki.vg/Protocol#VarInt_and_VarLong}
 */
public final class VarIntLongConverter {

    private VarIntLongConverter() {}
    
    public static ReadField<Integer> readVarInt(InputStream in) throws IOException {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = (byte) in.read();

            int value = (read & 0b01111111);
            result |= (value << (7 * numRead));

            numRead++;
            if (numRead > 5) {
                throw new RuntimeException("VarInt is too big");
            }
        } while ((read & 0b10000000) != 0);

        return new ReadField<>(result, numRead);
    }
    
    public static int writeVarInt(int value, OutputStream out) throws IOException {
        int length = 0;
        do {
            byte temp = (byte)(value & 0b01111111);
            // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
            value >>>= 7;
            if (value != 0) {
                temp |= 0b10000000;
            }
            out.write(temp);
            length++;
        } while (value != 0);
        
        return length;
    }
    
    public static ReadField<Long> readVarLong(InputStream in) throws IOException {
        int numRead = 0;
        long result = 0;
        byte read;
        do {
            read = (byte) in.read();
            int value = (read & 0b01111111);
            result |= (value << (7 * numRead));

            numRead++;
            if (numRead > 10) {
                throw new RuntimeException("VarLong is too big");
            }
        } while ((read & 0b10000000) != 0);

        return new ReadField<>(result, numRead);
    }
    
    public static int writeVarLong(long value, OutputStream out) throws IOException {
        int length = 0;
        do {
            byte temp = (byte)(value & 0b01111111);
            // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
            value >>>= 7;
            if (value != 0) {
                temp |= 0b10000000;
            }
            out.write(temp);
            length++;
        } while (value != 0);
        
        return length;
    }
}
