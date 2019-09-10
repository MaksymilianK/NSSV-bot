package pl.konradmaksymilian.nssvbot.protocol.packet.serverbound;

import pl.konradmaksymilian.nssvbot.ObjectConstructionException;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class ClientSettingsPacket implements Packet {

    private final String locale;
    private final int viewDistance;
    private final int chatMode;
    private final boolean chatColours;
    private final int displayedSkinParts;
    private final int mainHand;
    
    private ClientSettingsPacket(String locale, int viewDistance, int chatMode, boolean chatColours,
            int displayedSkinParts, int mainHand) {
        this.locale = locale;
        this.viewDistance = viewDistance;
        this.chatMode = chatMode;
        this.chatColours = chatColours;
        this.displayedSkinParts = displayedSkinParts;
        this.mainHand = mainHand;
    }
    
    public String getLocale() {
        return locale;
    }
    
    public int getViewDistance() {
        return viewDistance;
    }
    
    public int getChatMode() {
        return chatMode;
    }
    
    public boolean getChatColours() {
        return chatColours;
    }
    
    public int getDisplayedSkinParts() {
        return displayedSkinParts;
    }
    
    public int getMainHand() {
        return mainHand;
    }

    @Override
    public PacketName getName() {
        return PacketName.CLIENT_SETTINGS;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static final class Builder {
        
        private String locale;
        private Integer viewDistance;
        private Integer chatMode;
        private Boolean chatColours;
        private Integer displayedSkinParts;
        private Integer mainHand;
        
        Builder() {}

        public Builder locale(String locale) {
            this.locale = locale;
            return this;
        }
        
        public Builder viewDistance(int viewDistance) {
            this.viewDistance = viewDistance;
            return this;
        }
        
        public Builder chatMode(int chatMode) {
            this.chatMode = chatMode;
            return this;
        }
        
        public Builder chatColours(boolean chatColours) {
            this.chatColours = chatColours;
            return this;
        }
        
        public Builder displayedSkinParts(int displayedSkinParts) {
            this.displayedSkinParts = displayedSkinParts;
            return this;
        }
        
        public Builder mainHand(int mainHand) {
            this.mainHand = mainHand;
            return this;
        }
        
        public ClientSettingsPacket build() {
            if (locale == null || viewDistance == null || chatMode == null || chatColours == null 
                    || displayedSkinParts == null || mainHand == null) {
                throw new ObjectConstructionException("ClientSettingsPacket lacks fields");
            }
            return new ClientSettingsPacket(locale, viewDistance, chatMode, chatColours, displayedSkinParts, mainHand);
        }
    }
}
