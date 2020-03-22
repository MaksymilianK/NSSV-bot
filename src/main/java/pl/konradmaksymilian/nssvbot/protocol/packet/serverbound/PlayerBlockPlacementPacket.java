package pl.konradmaksymilian.nssvbot.protocol.packet.serverbound;

import pl.konradmaksymilian.nssvbot.ObjectConstructionException;
import pl.konradmaksymilian.nssvbot.protocol.Position;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public class PlayerBlockPlacementPacket implements Packet {

    private final Position location;
    private final int face;
    private final int hand;
    private final float cursorX;
    private final float cursorY;
    private final float cursorZ;

    private PlayerBlockPlacementPacket(Position location, int face, int hand, float cursorX, float cursorY, float cursorZ) {
        this.location = location;
        this.face = face;
        this.hand = hand;
        this.cursorX = cursorX;
        this.cursorY = cursorY;
        this.cursorZ = cursorZ;
    }

    public Position getLocation() {
        return location;
    }

    public int getFace() {
        return face;
    }

    public int getHand() {
        return hand;
    }

    public float getCursorX() {
        return cursorX;
    }

    public float getCursorY() {
        return cursorY;
    }

    public float getCursorZ() {
        return cursorZ;
    }

    @Override
    public PacketName getName() {
        return PacketName.PLAYER_BLOCK_PLACEMENT;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Integer x;
        private Integer y;
        private Integer z;
        private Integer face;
        private Integer hand;
        private Float cursorX;
        private Float cursorY;
        private Float cursorZ;

        Builder() {}

        public Builder x(int x) {
            this.x = x;
            return this;
        }

        public Builder y(int y) {
            this.y = y;
            return this;
        }

        public Builder z(int z) {
            this.z = z;
            return this;
        }

        public Builder face(int face) {
            this.face = face;
            return this;
        }

        public Builder hand(int hand) {
            this.hand = hand;
            return this;
        }

        public Builder cursorX(float cursorX) {
            this.cursorX = cursorX;
            return this;
        }

        public Builder cursorY(float cursorY) {
            this.cursorY = cursorY;
            return this;
        }

        public Builder cursorZ(float cursorZ) {
            this.cursorZ = cursorZ;
            return this;
        }

        public PlayerBlockPlacementPacket build() {
            if (x == null || y == null || z == null || face == null || hand == null || cursorX == null
                    || cursorY == null || cursorZ == null) {
                throw new ObjectConstructionException("Cannot build PlayerBlockPlacementPacket: a field is null");
            }
            return new PlayerBlockPlacementPacket(new Position(x, y, z), face, hand, cursorX, cursorY, cursorZ);
        }
    }
}
