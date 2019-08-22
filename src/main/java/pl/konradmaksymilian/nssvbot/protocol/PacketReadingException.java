package pl.konradmaksymilian.nssvbot.protocol;


public class PacketReadingException extends RuntimeException {

    public PacketReadingException(String message) {
        super(message);
    }
    
    public PacketReadingException(String message, Exception e) {
        super(message, e);
    }
}
