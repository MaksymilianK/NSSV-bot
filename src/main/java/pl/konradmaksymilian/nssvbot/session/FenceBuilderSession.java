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
import java.util.Arrays;

public class FenceBuilderSession extends MovableSession {

    private final int FIRST_X = 28400;
    private final int FIRST_Z = -5567;
    private final int LAST_X = 28558;
    private final int LAST_Z = -5409;

    private final int CHEST_X = 28475;
    private final int CHEST_Y = 1;
    private final int CHEST_Z = -5487;

    private FenceBuilderStatus fenceBuilderStatus = FenceBuilderStatus.DISABLED;
    private int currentX;
    private int currentZ;

    private final Slot[] inventory = new Slot[36];
    private int actionCounter = 1;
    private boolean waitForOpen = false;

    public FenceBuilderSession(ConnectionManager connection, Timer timer) {
        super(connection, timer);
        for (int i = 0; i < 36; i++) {
            inventory[i] = new Slot(new byte[] {});
        }
        timer.setTimeToNow("nextPossibleBuy");
        timer.setTimeFromNow("checkHand", Duration.ofSeconds(Integer.MAX_VALUE));
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
        if (fenceBuilderStatus.equals(FenceBuilderStatus.DISABLED) || packet.getStateID() == 0) {
            return;
        }

        if (fenceBuilderStatus.equals(FenceBuilderStatus.BUILDING_FENCE) && // || builderStatus.equals(SlabBuilderStatus.MOVING_SLABS)) &&
                packet.getPosition().getY() == (int) feetY - 1 && packet.getPosition().getX() == currentX && packet.getPosition().getZ() == currentZ) {
//            cancelledX.add(packet.getPosition().getX());
//            cancelledZ.add(packet.getPosition().getZ());
            System.out.println("block change " + packet.getStateID());
            timer.setTimeFromNow("checkHand", Duration.ofSeconds(Integer.MAX_VALUE));
            if ((packet.getStateID() == 1714 || packet.getStateID() == 1712) && waitForOpen) {
                openFence();
            } else if (packet.getStateID() == 1716 || packet.getStateID() == 1718) {
                nextFence();
                moveToFence();
            }
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

        if (fenceBuilderStatus.equals(FenceBuilderStatus.BUYING)) {
            if (message.endsWith("wysprzedany.")) {
                changeFenceBuilderStatus(FenceBuilderStatus.TP_TO_WORK);
            } else if (message.contains("w ekwipunku.")) {
                changeFenceBuilderStatus(FenceBuilderStatus.TP_TO_WORK);
            }
        }

        if (!message.endsWith("-")) {
            return;
        }

        if (message.endsWith("--start--")) {
            int[] coords = extractCoords(message);
            if (coords.length > 0) {
                startBuildingFence(coords[0], coords[1]);
            } else {
                startBuildingFence();
            }
        } else if (message.endsWith("--stop--")) {
            stop();
        } else if (message.endsWith("--throw--")) {
            throwItem();
        } else if (message.endsWith("--tpa--")) {
            connection.sendPacket(new ChatMessageServerboundPacket("/tpa geniuszmistrz"));
        } else if (message.endsWith("--status--")) {
            System.out.println(fenceBuilderStatus);
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
//            if (packet.getSlot() == 36) {
//                counter++;
//                if (counter == 2) {
//                    nextSlab();
//                    moveToSlab();
//                    counter = 0;
//                }
//            }
        }
        System.out.println("set slot");
    }

    protected void onWindowItems(WindowItemsPacket packet) {
        if (!status.equals(Status.GAME) || packet.getWindowId() != 0) {
            return;
        }
        for (int i = 9; i < 45; i++) {
            inventory[i - 9].setData(packet.getSlotData()[i]);
            System.out.println(inventory[i - 9].getCount());
            if (fenceBuilderStatus.equals(FenceBuilderStatus.BUILDING_FENCE) && inventory[27].getCount() == 0) {
                System.out.println("empty!!!!!!!!!!!!");
               // changeSlabBuilderStatus(SlabBuilderStatus.MOVING_SLABS);
            }
        }
    }

    @Override
    protected void onPlayerPositionAndLook(PlayerPositionAndLookClientboundPacket packet) {
        super.onPlayerPositionAndLook(packet);
        if (!status.equals(Status.GAME)) {
            return;
        }

        if (fenceBuilderStatus.equals(FenceBuilderStatus.TP_TO_CHESTS)) {
            //cancelledX.add(currentX);
            //cancelledZ.add(currentZ);
            moveToChest();
        } else if (fenceBuilderStatus.equals(FenceBuilderStatus.TP_TO_WORK)) {
            //currentX = cancelledX.poll();
            //currentZ = cancelledZ.poll();
            moveToFence();
        }
    }

    @Override
    protected void onEveryCheck() {
        super.onEveryCheck();

        if (fenceBuilderStatus.equals(FenceBuilderStatus.DISABLED)) {
            return;
        }

        if (fenceBuilderStatus.equals(FenceBuilderStatus.MOVING_FENCE) && !isMoving()) {
            changeFenceBuilderStatus(FenceBuilderStatus.BUILDING_FENCE);
        } else if (fenceBuilderStatus.equals(FenceBuilderStatus.MOVING_CHEST) && !isMoving()) {
            changeFenceBuilderStatus(FenceBuilderStatus.BUYING);
        } else if (fenceBuilderStatus.equals(FenceBuilderStatus.BUYING)) {
            buy();
        } else if (fenceBuilderStatus.equals(FenceBuilderStatus.BUILDING_FENCE)) {
            if (timer.isNowAfter("checkHand")) {
                timer.setTimeFromNow("checkHand", Duration.ofSeconds(Integer.MAX_VALUE));
                placeFence();
            }
        }
    }

    private void onChangeFenceBuilderStatus() {
        if (fenceBuilderStatus.equals(FenceBuilderStatus.BUILDING_FENCE)) {
            placeFence();
        } else if (fenceBuilderStatus.equals(FenceBuilderStatus.TP_TO_CHESTS)) {
            connection.sendPacket(new ChatMessageServerboundPacket("/sethome"));
            connection.sendPacket(new ChatMessageServerboundPacket("/p visit GeniuszMistrz 2"));
            System.out.println("sethome");
        } else if (fenceBuilderStatus.equals(FenceBuilderStatus.TP_TO_WORK)) {
            connection.sendPacket(new ChatMessageServerboundPacket("/home"));
        }
    }

    private void changeFenceBuilderStatus(FenceBuilderStatus fenceBuilderStatus) {
        this.fenceBuilderStatus = fenceBuilderStatus;
        onChangeFenceBuilderStatus();
    }

    private void buy() {
        if (!timer.isNowAfter("nextPossibleBuy")) {
            return;
        }

        connection.sendPacket(PlayerBlockPlacementPacket.builder()
                .cursorX(0.5f)
                .cursorY(0.5f)
                .cursorZ(0.125f)
                .face(3)
                .hand(0)
                .x(CHEST_X)
                .y(CHEST_Y)
                .z(CHEST_Z)
                .build());

        timer.setTimeFromNow("nextPossibleBuy", Duration.ofMillis(500));
    }

    private void moveToChest() {
        changeFenceBuilderStatus(FenceBuilderStatus.MOVING_CHEST);
        setNewDestination(x, z, -145.0f, 32.5f);
    }

    private void startBuildingFence() {
        startBuildingFence(FIRST_X, FIRST_Z);
    }

    private void startBuildingFence(int firstX, int firstZ) {
        currentX = firstX;
        currentZ = firstZ;
        //cacheX = 0;
        //cacheZ = 0;
        moveToFence();
    }

    private void moveToFence() {
        changeFenceBuilderStatus(FenceBuilderStatus.MOVING_FENCE);
        double newX, newZ;
        float newYaw, newPitch;

        if (isFenceOdd()) {
            if (currentZ == LAST_Z) {
                newX = currentX - 0.5;
                newZ = currentZ - 0.5;
                newYaw = -20.5f;
                newPitch = 49.5f;
            } else {
                newX = currentX - 0.5;
                newZ = currentZ + 1.5;
                newYaw = -160.5f;
                newPitch = 49.5f;
            }
        } else {
            if (currentX == FIRST_X) {
                if (currentZ == FIRST_Z) {
                    newX = currentX + 1.5;
                    newZ = currentZ + 1.5;
                    newYaw = 160.5f;
                    newPitch = 49.5f;
                } else {
                    newX = currentX + 1.5;
                    newZ = currentZ - 0.5;
                    newYaw = 20.5f;
                    newPitch = 49.5f;
                }
            } else if (currentZ == FIRST_Z) {
                newX = currentX - 0.5;
                newZ = currentZ + 1.5;
                newYaw = -160.5f;
                newPitch = 49.5f;
            } else {
                newX = currentX - 0.5;
                newZ = currentZ - 0.5;
                newYaw = -20.5f;
                newPitch = 49.5f;
            }
        }
        setNewDestination(newX, newZ, newYaw, newPitch);
        System.out.println("current " + currentX + " " + currentZ + " " + newYaw);
    }

    private void placeFence() {
        int eq = checkEq();

        if (eq == -1) {
            changeFenceBuilderStatus(FenceBuilderStatus.TP_TO_CHESTS);
            return;
        } else if (eq == 0) {
            changeFenceBuilderStatus(FenceBuilderStatus.MOVING_FENCE);
            return;
        } else {
            timer.setTimeFromNow("checkHand", Duration.ofSeconds(1));
        }

        if (!isFenceOdd() && currentZ % 8 == -1) {
            nextFence();
            moveToFence();
            return;
        }

        int side;
        float cursorX;
        float cursorZ;
        int placeZ;
        if ((isFenceOdd() && currentZ != LAST_Z) || (!isFenceOdd() && currentZ == FIRST_Z)) {
            side = 3;
            cursorX = 0.05f;
            cursorZ = 1.0f;
            placeZ = currentZ - 1;
        } else {
            side = 2;
            cursorX = 0.95f;
            cursorZ = 0.0f;
            placeZ = currentZ + 1;
        }

        System.out.println("fence " + side + " " + currentZ);
        connection.sendPacket(PlayerBlockPlacementPacket.builder()
                .x(currentX)
                .y((int) feetY - 1)
                .z(placeZ)
                .face(side)
                .hand(0)
                .cursorX(cursorX)
                .cursorY(0.875f)
                .cursorZ(cursorZ)
                .build());
        connection.sendPacket(new AnimationPacket(0));

        waitForOpen = true;
    }

    private void openFence() {
        System.out.println("open " + currentZ);
        connection.sendPacket(PlayerBlockPlacementPacket.builder()
                .x(currentX)
                .y((int) feetY - 1)
                .z(currentZ)
                .face(1)
                .hand(0)
                .cursorX(0.95f)
                .cursorY(1.0f)
                .cursorZ(0.5f)
                .build());
        connection.sendPacket(new AnimationPacket(0));

        waitForOpen = false;
    }

    private void nextFence() {
        if ((currentZ == LAST_Z && !isFenceOdd()) || (currentZ == FIRST_Z && isFenceOdd())) {
            if (currentX == LAST_X - 2) {
                currentX += 2;
            } else {
                currentX += 4;
            }
        } else if (!isFenceOdd()) {
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
            changeFenceBuilderStatus(FenceBuilderStatus.DISABLED);
        }
    }

    private boolean isFenceOdd() {
        return currentX % 8 != 0 && currentX != LAST_X;
    }

    private int checkEq() {
        System.out.println("eq");
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
                    connection.sendPacket(new CloseWindowPacket(0));
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
        changeFenceBuilderStatus(FenceBuilderStatus.DISABLED);
        //cancelledX.clear();
        //cancelledZ.clear();
        move = null;
        waitForOpen = false;
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
