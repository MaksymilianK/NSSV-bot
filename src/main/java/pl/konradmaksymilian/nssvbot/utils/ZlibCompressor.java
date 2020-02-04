package pl.konradmaksymilian.nssvbot.utils;

import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class ZlibCompressor {
    
    private final Deflater deflater = new Deflater();
    private final Inflater inflater = new Inflater();

    public byte[] compress(byte[] data) throws IOException {
        byte[] buffer = new byte[1024];
        deflater.setInput(data);
        deflater.finish();
        int length = deflater.deflate(buffer);
        deflater.reset();
        
        byte[] out = new byte[length];
        for (int i = 0; i < length; i++) {
            out[i] = buffer[i];
        }
        return out;
    }
    
    public byte[] decompress(byte[] data, int length) throws DataFormatException {
        byte[] buffer = new byte[length];
        inflater.setInput(data);
        inflater.inflate(buffer);
        inflater.reset();
        return buffer;
    }
}
