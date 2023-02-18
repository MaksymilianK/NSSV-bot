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

public class CactusBuilderSession extends MovableSession {

    private final int FIRST_X = 28401;
    private final int FIRST_Z = -5566;
    private final int LAST_X = 28555;
    private final int LAST_Z = -5410;

    private final int CHEST_X = 28473;
    private final int CHEST_Y = 1;
    private final int CHEST_Z = -5485;

    private CactusBuilderStatus builderStatus = CactusBuilderStatus.DISABLED;
    private int currentX;
    private int currentZ;

    private final Slot[] inventory = new Slot[36];
    private int actionCounter = 1;

    public CactusBuilderSession(ConnectionManager connection, Timer timer) {
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
        if (builderStatus.equals(CactusBuilderStatus.DISABLED) || packet.getStateID() == 0) {
            return;
        }

        if (builderStatus.equals(CactusBuilderStatus.BUILDING_CACTUS) && // || builderStatus.equals(CactusBuilderStatus.MOVING_CACTUSS)) &&
                packet.getPosition().getY() == (int) feetY && packet.getPosition().getX() == currentX && packet.getPosition().getZ() == currentZ) {
//            cancelledX.add(packet.getPosition().getX());
//            cancelledZ.add(packet.getPosition().getZ());
            System.out.println("block change " + packet.getStateID());
            timer.setTimeFromNow("checkHand", Duration.ofSeconds(Integer.MAX_VALUE));
            nextCactus();
            moveToCactus();
//            counter++;
//            if (counter == 2) {
//                nextCactus();
//                moveToCactus();
//                counter = 0;
//            }
        }
    }

