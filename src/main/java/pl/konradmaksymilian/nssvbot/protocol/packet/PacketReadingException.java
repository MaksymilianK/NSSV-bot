package pl.konradmaksymilian.nssvbot.protocol.packet;


public class PacketReadingException extends RuntimeException {

    public PacketReadingException(String message) {
        super(message);
    }
    
    public PacketReadingException(String message, Exception e) {
        super(message, e);
    }
}
