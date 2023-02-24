package pl.konradmaksymilian.nssvbot.session.v2;

import pl.konradmaksymilian.nssvbot.connection.ConnectionManager;
import pl.konradmaksymilian.nssvbot.protocol.Position;
import pl.konradmaksymilian.nssvbot.protocol.State;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.*;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.*;
import pl.konradmaksymilian.nssvbot.session.MovableSession;
import pl.konradmaksymilian.nssvbot.session.SlabBuilderStatus;
import pl.konradmaksymilian.nssvbot.session.Slot;
import pl.konradmaksymilian.nssvbot.session.Status;
import pl.konradmaksymilian.nssvbot.utils.ChatFormatter;
import pl.konradmaksymilian.nssvbot.utils.Timer;

import java.time.Duration;
import java.util.Arrays;

public class SandBuilderSessionV2 extends MovableSession {

    private final int FIRST_X = 28736;
    private final int FIRST_Z = -7776;
    private final int LAST_X = 28895;
    private final int LAST_Z = -7617;

    private final int CHEST_X = 28475;
    private final int CHEST_Y = 1;
    private final int CHEST_Z = -5485;

    private BuilderStatusV2 builderStatus = BuilderStatusV2.DISABLED;
    private int currentX;
    private int currentZ;

    private final Slot[] inventory = new Slot[36];
    private int actionCounter = 1;

    public SandBuilderSessionV2(ConnectionManager connection, Timer timer) {
        super(connection, timer);
        for (int i = 0; i < 36; i++) {
            inventory[i] = new Slot(new byte[] {});
        }
        timer.setTimeToNow("nextPossibleBuy");
        timer.setTimeFromNow("checkHand", Duration.ofSeconds(Integer.MAX_VALUE));
    }

    @Override
    protected void onLoginSuccess() {
        super.onLoginSuccess();
        builderStatus = BuilderStatusV2.DISABLED;
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
        if (builderStatus.equals(BuilderStatusV2.DISABLED) || packet.getStateID() == 0) {
            return;
        }

        if (builderStatus.equals(BuilderStatusV2.BUILDING) &&
                packet.getPosition().getY() == (int) feetY - 1 && packet.getPosition().getX() == currentX && packet.getPosition().getZ() == currentZ) {
            System.out.println("block change " + packet.getStateID());
            timer.setTimeFromNow("checkHand", Duration.ofSeconds(Integer.MAX_VALUE));
            nextSand();
            moveToSand();
        }
    }

