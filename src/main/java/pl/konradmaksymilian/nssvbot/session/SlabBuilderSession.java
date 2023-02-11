package pl.konradmaksymilian.nssvbot.session;

import pl.konradmaksymilian.nssvbot.connection.ConnectionManager;
import pl.konradmaksymilian.nssvbot.protocol.Position;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.*;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.*;
import pl.konradmaksymilian.nssvbot.utils.ChatFormatter;
import pl.konradmaksymilian.nssvbot.utils.Timer;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

public class SlabBuilderSession extends MovableSession {

    private final int FIRST_X = 28474;
    private final int FIRST_Z = -5567;
    private final int LAST_X = 28558;
    private final int LAST_Z = -5425;

    private final int CHEST_X = 28475;
    private final int CHEST_Y = 1;
    private final int CHEST_Z = -5486;

    private SlabBuilderStatus builderStatus = SlabBuilderStatus.DISABLED;
    private int currentX;
    private int currentZ;
    private int cacheX;
    private int cacheZ;
    private Queue<Integer> cancelledX = new ArrayDeque<>();
    private Queue<Integer> cancelledZ = new ArrayDeque<>();
    private Slot[] inventory = new Slot[36];
    private int actionCounter = 1;

    public SlabBuilderSession(ConnectionManager connection, Timer timer) {
        super(connection, timer);
        for (int i = 0; i < 36; i++) {
            inventory[i] = new Slot(new byte[] {});
        }
        timer.setTimeToNow("nextPossibleBuy");
    }

    @Override
    protected void onPacket(Packet packet) {
        super.onPacket(packet);
        if (!status.equals(Status.GAME)) {
            return;
        }

        if (packet.getName().equals(PacketName.BLOCK_CHANGE)) {
            onBlockChange((BlockChangePacket) packet);
        } else if (packet.getName().equals(PacketName.WINDOW_ITEMS)) {
            onWindowItems((WindowItemsPacket) packet);
        }
    }

    private void onBlockChange(BlockChangePacket packet) {
        if (builderStatus.equals(SlabBuilderStatus.DISABLED) || packet.getStateID() == 0) {
            return;
        }

        if (builderStatus.equals(SlabBuilderStatus.BUILDING_SLABS) && // || builderStatus.equals(SlabBuilderStatus.MOVING_SLABS)) &&
                packet.getPosition().getY() == (int) feetY - 1 && packet.getPosition().getX() == currentX && packet.getPosition().getZ() == currentZ) {
//            cancelledX.add(packet.getPosition().getX());
//            cancelledZ.add(packet.getPosition().getZ());
            nextSlab();
            moveToSlab();
        }
    }

    @Override
    protected void onChatMessage(ChatMessageClientboundPacket packet) {
        super.onChatMessage(packet);
        String message = ChatFormatter.getPureText(packet.getMessage().getComponents());

        if (builderStatus.equals(SlabBuilderStatus.BUYING)) {
            if (message.endsWith("wysprzedany.")) {
                changeSlabBuilderStatus(SlabBuilderStatus.DISABLED);
            } else if (message.contains("w ekwipunku.")) {
                changeSlabBuilderStatus(SlabBuilderStatus.TP_TO_WORK);
            }
        }

        if (!message.endsWith("-")) {
            return;
        }

        if (message.endsWith("--start--")) {
            int[] coords = extractCoords(message);
            if (coords.length > 0) {
                startBuildingSlabs(coords[0], coords[1]);
            } else {
                startBuildingSlabs();
            }
        } else if (message.endsWith("--stop--")) {
            stop();
        } else if (message.endsWith("--throw--")) {
            throwItem();
        } else if (message.endsWith("--tpa--")) {
            connection.sendPacket(new ChatMessageServerboundPacket("/tpa geniuszmistrz"));
        } else if (message.endsWith("--status--")) {
            System.out.println(builderStatus);
        } else if (message.endsWith("--pos--")) {
            if (move != null) {
                System.out.println(move.getDestinationX() + " " + move.getDestinationZ());
            }
            System.out.println(x + " " + z);
        }
    }

