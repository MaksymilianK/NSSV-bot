package pl.konradmaksymilian.nssvbot.protocol.packet.clientbound;

import pl.konradmaksymilian.nssvbot.ObjectConstructionException;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;
import pl.konradmaksymilian.nssvbot.protocol.packet.PlayerPositionAndLook;

public final class PlayerPositionAndLookClientboundPacket extends PlayerPositionAndLook {

    private final int teleportId;
    private final byte flags;

    private PlayerPositionAndLookClientboundPacket(double x, double feetY, double z, float yaw, float pitch,
                                                   byte flags, int teleportId) {
        super(x, feetY, z, yaw, pitch);
        this.flags = flags;
        this.teleportId = teleportId;
    }

    public byte getFlags() {
        return flags;
    }

    public int getTeleportId() {
        return teleportId;
    }
    
    @Override
    public PacketName getName() {
        return PacketName.PLAYER_POSITION_AND_LOOK_CLIENTBOUND;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends PlayerPositionAndLook.Builder {

        private Integer teleportId;
        private Byte flags;

        private Builder() {}

        public Builder x(double x) {
            this.x = x;
            return this;
        }

        public Builder feetY(double feetY) {
            this.feetY = feetY;
            return this;
        }

        public Builder z(double z) {
            this.z = z;
            return this;
        }

        public Builder yaw(float yaw) {
            this.yaw = yaw;
            return this;
        }

        public Builder pitch(float pitch) {
            this.pitch = pitch;
            return this;
        }

        public Builder flags(byte flags) {
            this.flags = flags;
            return this;
        }

        public Builder teleportId(int teleportId) {
            this.teleportId = teleportId;
            return this;
        }

        public PlayerPositionAndLookClientboundPacket build() {
            validate();
            if (teleportId == null || flags == null) {
                throw new ObjectConstructionException("Cannot build the object - a field is null");
            }

            return new PlayerPositionAndLookClientboundPacket(x, feetY, z, yaw, pitch, flags, teleportId);
        }
    }
}