    @Override
    protected void onChatMessage(ChatMessageClientboundPacket packet) {
        super.onChatMessage(packet);
        String message = ChatFormatter.getPureText(packet.getMessage().getComponents());

        if (builderStatus.equals(BuilderStatusV2.BUYING)) {
            if (message.endsWith("wysprzedany.")) {
                changeBuilderStatusV2(BuilderStatusV2.TP_TO_WORK);
            } else if (message.contains("w ekwipunku.")) {
                changeBuilderStatusV2(BuilderStatusV2.TP_TO_WORK);
            }
        }

        if (!message.endsWith("-")) {
            return;
        }

        if (message.endsWith("--start--")) {
            int[] coords = extractCoords(message);
            if (coords.length > 0) {
                startBuildingSands(coords[0], coords[1]);
            } else {
                startBuildingSands();
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
            moveToSand();
        }
    }

    @Override
    protected void onEveryCheck() {
        super.onEveryCheck();

        if (builderStatus.equals(BuilderStatusV2.DISABLED)) {
            return;
        }

        if (builderStatus.equals(BuilderStatusV2.MOVING) && !isMoving() && timer.isNowAfter("nextPossibleMove")) {
            changeBuilderStatusV2(BuilderStatusV2.BUILDING);
        } else if (builderStatus.equals(BuilderStatusV2.MOVING_CHEST) && !isMoving()) {
            changeBuilderStatusV2(BuilderStatusV2.BUYING);
        } else if (builderStatus.equals(BuilderStatusV2.BUYING)) {
            buy();
        } else if (builderStatus.equals(BuilderStatusV2.BUILDING)) {
            if (timer.isNowAfter("checkHand")) {
                timer.setTimeFromNow("checkHand", Duration.ofSeconds(Integer.MAX_VALUE));
                placeSand();
            }
        } else if (builderStatus.equals(BuilderStatusV2.MOVING_TP) && !isMoving()) {
            changeBuilderStatusV2(BuilderStatusV2.TP_TO_CHESTS);
        }
    }

    private void onChangeBuilderStatusV2() {
        if (builderStatus.equals(BuilderStatusV2.BUILDING)) {
            placeSand();
        } else if (builderStatus.equals(BuilderStatusV2.TP_TO_CHESTS)) {
            connection.sendPacket(new ChatMessageServerboundPacket("/sethome"));
            connection.sendPacket(new ChatMessageServerboundPacket("/p visit GeniuszMistrz 2"));
            System.out.println("sethome");
        } else if (builderStatus.equals(BuilderStatusV2.TP_TO_WORK)) {
            connection.sendPacket(new ChatMessageServerboundPacket("/home"));
        } else if (builderStatus.equals(BuilderStatusV2.MOVING_TP)) {
            int prevX = currentX;
            int prevZ = currentZ;
            if ((currentZ == LAST_Z && !isSandOdd()) || (currentZ == FIRST_Z && isSandOdd())) {
                prevX = currentX - 1;
            } else if (!isSandOdd()) {
                prevZ = currentZ - 1;
            } else {
                prevZ = currentZ + 1;
            }

            setNewDestination(prevX + 0.5, prevZ + 0.5, 80, 90);
        }
    }

    private void changeBuilderStatusV2(BuilderStatusV2 builderStatus) {
        this.builderStatus = builderStatus;
        onChangeBuilderStatusV2();
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
        changeBuilderStatusV2(BuilderStatusV2.MOVING_CHEST);
        setNewDestination(x, z, getYaw(x, z, CHEST_X, CHEST_Z), getPitch(x, z, CHEST_X, CHEST_Y, CHEST_Z));
    }

    private void startBuildingSands() {
        startBuildingSands(FIRST_X, FIRST_Z);
    }

    private void startBuildingSands(int firstX, int firstZ) {
        currentX = firstX;
        currentZ = firstZ;
        moveToSand();
    }

    private void moveToSand() {
        changeBuilderStatusV2(BuilderStatusV2.MOVING);
        double newX, newZ;
        float newYaw, newPitch;

        if (isSandOdd()) {
            if (currentZ == LAST_Z) {
                newX = currentX + 0.2d;
                newZ = currentZ + 0.5d;
                newYaw = 89.9f;
                newPitch = 84.1f;
            } else {
                newX = currentX + 0.5d;
                newZ = currentZ + 0.8d;
                newYaw = 0.1f;
                newPitch = 84.0f;
            }
        } else {
            if (currentZ == FIRST_Z) {
                newX = currentX + 0.2d;
                newZ = currentZ + 0.5d;
                newYaw = 89.9f;
                newPitch = 84.5f;
            } else {
                newX = currentX + 0.5d;
                newZ = currentZ + 0.2d;
                newYaw = 179.9f;
                newPitch = 83.9f;
            }
        }
        setNewDestination(newX, newZ, newYaw, newPitch);
        System.out.println("current " + currentX + " " + currentZ + " " + newZ);
    }

    private void placeSand() {
        int eq = checkEq();

        if (eq == -1) {
            changeBuilderStatusV2(BuilderStatusV2.MOVING_TP);
            return;
        } else if (eq == 0) {
            changeBuilderStatusV2(BuilderStatusV2.MOVING);
            return;
        } else {
            timer.setTimeFromNow("checkHand", Duration.ofSeconds(1));
        }

        if ((currentZ == FIRST_Z && !isSandOdd()) || (currentZ == LAST_Z && isSandOdd())) {
            connection.sendPacket(PlayerBlockPlacementPacket.builder()
                    .x(currentX - 1)
                    .y((int) feetY - 1)
                    .z(currentZ)
                    .face(5)
                    .hand(0)
                    .cursorX(1.0f)
                    .cursorY(0.875f)
                    .cursorZ(0.5f)
                    .build());
            connection.sendPacket(new AnimationPacket(0));
            return;
        }

        connection.sendPacket(PlayerBlockPlacementPacket.builder()
                .x(currentX)
                .y((int) feetY - 1)
                .z(isSandOdd() ? currentZ + 1 : currentZ - 1)
                .face(isSandOdd() ? 2 : 3)
                .hand(0)
                .cursorX(0.5f)
                .cursorY(0.875f)
                .cursorZ(isSandOdd() ? 0.0f : 1.0f)
                .build());
        connection.sendPacket(new AnimationPacket(0));
    }

    private void nextSand() {
            if ((currentZ == LAST_Z && !isSandOdd()) || (currentZ == FIRST_Z && isSandOdd())) {
                currentX++;
            } else if (!isSandOdd()) {
                currentZ++;
            } else {
                currentZ--;
            }

            if (currentX > LAST_X) {
                connection.disconnect();
            }
    }

    private boolean isSandOdd() {
        return currentX % 2 == 1;
    }

    private int checkEq() {
        System.out.println("eq");
        if (!inventory[27].isPresent()) {
            for (int i = 0; i < 36; i++) {
                if (inventory[i].isPresent()) {
                    if (inventory[i].getData()[1] != 12) {
                        connection.sendPacket(new ClickWindowPacket(
                                0, i + 9, 1, actionCounter, 4, new byte[]{(byte) 255, (byte) 255}
                        ));
                        System.out.println("throw");
                    }
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
            System.out.println("Skonczyly sie plytki :(");
            return -1;
        } else {
            return 1;
        }
    }

    private void stop() {
        builderStatus = BuilderStatusV2.DISABLED;
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
}
