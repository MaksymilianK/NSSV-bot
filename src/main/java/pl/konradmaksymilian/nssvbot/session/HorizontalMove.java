package pl.konradmaksymilian.nssvbot.session;

import pl.konradmaksymilian.nssvbot.ObjectConstructionException;

import java.util.Optional;

public class HorizontalMove {

    private final double destinationX;
    private final double destinationZ;
    private final double xMove;
    private final double zMove;
    private final Float yaw;
    private final Float pitch;

    private HorizontalMove(double currentX, double currentZ, double destinationX, double destinationZ, Float yaw,
                Float pitch) {
        this.destinationX = destinationX;
        this.destinationZ = destinationZ;
        this.yaw = yaw;
        this.pitch = pitch;
        double distanceX = destinationX - currentX;
        double distanceZ = destinationZ - currentZ;
        double distance = Math.sqrt((distanceX * distanceX) + (distanceZ * distanceZ));
        xMove = (distanceX / distance) * MovableSession.MAX_MOVE;
        zMove = (distanceZ / distance) * MovableSession.MAX_MOVE;
    }

    public double getDestinationX() {
        return destinationX;
    }

    public double getDestinationZ() {
        return destinationZ;
    }

    public double getXMove() {
        return xMove;
    }

    public double getZMove() {
        return zMove;
    }

    public Optional<Float> getYaw() {
        return Optional.of(yaw);
    }

    public Optional<Float> getPitch() {
        return Optional.of(pitch);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Double currentX;
        private Double currentZ;
        private Double destinationX;
        private Double destinationZ;
        private Float yaw;
        private Float pitch;

        Builder() {}

        public Builder currentX(double currentX) {
            this.currentX = currentX;
            return this;
        }

        public Builder currentZ(double currentZ) {
            this.currentZ = currentZ;
            return this;
        }

        public Builder destinationX(double destinationX) {
            this.destinationX = destinationX;
            return this;
        }

        public Builder destinationZ(double destinationZ) {
            this.destinationZ = destinationZ;
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

        public HorizontalMove build() {
            if (currentX == null || currentZ == null || destinationX == null || destinationZ == null) {
                throw new ObjectConstructionException("Cannot build HorizontalMove: a field is null");
            } else if ((yaw == null && pitch != null) || (yaw != null && pitch == null)) {
                throw new ObjectConstructionException("Cannot build HorizontalMove: both yaw and pitch values must be" +
                        "present or absent, but not only one of them");
            }
            return new HorizontalMove(currentX, currentZ, destinationX, destinationZ, yaw, pitch);
        }
    }
}