    @Override
    protected void onChatMessage(ChatMessageClientboundPacket packet) {
        super.onChatMessage(packet);
        String message = ChatFormatter.getPureText(packet.getMessage().getComponents());

        if (builderStatus.equals(CactusBuilderStatus.BUYING)) {
            if (message.endsWith("wysprzedany.")) {
                changeCactusBuilderStatus(CactusBuilderStatus.TP_TO_WORK);
            } else if (message.contains("w ekwipunku.")) {
                changeCactusBuilderStatus(CactusBuilderStatus.TP_TO_WORK);
            }
        }

        if (!message.endsWith("-")) {
            return;
        }

        if (message.endsWith("--start--")) {
            int[] coords = extractCoords(message);
            if (coords.length > 0) {
                startBuildingCactus(coords[0], coords[1]);
            } else {
                startBuildingCactus();
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
//                    nextCactus();
//                    moveToCactus();
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
            if (builderStatus.equals(CactusBuilderStatus.BUILDING_CACTUS) && inventory[27].getCount() == 0) {
                System.out.println("empty!!!!!!!!!!!!");
                // changeCactusBuilderStatus(CactusBuilderStatus.MOVING_CACTUSS);
            }
        }
    }

    @Override
    protected void onPlayerPositionAndLook(PlayerPositionAndLookClientboundPacket packet) {
        super.onPlayerPositionAndLook(packet);
        if (!status.equals(Status.GAME)) {
            return;
        }

        if (builderStatus.equals(CactusBuilderStatus.TP_TO_CHESTS)) {
            //cancelledX.add(currentX);
            //cancelledZ.add(currentZ);
            moveToChest();
        } else if (builderStatus.equals(CactusBuilderStatus.TP_TO_WORK)) {
            //currentX = cancelledX.poll();
            //currentZ = cancelledZ.poll();
            moveToCactus();
        }
    }

    @Override
    protected void onEveryCheck() {
        super.onEveryCheck();

        if (builderStatus.equals(CactusBuilderStatus.DISABLED)) {
            return;
        }

        if (builderStatus.equals(CactusBuilderStatus.MOVING_CACTUS) && !isMoving()) {
            changeCactusBuilderStatus(CactusBuilderStatus.BUILDING_CACTUS);
        } else if (builderStatus.equals(CactusBuilderStatus.MOVING_CHEST) && !isMoving()) {
            changeCactusBuilderStatus(CactusBuilderStatus.BUYING);
        } else if (builderStatus.equals(CactusBuilderStatus.BUYING)) {
            buy();
        } else if (builderStatus.equals(CactusBuilderStatus.BUILDING_CACTUS)) {
            if (timer.isNowAfter("checkHand")) {
                timer.setTimeFromNow("checkHand", Duration.ofSeconds(Integer.MAX_VALUE));
                placeCactus();
            }
        }
    }

    private void onChangeCactusBuilderStatus() {
        if (builderStatus.equals(CactusBuilderStatus.BUILDING_CACTUS)) {
            placeCactus();
        } else if (builderStatus.equals(CactusBuilderStatus.TP_TO_CHESTS)) {
            connection.sendPacket(new ChatMessageServerboundPacket("/sethome"));
            connection.sendPacket(new ChatMessageServerboundPacket("/p visit GeniuszMistrz 2"));
            System.out.println("sethome");
        } else if (builderStatus.equals(CactusBuilderStatus.TP_TO_WORK)) {
            connection.sendPacket(new ChatMessageServerboundPacket("/home"));
        }
    }

    private void changeCactusBuilderStatus(CactusBuilderStatus builderStatus) {
        this.builderStatus = builderStatus;
        onChangeCactusBuilderStatus();
    }

    private void buy() {
        if (!timer.isNowAfter("nextPossibleBuy")) {
            return;
        }

        connection.sendPacket(PlayerBlockPlacementPacket.builder()
                .cursorX(0.125f)
                .cursorY(0.5f)
                .cursorZ(0.5f)
                .face(5)
                .hand(0)
                .x(CHEST_X)
                .y(CHEST_Y)
                .z(CHEST_Z)
                .build());

        timer.setTimeFromNow("nextPossibleBuy", Duration.ofMillis(500));
    }

    private void moveToChest() {
        changeCactusBuilderStatus(CactusBuilderStatus.MOVING_CHEST);
        setNewDestination(x, z, getYaw(x, z, CHEST_X, CHEST_Z), getPitch(x, z, CHEST_X, CHEST_Y, CHEST_Z));
    }

    private void startBuildingCactus() {
        startBuildingCactus(FIRST_X, FIRST_Z);
    }

    private void startBuildingCactus(int firstX, int firstZ) {
        currentX = firstX;
        currentZ = firstZ;
        moveToCactus();
    }

    private void moveToCactus() {
        changeCactusBuilderStatus(CactusBuilderStatus.MOVING_CACTUS);
        double newX, newZ;
        float newYaw, newPitch;

        if (isCactusOdd()) {
            if (currentZ == FIRST_Z) {
                newX = currentX - 0.5d;
                newZ = currentZ + 3.5d;
                newYaw = -161.5f;
                newPitch = 11.7f;
            } else if (currentZ == LAST_Z) {
                newX = currentX - 0.5d;
                newZ = currentZ - 0.5d;
                newYaw = -43.5f;
                newPitch = 25.0f;
            } else {
                newX = currentX - 0.5d;
                newZ = currentZ + 1.5d;
                newYaw = -135.9f;
                newPitch = 24.4f;
            }
        } else {
            if (currentZ == FIRST_Z) {
                newX = currentX + 1.5d;
                newZ = currentZ + 3.5d;
                newYaw = 161.5f;
                newPitch = 11.7f;
            } else if (currentZ == LAST_Z) {
                newX = currentX + 1.5d;
                newZ = currentZ - 0.5d;
                newYaw = 43.8f;
                newPitch = 25.0f;
            } else {
                newX = currentX + 1.5d;
                newZ = currentZ + 1.5d;
                newYaw = 135.9f;
                newPitch = 24.4f;
            }
        }
        setNewDestination(newX, newZ, newYaw, newPitch);
        System.out.println("current " + currentX + " " + currentZ + " " + newYaw);
    }

    private void placeCactus() {
        int eq = checkEq();

        if (eq == -1) {
            changeCactusBuilderStatus(CactusBuilderStatus.TP_TO_CHESTS);
            return;
        } else if (eq == 0) {
            changeCactusBuilderStatus(CactusBuilderStatus.MOVING_CACTUS);
            return;
        } else {
            timer.setTimeFromNow("checkHand", Duration.ofSeconds(1));
        }

        connection.sendPacket(PlayerBlockPlacementPacket.builder()
                .x(currentX)
                .y((int) feetY)
                .z(currentZ)
                .face(1)
                .hand(0)
                .cursorX(0.5f)
                .cursorY(1.0f)
                .cursorZ(0.5f)
                .build());
        connection.sendPacket(new AnimationPacket(0));
    }

    private void nextCactus() {
        if ((currentZ == LAST_Z && !isCactusOdd()) || (currentZ == FIRST_Z && isCactusOdd())) {
            currentX += 2;
        } else if (!isCactusOdd()) {
            currentZ += 2;
        } else {
            currentZ -= 2;
        }

        if (currentX > LAST_X) {
            changeCactusBuilderStatus(CactusBuilderStatus.DISABLED);
        }
    }

    private boolean isCactusOdd() {
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
        changeCactusBuilderStatus(CactusBuilderStatus.DISABLED);
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
