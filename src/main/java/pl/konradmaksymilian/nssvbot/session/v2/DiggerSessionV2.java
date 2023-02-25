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

public class DiggerSessionV2 extends MovableSession {

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
    private int checkEqCounter = 0;

    public DiggerSessionV2(ConnectionManager connection, Timer timer) {
        super(connection, timer);
        for (int i = 0; i < 36; i++) {
            inventory[i] = new Slot(new byte[] {});
        }
        timer.setTimeToNow("nextPossibleBuy");
        timer.setTimeFromNow("checkHand", Duration.ofSeconds(Integer.MAX_VALUE));
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
        System.out.println("set slot " + packet.getSlot() + " " + Arrays.toString(inventory[packet.getSlot() - 9].getData()));
    }

    protected void onWindowItems(WindowItemsPacket packet) {
        if (!status.equals(Status.GAME) || packet.getWindowId() != 0) {
            return;
        }
        for (int i = 9; i < 45; i++) {
            inventory[i - 9].setData(packet.getSlotData()[i]);
            System.out.println("windows " + i + " " + Arrays.toString(inventory[i - 9].getData()));
            if (builderStatus.equals(BuilderStatusV2.BUILDING) && inventory[27].getCount() == 0) {
                System.out.println("empty!!!!!!!!!!!!");
            }
        }
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
        if (builderStatus.equals(BuilderStatusV2.DISABLED) || packet.getStateID() != 0) {
            return;
        }

        if (builderStatus.equals(BuilderStatusV2.BUILDING) &&
                packet.getPosition().getY() == (int) feetY - 1 && packet.getPosition().getX() == currentX && packet.getPosition().getZ() == currentZ) {
            timer.setTimeFromNow("checkHand", Duration.ofSeconds(Integer.MAX_VALUE));
            nextHole();
            moveToHole();
        }
    }