    @Override
    protected void onSetSlot(SetSlotPacket packet) {
        super.onSetSlot(packet);
        if (!status.equals(Status.GAME)) {
            return;
        }
        if (packet.getWindowId() == 0 && packet.getSlot() > 8) {
            inventory[packet.getSlot() - 9].setData(packet.getSlotData());
        }
    }

    protected void onWindowItems(WindowItemsPacket packet) {
        if (!status.equals(Status.GAME) || packet.getWindowId() != 0) {
            return;
        }
        for (int i = 9; i < 45; i++) {
            inventory[i - 9].setData(packet.getSlotData()[i]);
            System.out.println(inventory[i - 9].getCount());
        }
    }

    @Override
    protected void onPlayerPositionAndLook(PlayerPositionAndLookClientboundPacket packet) {
        super.onPlayerPositionAndLook(packet);
        if (!status.equals(Status.GAME)) {
            return;
        }

        if (builderStatus.equals(SlabBuilderStatus.TP_TO_CHESTS)) {
            cancelledX.add(currentX);
            cancelledZ.add(currentZ);
            moveToChest();
        } else if (builderStatus.equals(SlabBuilderStatus.TP_TO_WORK)) {
            currentX = cancelledX.poll();
            currentZ = cancelledZ.poll();
            moveToSlab();
        }
    }

    @Override
    protected void onEveryCheck() {
        super.onEveryCheck();

        if (builderStatus.equals(SlabBuilderStatus.DISABLED)) {
            return;
        }

        if (builderStatus.equals(SlabBuilderStatus.MOVING_SLABS) && !isMoving()) {
            changeSlabBuilderStatus(SlabBuilderStatus.BUILDING_SLABS);
        } else if (builderStatus.equals(SlabBuilderStatus.MOVING_CHEST) && !isMoving()) {
            changeSlabBuilderStatus(SlabBuilderStatus.BUYING);
        } else if (builderStatus.equals(SlabBuilderStatus.BUYING)) {
            buy();
        }
    }

    private void onChangeSlabBuilderStatus() {
        if (builderStatus.equals(SlabBuilderStatus.BUILDING_SLABS)) {
            placeSlab();
        } else if (builderStatus.equals(SlabBuilderStatus.TP_TO_CHESTS)) {
            connection.sendPacket(new ChatMessageServerboundPacket("/p visit GeniuszMistrz 2"));
        } else if (builderStatus.equals(SlabBuilderStatus.TP_TO_WORK)) {
            connection.sendPacket(new ChatMessageServerboundPacket("/home"));
        }
    }

    private void changeSlabBuilderStatus(SlabBuilderStatus builderStatus) {
        this.builderStatus = builderStatus;
        onChangeSlabBuilderStatus();
    }

    private void buy() {
        if (!timer.isNowAfter("nextPossibleBuy")) {
            return;
        }

        connection.sendPacket(PlayerBlockPlacementPacket.builder()
                .cursorX(0.875f)
                .cursorY(0.5f)
                .cursorZ(0.5f)
                .face(4)
                .hand(0)
                .x(CHEST_X)
                .y(CHEST_Y)
                .z(CHEST_Z)
                .build());

        timer.setTimeFromNow("nextPossibleBuy", Duration.ofMillis(500));
    }

    private void moveToChest() {
        changeSlabBuilderStatus(SlabBuilderStatus.MOVING_CHEST);
        setNewDestination(x, z, getYaw(x, z, CHEST_X, CHEST_Z), getPitch(x, z, CHEST_X, CHEST_Y, CHEST_Z));
    }

    private void startBuildingSlabs() {
        startBuildingSlabs(FIRST_X, FIRST_Z);
    }

    private void startBuildingSlabs(int firstX, int firstZ) {
        connection.sendPacket(new HeldItemChangeServerboundPacket((short) 0));
        currentX = firstX;
        currentZ = firstZ;
        cacheX = 0;
        cacheZ = 0;
        moveToSlab();
    }

