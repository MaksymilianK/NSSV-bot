package pl.konradmaksymilian.nssvbot.session;

import pl.konradmaksymilian.nssvbot.connection.ConnectionManager;
import pl.konradmaksymilian.nssvbot.protocol.Position;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.*;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.*;
import pl.konradmaksymilian.nssvbot.utils.ChatFormatter;
import pl.konradmaksymilian.nssvbot.utils.Timer;

public class HoleDiggerSession extends MovableSession {

    private final int FIRST_X = 28400;
    private final int FIRST_Z = -5567;
    private final int LAST_X = 28558;
    private final int LAST_Z = -5409;

    private DiggerStatus diggerStatus = DiggerStatus.DISABLED;
    private int currentX;
    private int currentZ;

    private int tickCounter = 0;

    public HoleDiggerSession(ConnectionManager connection, Timer timer) {
        super(connection, timer);
    }

    @Override
    protected void onPacket(Packet packet) {
        super.onPacket(packet);
        if (!status.equals(Status.GAME)) {
            return;
        }

        if (packet.getName().equals(PacketName.BLOCK_CHANGE)) {
            onBlockChange((BlockChangePacket) packet);
        }
    }

    private void onBlockChange(BlockChangePacket packet) {
        if (diggerStatus.equals(DiggerStatus.DISABLED) || packet.getStateID() != 0) {
            return;
        }

        if (diggerStatus.equals(DiggerStatus.DIGGING) &&
                packet.getPosition().getY() == (int) feetY - 1 && packet.getPosition().getX() == currentX && packet.getPosition().getZ() == currentZ) {
//            cancelledX.add(packet.getPosition().getX());
//            cancelledZ.add(packet.getPosition().getZ());
            nextHole();
            moveToHole();
//            counter++;
//            if (counter == 2) {
//                nextSlab();
//                moveToSlab();
//                counter = 0;
//            }
        }
    }

    @Override
    protected void onChatMessage(ChatMessageClientboundPacket packet) {
        super.onChatMessage(packet);
        String message = ChatFormatter.getPureText(packet.getMessage().getComponents());

        if (!message.endsWith("-")) {
            return;
        }

        if (message.endsWith("--start--")) {
            int[] coords = extractCoords(message);
            if (coords.length > 0) {
                startDiggingHoles(coords[0], coords[1]);
            } else {
                startDiggingHoles();
            }
        } else if (message.endsWith("--stop--")) {
            changeDiggerStatus(DiggerStatus.DISABLED);
        } else if (message.endsWith("--throw--")) {
            throwItem();
        } else if (message.endsWith("--tpa--")) {
            connection.sendPacket(new ChatMessageServerboundPacket("/tpa geniuszmistrz"));
        } else if (message.endsWith("--status--")) {
            System.out.println(diggerStatus);
        } else if (message.endsWith("--pos--")) {
            if (move != null) {
                System.out.println(move.getDestinationX() + " " + move.getDestinationZ());
            }
            System.out.println(x + " " + z);
        }
    }

    @Override
    protected void onEveryCheck() {
        super.onEveryCheck();
        if (!status.equals(Status.GAME) || diggerStatus.equals(DiggerStatus.DISABLED)) {
            return;
        }

        if (diggerStatus.equals(DiggerStatus.MOVING) && !isMoving()) {
            changeDiggerStatus(DiggerStatus.DIGGING);
        } else if (diggerStatus.equals(DiggerStatus.DIGGING)) {
            tickCounter++;
            if (tickCounter == 2) {
                finishDigHole();
                tickCounter = 0;
            } else {
                continueDigHole();
            }
        }
    }

    private void onChangeDiggerStatus() {
        if (diggerStatus.equals(DiggerStatus.DIGGING)) {
            startDigHole();
        } else if (diggerStatus.equals(DiggerStatus.DISABLED)) {
            stop();
        }
    }

