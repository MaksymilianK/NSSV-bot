package pl.konradmaksymilian.nssvbot.session.v2;

import pl.konradmaksymilian.nssvbot.connection.ConnectionManager;
import pl.konradmaksymilian.nssvbot.protocol.Position;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.*;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.*;
import pl.konradmaksymilian.nssvbot.session.MovableSession;
import pl.konradmaksymilian.nssvbot.session.Slot;
import pl.konradmaksymilian.nssvbot.session.Status;
import pl.konradmaksymilian.nssvbot.utils.ChatFormatter;
import pl.konradmaksymilian.nssvbot.utils.Timer;

import java.time.Duration;
import java.util.Arrays;

public class StringBuilderSessionV2 extends MovableSession {

    private final int FIRST_X = 28736;
    private final int FIRST_Z = -7776;
    private final int LAST_X = 28895;
    private final int LAST_Z = -7617;

    private final int CHEST_X = 28475;
    private final int CHEST_Y = 1;
    private final int CHEST_Z = -5486;

    private BuilderStatusV2 builderStatus = BuilderStatusV2.DISABLED;
    private int currentX;
    private int currentZ;
    private int nextX;
    private int nextZ;

    private final Slot[] inventory = new Slot[36];
    private int actionCounter = 1;

    public StringBuilderSessionV2(ConnectionManager connection, Timer timer) {
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
        if (builderStatus.equals(BuilderStatusV2.DISABLED) || packet.getStateID() != 2114) {
            return;
        }

        if (builderStatus.equals(BuilderStatusV2.BUILDING) &&
                packet.getPosition().getY() == (int) feetY - 1 && Math.abs(packet.getPosition().getX() - currentX) == 1 && packet.getPosition().getZ() == currentZ) {
            System.out.println("block change " + packet.getStateID());
            timer.setTimeFromNow("checkHand", Duration.ofSeconds(Integer.MAX_VALUE));

            next();
            moveToBuild();
        }
    }

