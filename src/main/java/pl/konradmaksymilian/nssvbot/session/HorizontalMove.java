package pl.konradmaksymilian.nssvbot.session;

public class HorizontalMove {

    private final double destinationX;
    private final double destinationZ;
    private final double xMove;
    private final double zMove;

    public HorizontalMove(double currentX, double currentZ, double destinationX, double destinationZ) {
        this.destinationX = destinationX;
        this.destinationZ = destinationZ;
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
}
