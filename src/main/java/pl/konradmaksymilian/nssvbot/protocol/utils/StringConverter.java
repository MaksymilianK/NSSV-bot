package pl.konradmaksymilian.nssvbot.protocol.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class StringConverter {
    
    private StringConverter() {}
    
    public static String readString(InputStream in) throws IOException {
        int length = VarIntLongConverter.readVarInt(in).getValue();
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) in.read();
        }
        return new String(bytes);
    }
    
    public static void writeString(String text, OutputStream out) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        VarIntLongConverter.writeVarInt(bytes.length, out);
        out.write(bytes);
    }
}
