package pl.konradmaksymilian.nssvbot.protocol.packet;

public class UnrecognizedPacketException extends RuntimeException {
    
    public UnrecognizedPacketException(String message) {
        super(message);
    }
}