    private void moveToSlab() {
        changeSlabBuilderStatus(SlabBuilderStatus.MOVING_SLABS);
        double newX, newZ;
        float newYaw, newPitch;

        if (isSlabOdd()) {
            if (currentZ == LAST_Z) {
                newX = x;
                newZ = z;
                newYaw = -130.0f;
                newPitch = 55.0f;
            } else {
                newX = (double) currentX + 0.5d;
                newZ = (double) currentZ + 0.75d;
                newYaw = 0.1f;
                newPitch = 82.2f;
            }
        } else {
            if (currentZ == FIRST_Z) {
                newX = x;
                newZ = z;
                newYaw = -52.4f;
                newPitch = 55.0f;
            } else {
                newX = (double) currentX + 0.5d;
                newZ = (double) currentZ + 0.25d;
                newYaw = 179.9f;
                newPitch = 82.2f;
            }
        }
        setNewDestination(newX, newZ, newYaw, newPitch);
        System.out.println("current " + currentX + " " + currentZ + " " + newYaw);
    }

    private void placeSlab() {
        int eq = checkEq();

        if (eq == -1) {
            connection.sendPacket(new ChatMessageServerboundPacket("/sethome"));
            System.out.println("sethome");
            changeSlabBuilderStatus(SlabBuilderStatus.TP_TO_CHESTS);
            return;
        } else if (eq == 0) {
            changeSlabBuilderStatus(SlabBuilderStatus.MOVING_SLABS);
            return;
        }

        connection.sendPacket(PlayerBlockPlacementPacket.builder()
                .x(currentX)
                .y((int) feetY - 1)
                .z(isSlabOdd() ? currentZ + 1 : currentZ - 1)
                .face(isSlabOdd() ? 2 : 3)
                .hand(0)
                .cursorX(0.5f)
                .cursorY(0.875f)
                .cursorZ(isSlabOdd() ? 0.0f : 1.0f)
                .build());
        connection.sendPacket(new AnimationPacket(0));
    }

    private void nextSlab() {
        if (cacheX != 0) {
            currentX = cacheX;
            currentZ = cacheZ;
            cacheX = 0;
            cacheZ = 0;
        }

        if (cancelledX.isEmpty()) {
            if ((currentZ == LAST_Z && !isSlabOdd()) || (currentZ == FIRST_Z && isSlabOdd())) {
                currentX++;
            } else if (!isSlabOdd()) {
                currentZ++;
            } else {
                currentZ--;
            }
        } else {
            cacheX = currentX;
            cacheZ = currentZ;
            currentX = cancelledX.poll();
            currentZ = cancelledZ.poll();
        }
    }

    private boolean isSlabOdd() {
        return currentX % 2 == 1;
    }

    private int checkEq() {
        if (!inventory[27].isPresent()) {
            for (int i = 0; i < 36; i++) {
                if (inventory[i].isPresent()) {
                    System.out.println(" non empty " + Arrays.toString(inventory[i].getData()));
                    connection.sendPacket(new ClickWindowPacket(
                            0, i + 9, 0, actionCounter, 0, inventory[i].getData()
                    ));
                    actionCounter++;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    connection.sendPacket(new ClickWindowPacket(
                            0, 36, 0, actionCounter, 0, inventory[i].getData()
                    ));
                    actionCounter++;
                    return 0;
                } else {
                    System.out.println(" empty " + Arrays.toString(inventory[i].getData()));
                }
            }
            System.out.println("Skonczyly sie plytki :(");
            return -1;
        } else {
            return 1;
        }
    }

    private void stop() {
        builderStatus = SlabBuilderStatus.DISABLED;
        cancelledX.clear();
        cancelledZ.clear();
        move = null;
    }

    private int[] extractCoords(String message) {
        int first = message.indexOf('[');
        int last = message.indexOf(']');
        if (first != -1 && last != -1) {
            String[] coords = message.substring(first + 1, last).split(";");
            int x = Integer.parseInt(coords[0]);
            int z = Integer.parseInt(coords[1]);
            return new int[] {x, z};
        } else {
            return new int[] {};
        }
    }

    private void throwItem() {
        connection.sendPacket(new PlayerDiggingPacket(3, new Position(0, 0, 0), 0));
    }

    private boolean isEmpty() {
        return currentX % 8 == 5 && currentZ % 8 == 3;
    }
}
