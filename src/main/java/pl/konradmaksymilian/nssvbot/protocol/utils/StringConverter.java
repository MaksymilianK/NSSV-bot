package pl.konradmaksymilian.nssvbot.protocol.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import pl.konradmaksymilian.nssvbot.protocol.ReadField;

public final class StringConverter {
    
    private StringConverter() {}
    
    public static ReadField<String> readString(InputStream in) throws IOException {
        var length = VarIntLongConverter.readVarInt(in);
        byte[] bytes = new byte[length.getValue()];
        for (int i = 0; i < length.getValue(); i++) {
            bytes[i] = (byte) in.read();
        }
        return new ReadField<>(new String(bytes, StandardCharsets.UTF_8), length.getLength() + length.getValue());
    }
    
    public static void writeString(String text, OutputStream out) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        VarIntLongConverter.writeVarInt(bytes.length, out);
        out.write(bytes);
    }
}
