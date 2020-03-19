package pl.konradmaksymilian.nssvbot.protocol.packet.clientbound;

import pl.konradmaksymilian.nssvbot.ObjectConstructionException;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;
import pl.konradmaksymilian.nssvbot.protocol.packet.PlayerPositionAndLook;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class PlayerPositionAndLookClientboundPacket extends PlayerPositionAndLook {

    private final int teleportId;

    private PlayerPositionAndLookClientboundPacket(double x, double feetY, double z, float yaw, float pitch,
                                                   int teleportId) {
        super(x, feetY, z, yaw, pitch);
        this.teleportId = teleportId;
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

        public Builder teleportId(int teleportId) {
            this.teleportId = teleportId;
            return this;
        }

        public PlayerPositionAndLookClientboundPacket build() {
            validate();
            if (teleportId == null) {
                throw new ObjectConstructionException("Cannot build the object - a field is null");
            }

            return new PlayerPositionAndLookClientboundPacket(x, feetY, z, yaw, pitch, teleportId);
        }
    }
}
