package pl.konradmaksymilian.nssvbot.protocol.packet;

public enum PacketName {
    HANDSHAKE (0x00),
    LOGIN_START (0x00),
    DISCONNECT_LOGIN (0x00),
    LOGIN_SUCCESS (0x02),
    SET_COMPRESSION (0x03);
        
    private final int id;
    
    PacketName(int id) {
        this.id = id;
    }
    
    public int getId() {
        return id;
    }
}
