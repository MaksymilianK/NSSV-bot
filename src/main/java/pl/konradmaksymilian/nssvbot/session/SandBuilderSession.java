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

public class SandBuilderSession extends MovableSession {

    private final int FIRST_X = 28401;
    private final int FIRST_Z = -5566;
    private final int LAST_X = 28557;
    private final int LAST_Z = -5410;

    private final int CHEST_X = 28475;
    private final int CHEST_Y = 1;
    private final int CHEST_Z = -5485;

    private SandBuilderStatus builderStatus = SandBuilderStatus.DISABLED;
    private int currentX;
    private int currentZ;

    private final Slot[] inventory = new Slot[36];
    private int actionCounter = 1;

    public SandBuilderSession(ConnectionManager connection, Timer timer) {
        super(connection, timer);
        for (int i = 0; i < 36; i++) {
            inventory[i] = new Slot(new byte[]{});
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
        if (builderStatus.equals(SandBuilderStatus.DISABLED) || packet.getStateID() == 0) {
            return;
        }

        if (builderStatus.equals(SandBuilderStatus.BUILDING_SAND) && // || builderStatus.equals(SandBuilderStatus.MOVING_SANDS)) &&
                packet.getPosition().getY() == (int) feetY - 1 && packet.getPosition().getX() == currentX && packet.getPosition().getZ() == currentZ) {
//            cancelledX.add(packet.getPosition().getX());
//            cancelledZ.add(packet.getPosition().getZ());
            System.out.println("block change " + packet.getStateID());
            timer.setTimeFromNow("checkHand", Duration.ofSeconds(Integer.MAX_VALUE));
            nextSand();
            moveToSand();
//            counter++;
//            if (counter == 2) {
//                nextSand();
//                moveToSand();
//                counter = 0;
//            }
        }
    }

    @Override
    protected void onChatMessage(ChatMessageClientboundPacket packet) {
        super.onChatMessage(packet);
        String message = ChatFormatter.getPureText(packet.getMessage().getComponents());

        if (builderStatus.equals(SandBuilderStatus.BUYING)) {
            if (message.endsWith("wysprzedany.")) {
                changeSandBuilderStatus(SandBuilderStatus.TP_TO_WORK);
            } else if (message.contains("w ekwipunku.")) {
                changeSandBuilderStatus(SandBuilderStatus.TP_TO_WORK);
            }
        }

        if (!message.endsWith("-")) {
            return;
        }

        if (message.endsWith("--start--")) {
            int[] coords = extractCoords(message);
            if (coords.length > 0) {
                startBuildingSand(coords[0], coords[1]);
            } else {
                startBuildingSand();
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
//            if (packet.getSlot() == 36) {
//                counter++;
//                if (counter == 2) {
//                    nextSand();
//                    moveToSand();
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
            if (builderStatus.equals(SandBuilderStatus.BUILDING_SAND) && inventory[27].getCount() == 0) {
                System.out.println("empty!!!!!!!!!!!!");
                // changeSandBuilderStatus(SandBuilderStatus.MOVING_SANDS);
            }
        }
    }

    @Override
    protected void onPlayerPositionAndLook(PlayerPositionAndLookClientboundPacket packet) {
        super.onPlayerPositionAndLook(packet);
        if (!status.equals(Status.GAME)) {
            return;
        }

        if (builderStatus.equals(SandBuilderStatus.TP_TO_CHESTS)) {
            //cancelledX.add(currentX);
            //cancelledZ.add(currentZ);
            moveToChest();
        } else if (builderStatus.equals(SandBuilderStatus.TP_TO_WORK)) {
            //currentX = cancelledX.poll();
            //currentZ = cancelledZ.poll();
            moveToSand();
        }
    }

    @Override
    protected void onEveryCheck() {
        super.onEveryCheck();

        if (builderStatus.equals(SandBuilderStatus.DISABLED)) {
            return;
        }

        if (builderStatus.equals(SandBuilderStatus.MOVING_SAND) && !isMoving()) {
            changeSandBuilderStatus(SandBuilderStatus.BUILDING_SAND);
        } else if (builderStatus.equals(SandBuilderStatus.MOVING_CHEST) && !isMoving()) {
            changeSandBuilderStatus(SandBuilderStatus.BUYING);
        } else if (builderStatus.equals(SandBuilderStatus.BUYING)) {
            buy();
        } else if (builderStatus.equals(SandBuilderStatus.BUILDING_SAND)) {
            if (timer.isNowAfter("checkHand")) {
                timer.setTimeFromNow("checkHand", Duration.ofSeconds(Integer.MAX_VALUE));
                placeSand();
            }
        }
    }

    private void onChangeSandBuilderStatus() {
        if (builderStatus.equals(SandBuilderStatus.BUILDING_SAND)) {
            placeSand();
        } else if (builderStatus.equals(SandBuilderStatus.TP_TO_CHESTS)) {
            connection.sendPacket(new ChatMessageServerboundPacket("/sethome"));
            connection.sendPacket(new ChatMessageServerboundPacket("/p visit GeniuszMistrz 2"));
            System.out.println("sethome");
        } else if (builderStatus.equals(SandBuilderStatus.TP_TO_WORK)) {
            connection.sendPacket(new ChatMessageServerboundPacket("/home"));
        }
    }

    private void changeSandBuilderStatus(SandBuilderStatus builderStatus) {
        this.builderStatus = builderStatus;
        onChangeSandBuilderStatus();
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
        changeSandBuilderStatus(SandBuilderStatus.MOVING_CHEST);
        setNewDestination(x, z, getYaw(x, z, CHEST_X, CHEST_Z), getPitch(x, z, CHEST_X, CHEST_Y, CHEST_Z));
    }

    private void startBuildingSand() {
        startBuildingSand(FIRST_X, FIRST_Z);
    }

    private void startBuildingSand(int firstX, int firstZ) {
        currentX = firstX;
        currentZ = firstZ;
        moveToSand();
    }

    private void moveToSand() {
        changeSandBuilderStatus(SandBuilderStatus.MOVING_SAND);
        double newX, newZ;
        float newYaw, newPitch;

        if (isSandOdd()) {
            if (currentZ == FIRST_Z) {
                newX = currentX + 1.5d;
                newZ = currentZ + 0.5d;
                newYaw = 90.0f;
                newPitch = 57.0f;
            } else {
                newX = currentX + 0.5d;
                newZ = currentZ - 0.5d;
                newYaw = 0.1f;
                newPitch = 57.1f;
            }
        } else {
            if (currentZ == LAST_Z) {
                newX = currentX + 1.5d;
                newZ = currentZ + 0.5d;
                newYaw = 90.0f;
                newPitch = 56.9f;
            } else {
                newX = currentX + 0.5d;
                newZ = currentZ + 1.5d;
                newYaw = -179.9f;
                newPitch = 57.2f;
            }
        }
        setNewDestination(newX, newZ, newYaw, newPitch);
        System.out.println("current " + currentX + " " + currentZ + " " + newYaw);
    }

    private void placeSand() {
        int eq = checkEq();

        if (eq == -1) {
            changeSandBuilderStatus(SandBuilderStatus.TP_TO_CHESTS);
            return;
        } else if (eq == 0) {
            changeSandBuilderStatus(SandBuilderStatus.MOVING_SAND);
            return;
        } else {
            timer.setTimeFromNow("checkHand", Duration.ofSeconds(1));
        }

        connection.sendPacket(PlayerBlockPlacementPacket.builder()
                .x(currentX)
                .y((int) feetY - 1)
                .z(currentZ)
                .face(1)
                .hand(0)
                .cursorX(0.5f)
                .cursorY(1.0f)
                .cursorZ(0.5f)
                .build());
        connection.sendPacket(new AnimationPacket(0));
    }

    private void nextSand() {
        if ((currentZ == LAST_Z && !isSandOdd()) || (currentZ == FIRST_Z && isSandOdd())) {
            currentX += 2;
        } else if (!isSandOdd()) {
            currentZ += 2;
        } else {
            currentZ -= 2;
        }

        if (currentX > LAST_X) {
            changeSandBuilderStatus(SandBuilderStatus.DISABLED);
        }
    }

    private boolean isSandOdd() {
        return currentX % 4 == 3;
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
            System.out.println("Skonczyl sie piasek :(");
            return -1;
        } else {
            return 1;
        }
    }

    private void stop() {
        changeSandBuilderStatus(SandBuilderStatus.DISABLED);
        move = null;
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
}
