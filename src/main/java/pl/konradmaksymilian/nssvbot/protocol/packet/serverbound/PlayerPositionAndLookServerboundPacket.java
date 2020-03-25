package pl.konradmaksymilian.nssvbot.protocol.packet.serverbound;

import pl.konradmaksymilian.nssvbot.ObjectConstructionException;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class PlayerPositionAndLookServerboundPacket implements Packet {

    private final double x;
    private final double feetY;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final boolean onGround;

    private PlayerPositionAndLookServerboundPacket(double x, double feetY, double z, float yaw, float pitch, boolean onGround) {
        this.x = x;
        this.feetY = feetY;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
    }

    public double getX() {
        return x;
    }

    public double getFeetY() {
        return feetY;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public boolean isOnGround() {
        return onGround;
    }

    @Override
    public PacketName getName() {
        return PacketName.PLAYER_POSITION_AND_LOOK_SERVERBOUND;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Double x;
        private Double feetY;
        private Double z;
        private Float yaw;
        private Float pitch;
        private Boolean onGround;

        Builder() {}

        public Builder x(Double x) {
            this.x = x;
            return this;
        }

        public Builder feetY(Double feetY) {
            this.feetY = feetY;
            return this;
        }

        public Builder z(Double z) {
            this.z = z;
            return this;
        }

        public Builder yaw(Float yaw) {
            this.yaw = yaw;
            return this;
        }

        public Builder pitch(Float pitch) {
            this.pitch = pitch;
            return this;
        }

        public Builder onGround(Boolean onGround) {
            this.onGround = onGround;
            return this;
        }

        public PlayerPositionAndLookServerboundPacket build() {
            if (x == null || feetY == null || z == null || yaw == null || pitch == null || onGround == null) {
                throw new ObjectConstructionException("Cannot build PlayerPositionAndLookServerbound packet: a field is null");
            }
            return new PlayerPositionAndLookServerboundPacket(x, feetY, z, yaw, pitch, onGround);
        }
    }
}
