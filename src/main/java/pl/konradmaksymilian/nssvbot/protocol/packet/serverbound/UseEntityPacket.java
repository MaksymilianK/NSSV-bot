package pl.konradmaksymilian.nssvbot.protocol.packet.serverbound;

import pl.konradmaksymilian.nssvbot.ObjectConstructionException;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

import java.util.Optional;

public final class UseEntityPacket implements Packet {

    private final int type;
    private final Float targetX;
    private final Float targetY;
    private final Float targetZ;
    private final Integer hand;

    private UseEntityPacket(int type, Float targetX, Float targetY, Float targetZ, Integer hand) {
        this.type = type;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.hand = hand;
    }

    public int getType() {
        return type;
    }

    public Optional<Float> getTargetX() {
        return Optional.ofNullable(targetX);
    }

    public Optional<Float> getTargetY() {
        return Optional.ofNullable(targetY);
    }

    public Optional<Float> getTargetZ() {
        return Optional.ofNullable(targetZ);
    }

    public Optional<Integer> getHand() {
        return Optional.ofNullable(hand);
    }

    @Override
    public PacketName getName() {
        return PacketName.USE_ENTITY;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Integer type;
        private Float targetX;
        private Float targetY;
        private Float targetZ;
        private Integer hand;

        Builder() {}

        public Builder type(Integer type) {
            this.type = type;
            return this;
        }

        public Builder targetX(Float targetX) {
            this.targetX = targetX;
            return this;
        }

        public Builder targetY(Float targetY) {
            this.targetY = targetY;
            return this;
        }

        public Builder targetZ(Float targetZ) {
            this.targetZ = targetZ;
            return this;
        }

        public Builder hand(Integer hand) {
            this.hand = hand;
            return this;
        }

        public UseEntityPacket build() {
            if (type == null) {
                throw new ObjectConstructionException("Cannot create Use Entity Packet - 'type' field is null");
            }
            return new UseEntityPacket(type, targetX, targetY, targetZ, hand);
        }
    }
}