    @Override
    protected void onChatMessage(ChatMessageClientboundPacket packet) {
        super.onChatMessage(packet);
        String message = ChatFormatter.getPureText(packet.getMessage().getComponents());

        if (builderStatus.equals(BuilderStatusV2.BUYING)) {
            if (message.endsWith("materialow na sprzedaz.")) {
                changeDiggerStatus(BuilderStatusV2.TP_TO_WORK);
            } else if (message.contains("w skrzynce.")) {
                changeDiggerStatus(BuilderStatusV2.TP_TO_WORK);
            }
        }

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
            changeDiggerStatus(BuilderStatusV2.DISABLED);
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
    protected void onEveryCheck() {
        super.onEveryCheck();
        if (!status.equals(Status.GAME) || builderStatus.equals(BuilderStatusV2.DISABLED)) {
            return;
        }

        if (builderStatus.equals(BuilderStatusV2.MOVING) && !isMoving()) {
            changeDiggerStatus(BuilderStatusV2.BUILDING);
        } else if (builderStatus.equals(BuilderStatusV2.BUYING)) {
            buy();
        } else if (builderStatus.equals(BuilderStatusV2.MOVING_CHEST) && !isMoving()) {
            changeDiggerStatus(BuilderStatusV2.BUYING);
        } else if (builderStatus.equals(BuilderStatusV2.BUILDING)) {
            if (timer.isNowAfter("checkHand")) {
                timer.setTimeFromNow("checkHand", Duration.ofSeconds(Integer.MAX_VALUE));
                digHole();
            }
        }
    }

    private void moveToChest() {
        changeDiggerStatus(BuilderStatusV2.MOVING_CHEST);
        setNewDestination(x, z, getYaw(x, z, CHEST_X, CHEST_Z), getPitch(x, z, CHEST_X, CHEST_Y, CHEST_Z));
    }

    private void buy() {
        if (!timer.isNowAfter("nextPossibleBuy")) {
            return;
        }

        connection.sendPacket(new PlayerDiggingPacket(0, new Position(CHEST_X, CHEST_Y, CHEST_Z), 4));
        connection.sendPacket(new PlayerDiggingPacket(1, new Position(CHEST_X, CHEST_Y, CHEST_Z), 4));

        timer.setTimeFromNow("nextPossibleBuy", Duration.ofMillis(500));
    }

    private void onChangeDiggerStatus() {
        if (builderStatus.equals(BuilderStatusV2.BUILDING)) {
            digHole();
        } else if (builderStatus.equals(BuilderStatusV2.DISABLED)) {
            stop();
        } else if (builderStatus.equals(BuilderStatusV2.TP_TO_CHESTS)) {
            connection.sendPacket(new ChatMessageServerboundPacket("/sethome"));
            connection.sendPacket(new ChatMessageServerboundPacket("/p visit GeniuszMistrz 2"));
            System.out.println("sethome");
        } else if (builderStatus.equals(BuilderStatusV2.TP_TO_WORK)) {
            connection.sendPacket(new ChatMessageServerboundPacket("/home"));
        }
    }

    private void changeDiggerStatus(BuilderStatusV2 diggerStatus) {
        this.builderStatus = diggerStatus;
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
        changeDiggerStatus(BuilderStatusV2.MOVING);
        double newX, newZ;
        float newYaw, newPitch;

        if ((int) feetY % 4 != 0 && currentZ == LAST_Z && !isHoleOdd()) {
            newX = currentX + 1.2d;
            newZ = currentZ + 0.5d;
            newYaw = 90.0f;
            newPitch = 72.1f;
        } else if ((int) feetY % 4 != 0 && currentZ == FIRST_Z && isHoleOdd()) {
            newX = currentX + 1.2d;
            newZ = currentZ + 0.5d;
            newYaw = 90.1f;
            newPitch = 72.1f;
        } else if (isHoleOdd()) {
            newX = currentX + 0.5d;
            newZ = currentZ - 0.2d;
            newYaw = 0.1f;
            newPitch = 72.1f;
        } else {
            newX = currentX + 0.5d;
            newZ = currentZ + 0.8d;
            newYaw = 179.9f;
            newPitch = 72.0f;
        }
        setNewDestination(newX, newZ, newYaw, newPitch);
        System.out.println("current " + currentX + " " + currentZ + " " + newZ);
    }

    private void digHole() {
        checkEqCounter++;
        if (checkEqCounter >= 64) {
            checkEqCounter = 0;

            int eq = checkEq();
            if (eq == -1) {
                changeDiggerStatus(BuilderStatusV2.TP_TO_CHESTS);
                return;
            } else {
                timer.setTimeFromNow("checkHand", Duration.ofSeconds(1));
            }
        }

        connection.sendPacket(new PlayerDiggingPacket(0, new Position(currentX, (int) feetY - 1, currentZ), 1));
        connection.sendPacket(new AnimationPacket(0));
        connection.sendPacket(new PlayerDiggingPacket(2, new Position(currentX, (int) feetY - 1, currentZ), 1));
        connection.sendPacket(new AnimationPacket(0));
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
            moveToHole();
        }
    }

    private void nextHole() {
        if ((int) feetY % 4 == 0 && currentZ == LAST_Z - 1 && !isHoleOdd()) {
            currentX++;
            currentZ++;
        } else if ((int) feetY % 4 == 0 && currentZ == FIRST_Z + 1 && isHoleOdd()) {
            currentX++;
            currentZ--;
        } else if ((int) feetY % 4 != 0 && currentZ == LAST_Z && !isHoleOdd()) {
            currentX++;
            currentZ--;
        } else if ((int) feetY % 4 != 0 && currentZ == FIRST_Z && isHoleOdd()) {
            currentX++;
            currentZ++;
        } else if (!isHoleOdd()) {
            currentZ += 2;
        } else {
            currentZ -= 2;
        }

        if (currentX > LAST_X) {
            connection.disconnect();
        }
    }

    private boolean isHoleOdd() {
        return currentX % 2 == 1;
    }

    private void stop() {
        builderStatus = BuilderStatusV2.DISABLED;
        move = null;
    }

    private int checkEq() {
        for (int i = 0; i < 36; i++) {
            if (i == 27) {
                continue;
            }

            if (inventory[i].isPresent()) {
                if (inventory[i].getData()[1] != 12) {
                    connection.sendPacket(new ClickWindowPacket(
                            0, i + 9, 1, actionCounter++, 4, new byte[]{(byte) 255, (byte) 255}
                    ));
                    System.out.println("throw " + (i + 9) + " " + Arrays.toString(inventory[i].getData()));
                }
            }
        }

        if (inventory[26].isPresent()) {
            return -1;
        }
        return 0;
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
