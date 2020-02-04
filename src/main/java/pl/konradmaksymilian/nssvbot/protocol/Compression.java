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
    
    @Override
    public boolean equals(Object obj) {
        if (obj != null) {
            if (obj instanceof Compression) {
                var otherCompression = (Compression) obj;
                if (compressed && otherCompression.isCompressed()) {
                    return treshold == otherCompression.getTreshold();
                } else if (!compressed && !otherCompression.isCompressed()) {
                    return true;
                }
            }
        }
        return false;
           
    }
}
