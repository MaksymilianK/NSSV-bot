package pl.konradmaksymilian.nssvbot.protocol;


public class UnexpectedPacketException extends RuntimeException {
    
    public UnexpectedPacketException(String message) {
        super(message);
    }
}
