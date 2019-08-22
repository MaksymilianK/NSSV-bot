package pl.konradmaksymilian.nssvbot.protocol;


public class UnrecognizedPacketException extends RuntimeException {
    
    public UnrecognizedPacketException(String message) {
        super(message);
    }
}