    private void changeDiggerStatus(DiggerStatus diggerStatus) {
        this.diggerStatus = diggerStatus;
        onChangeDiggerStatus();
    }

    private void startDiggingHoles() {
        startDiggingHoles(FIRST_X, FIRST_Z);
    }

    private void startDiggingHoles(int firstX, int firstZ) {
        currentX = firstX;
        currentZ = firstZ;
        moveToHole();
    }

    private void moveToHole() {
        changeDiggerStatus(DiggerStatus.MOVING);
        double newX, newZ;
        float newYaw, newPitch;

        if (isHoleOdd()) {
            if (currentZ == LAST_Z) {
                newX = currentX - 0.5;
                newZ = currentZ - 0.5;
                newYaw = -143.5f;
                newPitch = 49.5f;
            } else {
                newX = currentX - 0.5;
                newZ = currentZ + 1.5;
                newYaw = -135.5f;
                newPitch = 49.5f;
            }
        } else {
            if (currentX == FIRST_X) {
                if (currentZ == FIRST_Z) {
                    newX = currentX + 1.5;
                    newZ = currentZ + 1.5;
                    newYaw = 135.5f;
                    newPitch = 49.5f;
                } else {
                    newX = currentX + 1.5;
                    newZ = currentZ - 0.5;
                    newYaw = 45.5f;
                    newPitch = 49.5f;
                }
            } else if (currentZ == FIRST_Z) {
                newX = currentX - 0.5;
                newZ = currentZ + 1.5;
                newYaw = -135.5f;
                newPitch = 49.5f;
            } else {
                newX = currentX - 0.5;
                newZ = currentZ - 0.5;
                newYaw = -45.5f;
                newPitch = 49.5f;
            }
        }
        setNewDestination(newX, newZ, newYaw, newPitch);
        System.out.println("current " + currentX + " " + currentZ + " " + newYaw);
    }

    private void startDigHole() {
        connection.sendPacket(new PlayerDiggingPacket(0, new Position(currentX, (int) feetY - 1, currentZ), 1));
        connection.sendPacket(new AnimationPacket(0));
    }

    private void continueDigHole() {
        connection.sendPacket(new AnimationPacket(0));
    }

    private void finishDigHole() {
        connection.sendPacket(new PlayerDiggingPacket(2, new Position(currentX, (int) feetY - 1, currentZ), 1));
        connection.sendPacket(new AnimationPacket(0));
    }

    private void nextHole() {
        if ((currentZ == LAST_Z && !isHoleOdd()) || (currentZ == FIRST_Z && isHoleOdd())) {
            if (currentX == LAST_X - 2) {
                currentX += 2;
            } else {
                currentX += 4;
            }
        } else if (!isHoleOdd()) {
            if (currentZ == FIRST_Z) {
                currentZ += 2;
            } else {
                currentZ += 4;
            }
        } else {
            if (currentZ == FIRST_Z + 2) {
                currentZ -= 2;
            } else {
                currentZ -= 4;
            }
        }

        if (currentX > LAST_X) {
            changeDiggerStatus(DiggerStatus.DISABLED);
        }
    }

    private boolean isHoleOdd() {
        return currentX % 8 != 0 && currentX != LAST_X;
    }

    private void stop() {
        move = null;
        tickCounter = 0;
    }

    private int[] extractCoords(String message) {
        int first = message.indexOf('[');
        int last = message.indexOf(']');
        if (first != -1 && last != -1) {
            String[] coords = message.substring(first + 1, last).split(";");
            int x = Integer.parseInt(coords[0]);
            int z = Integer.parseInt(coords[1]);
            return new int[]{x, z};
        } else {
            return new int[]{};
        }
    }

    private void throwItem() {
        connection.sendPacket(new PlayerDiggingPacket(3, new Position(0, 0, 0), 0));
    }

    private boolean isEmpty() {
        return currentX % 8 == 5 && currentZ % 8 == 3;
    }
}
