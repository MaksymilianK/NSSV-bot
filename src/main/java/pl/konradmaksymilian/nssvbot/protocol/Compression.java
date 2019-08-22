package pl.konradmaksymilian.nssvbot.protocol;


public final class Compression {

    private final boolean compressed;
    private final int treshold;
    
    public Compression(boolean compressed, int treshold) {
        this.compressed = compressed;
        this.treshold = treshold;
    }
    
    public boolean isCompressed() {
        return compressed;
    }

    
    public int getTreshold() {
        return treshold;
    }
}
