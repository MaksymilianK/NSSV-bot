package pl.konradmaksymilian.nssvbot.protocol.packet;

import pl.konradmaksymilian.nssvbot.ObjectConstructionException;

public abstract class PlayerPositionAndLook implements Packet {

    private final double x;
    private final double feetY;
    private final double z;
    private final float yaw;
    private final float pitch;

    protected PlayerPositionAndLook(double x, double feetY, double z, float yaw, float pitch) {
        this.x = x;
        this.feetY = feetY;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
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

    public static abstract class Builder {

        protected Double x;
        protected Double feetY;
        protected Double z;
        protected Float yaw;
        protected Float pitch;

        protected Builder() {}

        protected void validate() {
            if (x == null || feetY == null || z == null || yaw == null || pitch == null) {
                throw new ObjectConstructionException("Cannot build the object: a field is null");
            }
        }
    }
}