    @Override
    protected void onChatMessage(ChatMessageClientboundPacket packet) {
        super.onChatMessage(packet);
        String message = ChatFormatter.getPureText(packet.getMessage().getComponents());

        if (builderStatus.equals(BuilderStatusV2.BUYING)) {
            if (message.endsWith("wysprzedany.")) {
                changeBuilderStatus(BuilderStatusV2.TP_TO_WORK);
            } else if (message.contains("w ekwipunku.")) {
                changeBuilderStatus(BuilderStatusV2.TP_TO_WORK);
            }
        }

        if (!message.endsWith("-")) {
            return;
        }

        if (message.endsWith("--start--")) {
            int[] coords = extractCoords(message);
            if (coords.length > 0) {
                start(coords[0], coords[1]);
            } else {
                start();
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
        System.out.println("set slot");
    }

    protected void onWindowItems(WindowItemsPacket packet) {
        if (!status.equals(Status.GAME) || packet.getWindowId() != 0) {
            return;
        }
        for (int i = 9; i < 45; i++) {
            inventory[i - 9].setData(packet.getSlotData()[i]);
            System.out.println(inventory[i - 9].getCount());
            if (builderStatus.equals(BuilderStatusV2.BUILDING) && inventory[27].getCount() == 0) {
                System.out.println("empty!!!!!!!!!!!!");
            }
        }
    }

    @Override
    protected void onPlayerPositionAndLook(PlayerPositionAndLookClientboundPacket packet) {
        super.onPlayerPositionAndLook(packet);
        if (!status.equals(Status.GAME)) {
            return;
        }

        if (builderStatus.equals(BuilderStatusV2.TP_TO_CHESTS)) {
            moveToChest();
        } else if (builderStatus.equals(BuilderStatusV2.TP_TO_WORK)) {
            moveToBuild();
        }
    }

    @Override
    protected void onEveryCheck() {
        super.onEveryCheck();

        if (builderStatus.equals(BuilderStatusV2.DISABLED)) {
            return;
        }

        if (builderStatus.equals(BuilderStatusV2.MOVING) && !isMoving() && timer.isNowAfter("nextPossibleMove")) {
            changeBuilderStatus(BuilderStatusV2.BUILDING);
        } else if (builderStatus.equals(BuilderStatusV2.MOVING_CHEST) && !isMoving()) {
            changeBuilderStatus(BuilderStatusV2.BUYING);
        } else if (builderStatus.equals(BuilderStatusV2.BUYING)) {
            buy();
        } else if (builderStatus.equals(BuilderStatusV2.BUILDING)) {
            if (timer.isNowAfter("checkHand")) {
                timer.setTimeFromNow("checkHand", Duration.ofSeconds(Integer.MAX_VALUE));
                place();
            }
        }
    }

    private void onChangeSandBuilderStatus() {
        if (builderStatus.equals(BuilderStatusV2.BUILDING)) {
            place();
        } else if (builderStatus.equals(BuilderStatusV2.TP_TO_CHESTS)) {
            connection.sendPacket(new ChatMessageServerboundPacket("/sethome"));
            connection.sendPacket(new ChatMessageServerboundPacket("/p visit GeniuszMistrz 2"));
            System.out.println("sethome");
        } else if (builderStatus.equals(BuilderStatusV2.TP_TO_WORK)) {
            connection.sendPacket(new ChatMessageServerboundPacket("/home"));
        }
    }

    private void changeBuilderStatus(BuilderStatusV2 builderStatus) {
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
        changeBuilderStatus(BuilderStatusV2.MOVING_CHEST);
        setNewDestination(x, z, getYaw(x, z, CHEST_X, CHEST_Z), getPitch(x, z, CHEST_X, CHEST_Y, CHEST_Z));
    }

    private void start() {
        start(FIRST_X, FIRST_Z);
    }

    private void start(int firstX, int firstZ) {
        nextX = firstX;
        nextZ = firstZ;
        next();
        moveToBuild();
    }

    private void moveToBuild() {
        changeBuilderStatus(BuilderStatusV2.MOVING);
        if (isOdd() && currentZ == LAST_Z) {
            setNewDestination(
                    currentX + 0.5,
                    currentZ + 0.5,
                    getYaw(currentX + 0.5, currentZ + 0.5, currentX - 0.5f, (float) currentZ),
                    getPitch(currentX + 0.5, currentZ + 0.5, currentX - 0.5f , (float) feetY + 0.5f, (float) currentZ)
            );
            return;
        } else if (!isOdd() && currentZ == FIRST_Z) {
            setNewDestination(
                    currentX + 2.5,
                    currentZ + 2.5,
                    getYaw(currentX + 2.5, currentZ + 2.5, currentX + 1.0f, (float) currentZ + 0.5f),
                    getPitch(currentX + 2.5, currentZ + 2.5, currentX + 1.0f , (float) feetY + 0.5f, (float) currentZ + 0.5f)
            );
            return;
        }

        boolean isWest = currentX % 2 != 0;
        setNewDestination(
                nextX + 0.5,
                nextZ + 0.5,
                getYaw(nextX + 0.5, nextZ + 0.5, isWest ? (float) currentX : (float) currentX + 1.0f, (float) currentZ + 0.5f),
                getPitch(nextX + 0.5, nextZ + 0.5, isWest ? (float) currentX : (float) currentX + 1.0f, (float) feetY + 0.5f, (float) currentZ + 0.5f)
        );
        System.out.println("current " + currentX + " " + currentZ + " " + getYaw(nextX + 0.5, nextZ + 0.5, isWest ? (float) currentX : (float) currentX + 1.0f, (float) currentZ + 0.5f) + " " + getPitch(nextX + 0.5, nextZ + 0.5, isWest ? (float) currentX : (float) currentX + 1.0f, (float) feetY - 0.5f, (float) currentZ + 0.5f));
    }

    private void place() {
        int eq = checkEq();

        if (eq == -1) {
            changeBuilderStatus(BuilderStatusV2.TP_TO_CHESTS);
            return;
        } else if (eq == 0) {
            changeBuilderStatus(BuilderStatusV2.MOVING);
            return;
        } else {
            timer.setTimeFromNow("checkHand", Duration.ofSeconds(1));
        }

        if (isOdd() && currentZ == LAST_Z) {
            connection.sendPacket(PlayerBlockPlacementPacket.builder()
                    .x(currentX - 1)
                    .y((int) feetY - 1)
                    .z(currentZ - 1)
                    .face(3)
                    .hand(0)
                    .cursorX(0.5f)
                    .cursorY(0.5f)
                    .cursorZ(1.0f)
                    .build());
            connection.sendPacket(new AnimationPacket(0));
            return;
        } else if (!isOdd() && currentZ == FIRST_Z) {
            connection.sendPacket(PlayerBlockPlacementPacket.builder()
                    .x(currentX)
                    .y((int) feetY - 1)
                    .z(currentZ)
                    .face(5)
                    .hand(0)
                    .cursorX(1.0f)
                    .cursorY(0.5f)
                    .cursorZ(0.5f)
                    .build());
            connection.sendPacket(new AnimationPacket(0));
            return;
        }

        boolean isWest = currentX % 2 != 0;
        connection.sendPacket(PlayerBlockPlacementPacket.builder()
                .x(currentX)
                .y((int) feetY - 1)
                .z(currentZ)
                .face(isWest ? 4 : 5)
                .hand(0)
                .cursorX(isWest ? 0.0f : 1.0f)
                .cursorY(0.5f)
                .cursorZ(0.5f)
                .build());
        connection.sendPacket(new AnimationPacket(0));
        System.out.println("place " + currentX + " " + ((int) feetY - 1) + " " + currentZ + " " + (isWest ? 0.0f : 1.0f));
    }

    private void next() {
        currentX = nextX;
        currentZ = nextZ;

        if ((nextZ == LAST_Z && isOdd()) || (nextZ == FIRST_Z && !isOdd())) {
           nextX += 2;
        } else if (isOdd()) {
            if (nextX % 2 == 1) {
                nextX--;
            } else {
                nextX++;
            }
            nextZ++;
        } else {
            if (nextX % 2 == 0) {
                nextX++;
            } else {
                nextX--;
            }
            nextZ--;
        }

        if (nextX > LAST_X) {
            changeBuilderStatus(BuilderStatusV2.DISABLED);
        }
    }

    private boolean isOdd() {
        return currentX % 4 < 2;
    }

    private int checkEq() {
        System.out.println("eq");
        if (!inventory[27].isPresent()) {
            for (int i = 0; i < 36; i++) {
                if (inventory[i].isPresent()) {
//                    if (inventory[i].getData()[1] != 5) {
//                        connection.sendPacket(new ClickWindowPacket(
//                                0, i + 9, 1, actionCounter, 4, new byte[]{(byte) 255, (byte) 255}
//                        ));
//                        System.out.println("throw");
//                    }
                    System.out.println(" non empty " + Arrays.toString(inventory[i].getData()));
                    connection.sendPacket(new ClickWindowPacket(
                            0, i + 9, 0, actionCounter, 0, inventory[i].getData()
                    ));
                    actionCounter++;
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
            System.out.println("Skonczyly sie nici :(");
            return -1;
        } else {
            return 1;
        }
    }

    private void stop() {
        changeBuilderStatus(BuilderStatusV2.DISABLED);
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
