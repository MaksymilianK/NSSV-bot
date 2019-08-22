package pl.konradmaksymilian.nssvbot.protocol;


public final class ReadField<T> {
    
    private final T value;
    private final int length;
    
    public ReadField(T value, int length) {
        this.value = value;
        this.length = length;
    }
    
    public T getValue() {
        return value;
    }
    
    public int getLength() {
        return length;
    }
}
