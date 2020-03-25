package pl.konradmaksymilian.nssvbot.protocol.packet;

public enum PacketName {
    HANDSHAKE (0x00),
    LOGIN_START (0x00),
    DISCONNECT_LOGIN (0x00),
    DISCONNECT_PLAY (0x1A),
    LOGIN_SUCCESS (0x02),
    SET_COMPRESSION (0x03),
    PLUGIN_MESSAGE_CLIENTBOUND (0x18),
    PLUGIN_MESSAGE_SERVERBOUND (0x09),
    JOIN_GAME (0x23),
    CHAT_MESSAGE_CLIENTBOUND (0x0F),
    CHAT_MESSAGE_SERVERBOUND (0x02),
    CHANGE_GAME_STATE (0x1E), 
    KEEP_ALIVE_CLIENTBOUND (0x1F),
    KEEP_ALIVE_SERVERBOUND (0x0B),
    PLAYER_POSITION_AND_LOOK_CLIENTBOUND (0x2F),
    TELEPORT_CONFIRM (0x00),
    CLIENT_SETTINGS (0x04), 
    RESPAWN (0x35),
    PLAYER_POSITION (0x0D),
    PLAYER_LOOK (0x0F),
    PLAYER_DIGGING (0x14),
    PLAYER_BLOCK_PLACEMENT (0x1F),
    PLAYER_POSITION_AND_LOOK_SERVERBOUND (0x0E),
    ENTITY_ACTION (0x15);

    private final int id;
    
    PacketName(int id) {
        this.id = id;
    }
    
    public int getId() {
        return id;
    }
}
