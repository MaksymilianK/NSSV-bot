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
import java.util.Queue;

public class BuilderSession extends MovableSession {

    private final int Y = 49;
    private final int FIRST_X = 8625;
    private final int FIRST_Z = 8727;
    private final int LAST_X = 8817;
    private final int LAST_Z = 8919;
    private final float YAW_ODD = 26.4f;
    private final float PITCH = 59.6f;
    private final float YAW_EVEN = 152.7f;
    private final float YAW_FIRST_LAST = 90.0f;
    private final float PITCH_FIRST_LAST = 82.5f;
    private final float YAW_HOLES = 90.0f;
    private final float PITCH_HOLES = 57.5f;

    private final float YAW_CHEST = -67.2f;
    private final float PITCH_CHEST = 43.0f;

    private final float YAW_SAND_EVEN = -90.0f;
    private final float PITCH_SAND = 56.5f;
    private final float YAW_SAND_ODD = 90.0f;
    private final float PITCH_SAND_LONG = 28.6f;

    private BuilderStatus builderStatus = BuilderStatus.DISABLED;
    private int currentX;
    private int currentZ;
    private int cacheX;
    private int cacheZ;
    private Queue<Integer> cancelledX = new ArrayDeque<>();
    private Queue<Integer> cancelledZ = new ArrayDeque<>();
    private Slot[] inventory = new Slot[36];
    private int actionCounter = 1;

    public BuilderSession(ConnectionManager connection, Timer timer) {
        super(connection, timer);
        for (int i = 0; i < 36; i++) {
            inventory[i] = new Slot(new byte[] {});
        }
    }

    @Override
    protected void onPacket(Packet packet) {
        super.onPacket(packet);

        if (packet.getName().equals(PacketName.BLOCK_CHANGE)) {
            onBlockChange((BlockChangePacket) packet);
        }
    }

    private void onBlockChange(BlockChangePacket packet) {
        if (builderStatus.equals(BuilderStatus.DISABLED) || packet.getStateID() != 0) {
            return;
        }

        if ((builderStatus.equals(BuilderStatus.BUILDING_SLABS) || builderStatus.equals(BuilderStatus.MOVING)) &&
                packet.getPosition().getY() == Y) {
            cancelledX.add(packet.getPosition().getX());
            cancelledZ.add(packet.getPosition().getZ());
        } else if ((builderStatus.equals(BuilderStatus.PLACING_SAND) || builderStatus.equals(BuilderStatus.MOVING_SAND)) &&
                packet.getPosition().getY() == Y + 1) {
            cancelledX.add(packet.getPosition().getX());
            cancelledZ.add(packet.getPosition().getZ());
        }
    }

    @Override
    protected void setUpTimer() {
        super.setUpTimer();
        timer.setDuration("tpDelay", Duration.ofMillis(1500));
        timer.setDuration("builderActionDelay", Duration.ofMillis(1000));
        timer.setTimeToNow("nextPossibleBuilderAction");
    }

    @Override
    protected void onChatMessage(ChatMessageClientboundPacket packet) {
        super.onChatMessage(packet);
        String message = ChatFormatter.getPureText(packet.getMessage().getComponents());

        if (builderStatus.equals(BuilderStatus.BUYING) && isNsMessage(packet.getMessage())) {
            if (message.endsWith("wysprzedany.")) {
                nextChest();
                moveToChest();
            } else if (message.contains("w ekwipunku.")) {
                builderStatus = BuilderStatus.TP_TO_WORK;
            }
        }

        if (!message.endsWith("-")) {
            return;
        }

        if (message.endsWith("--slabs--")) {
            int[] coords = extractCoords(message);
            if (coords.length > 0) {
                startBuildingSlabs(coords[0], coords[1]);
            } else {
                startBuildingSlabs();
            }
        } else if (message.endsWith("--holes--")) {
            int[] coords = extractCoords(message);
            if (coords.length > 0) {
                startDiggingHoles(coords[0], coords[1]);
            } else {
                startDiggingHoles();
            }
        } else if (message.endsWith("--sand--")) {
            int[] coords = extractCoords(message);
            if (coords.length > 0) {
                startPlacingSand(coords[0], coords[1]);
            } else {
                startPlacingSand();
            }
        } else if (message.endsWith("--chest--")) {
            currentX = 8646;
            currentZ = 8383;
            moveToChest();
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

    @Override
    protected void onPlayerPositionAndLook(PlayerPositionAndLookClientboundPacket packet) {
        super.onPlayerPositionAndLook(packet);

        if (builderStatus.equals(BuilderStatus.TP_TO_CHESTS) && packet.getX() == 8645.5d && packet.getZ() == 8382.5d) {
            cancelledX.add(currentX);
            cancelledZ.add(currentZ);
            currentX = 8646;
            currentZ = 8383;
            moveToChest();
        } else if (builderStatus.equals(BuilderStatus.TP_TO_WORK) && currentX != 8645.5d) {
            currentX = cancelledX.poll();
            currentZ = cancelledZ.poll();
            moveToSand();
        }
    }

    @Override
    protected void onConfirmTransaction(ConfirmTransactionClientboundPacket packet) {
        super.onConfirmTransaction(packet);
    }

    @Override
    protected void onEveryCheck() {
        super.onEveryCheck();

        if (builderStatus.equals(BuilderStatus.DISABLED) || !timer.isNowAfter("nextPossibleBuilderAction")) {
            return;
        }

        if (builderStatus.equals(BuilderStatus.MOVING) && !isMoving()) {
            builderStatus = BuilderStatus.BUILDING_SLABS;
            placeSlab();
        } else if (builderStatus.equals(BuilderStatus.BUILDING_SLABS)) {
            placeSlab();
        } else if (builderStatus.equals(BuilderStatus.MOVING_HOLES) && !isMoving()) {
            builderStatus = BuilderStatus.DIGGING_HOLES;
            digHole();
        } else if (builderStatus.equals(BuilderStatus.DIGGING_HOLES)) {
            digHole();
        } else if (builderStatus.equals(BuilderStatus.MOVING_SAND) && !isMoving()) {
            builderStatus = BuilderStatus.PLACING_SAND;
            placeSand();
        } else if (builderStatus.equals(BuilderStatus.PLACING_SAND)) {
            placeSand();
        } else if (builderStatus.equals(BuilderStatus.TP_TO_CHESTS)) {
            connection.sendPacket(new ChatMessageServerboundPacket("/p home geniuszmistrz 1"));
            delayNextBuilderAction();
        } else if (builderStatus.equals(BuilderStatus.MOVING_CHEST) && !isMoving()) {
            builderStatus = BuilderStatus.BUYING;
            buy();
        } else if (builderStatus.equals(BuilderStatus.BUYING)) {
            buy();
        } else if (builderStatus.equals(BuilderStatus.TP_TO_WORK)) {
            connection.sendPacket(new ChatMessageServerboundPacket("/home"));
            delayNextBuilderAction();
        }
    }

    private void buy() {
        connection.sendPacket(PlayerBlockPlacementPacket.builder()
                .cursorX(0.5f)
                .cursorY(0.5f)
                .cursorZ(0.5f)
                .face(2)
                .hand(0)
                .x(currentX)
                .y(65)
                .z(currentZ - 1)
                .build());
    }

    private void moveToChest() {
        builderStatus = BuilderStatus.MOVING_CHEST;
        setNewDestination(currentX - 0.5d, currentZ - 0.5d, YAW_CHEST, PITCH_CHEST);
    }

    private void nextChest() {
        currentZ += 2;
    }

    //SLABS

    private void startBuildingSlabs() {
        startBuildingSlabs(FIRST_X, FIRST_Z);
    }

    private void startBuildingSlabs(int firstX, int firstZ) {
        connection.sendPacket(new HeldItemChangePacket((short) 0));
        currentX = firstX;
        currentZ = firstZ;
        cacheX = 0;
        cacheZ = 0;
        moveToSlab();
    }

    private void moveToSlab() {
        builderStatus = BuilderStatus.MOVING;
        if ((currentZ == LAST_Z && !isSlabOdd()) || (currentZ == FIRST_Z && isSlabOdd())) {
            setNewDestination((double) currentX + 0.25d, (double) currentZ + 0.5d, YAW_FIRST_LAST, PITCH_FIRST_LAST);
        } else if (isSlabOdd()) {
            setNewDestination((double) currentX + 0.5d, (double) currentZ - 0.5d, YAW_ODD, PITCH);
        } else {
            setNewDestination((double) currentX + 0.5d, (double) currentZ + 1.5d, YAW_EVEN, PITCH);
        }
    }

    private void placeSlab() {
        if (!inventory[27].isPresent()) {
            for (int i = 0; i < 36; i++) {
                if (inventory[i].isPresent()) {
                    connection.sendPacket(new ClickWindowPacket(
                            0, i + 9, 0, actionCounter, 1, inventory[i].getData()
                    ));
                    actionCounter++;
                    // delayNextBuilderAction();
                    return;
                }
            }
            builderStatus = BuilderStatus.DISABLED;
            System.out.println("Skonczyly sie plytki :(");
            return;
        }

        connection.sendPacket(new AnimationPacket(0));
        connection.sendPacket(PlayerBlockPlacementPacket.builder()
                .x(currentX - 1)
                .y((int) feetY - 1)
                .z(currentZ)
                .face(5)
                .hand(0)
                .cursorX(1.0f)
                .cursorY(0.75f)
                .cursorZ(0.5f)
                .build());
        nextSlab();
        moveToSlab();
    }

    private void nextSlab() {
        if (cacheX != 0) {
            currentX = cacheX;
            currentZ = cacheZ;
            cacheX = 0;
            cacheZ = 0;
        }

        if (cancelledX.isEmpty()) {
            if ((currentZ == LAST_Z && isSlabOdd()) || (currentZ == FIRST_Z && !isSlabOdd())) {
                currentX++;
            } else if (isSlabOdd()) {
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


    //SLABS_END; HOLES

    private void startDiggingHoles() {
        startDiggingHoles(FIRST_X, FIRST_Z);
    }

    private void startDiggingHoles(int firstX, int firstZ) {
        connection.sendPacket(new HeldItemChangePacket((short) 0));
        currentX = firstX;
        currentZ = firstZ;
        cacheX = 0;
        cacheZ = 0;
        moveToHole();
    }

    private void moveToHole() {
        builderStatus = BuilderStatus.MOVING_HOLES;
        if (currentX == LAST_X) {
            setNewDestination(currentX - 0.5d, currentZ + 0.5d, -YAW_HOLES, PITCH_HOLES);
        } else {
            setNewDestination(currentX + 1.5d, currentZ + 0.5d, YAW_HOLES, PITCH_HOLES);
        }
    }

    private void digHole() {
        connection.sendPacket(new PlayerDiggingPacket(0, new Position(currentX, Y, currentZ), 1));
        connection.sendPacket(new AnimationPacket(0));
        connection.sendPacket(new PlayerDiggingPacket(2, new Position(currentX, Y, currentZ), 1));
        nextHole();
        moveToHole();
    }

    private void nextHole() {
        if ((currentZ == LAST_Z && isHoleOdd()) || (currentZ == FIRST_Z && !isHoleOdd())) {
            currentX += 4;
        } else if (isHoleOdd()) {
            currentZ += 4;
        } else {
            currentZ -= 4;
        }
    }

    private boolean isHoleOdd() {
        return currentX % 8 == 1;
    }

    //HOLES_END; SAND

    private void startPlacingSand(int firstX, int firstZ) {
        connection.sendPacket(new HeldItemChangePacket((short) 0));
        currentX = firstX;
        currentZ = firstZ;
        moveToSand();
    }

    private void startPlacingSand() {
        startPlacingSand(FIRST_X + 1, FIRST_Z + 1);
    }

    private void moveToSand() {
        builderStatus = BuilderStatus.MOVING_SAND;

        if (isSandOdd()) {
            setNewDestination(currentX + 1.5d, currentZ + 0.5d, YAW_SAND_ODD, PITCH_SAND);
        } else {
            setNewDestination(currentX - 0.5d, currentZ + 0.5d, YAW_SAND_EVEN, PITCH_SAND);
        }
    }

    private void moveToSandLast() {
        builderStatus = BuilderStatus.MOVING_SAND;
        setNewDestination(currentX + 3.5d, currentZ + 0.5d, YAW_SAND_ODD, PITCH_SAND_LONG);
    }

    private void nextSand() {
        if (cacheX != 0) {
            currentX = cacheX;
            currentZ = cacheZ;
            cacheX = 0;
            cacheZ = 0;
        }

        if (cancelledX.isEmpty()) {
            if ((currentZ == LAST_Z - 1 && isSandOdd()) || (currentZ == FIRST_Z + 1 && !isSandOdd())) {
                currentX += 2;
            } else if (isSandOdd()) {
                currentZ += 2;
            } else {
                currentZ -= 2;
            }
        } else {
            cacheX = currentX;
            cacheZ = currentZ;
            currentX = cancelledX.poll();
            currentZ = cancelledZ.poll();
        }
    }

    private void placeSand() {
        int eq = checkEq();

        if (eq == -1) {
            builderStatus = BuilderStatus.TP_TO_CHESTS;
            connection.sendPacket(new ChatMessageServerboundPacket("/sethome"));
            timer.setTimeFromNow("nextPossibleBuilderAction", Duration.ofSeconds(2));
            return;
        } else if (eq == 0) {
            return;
        }

        if (currentZ == FIRST_Z + 1 && !isSandOdd() && x < currentX) {
            moveToSandLast();
            return;
        }

        connection.sendPacket(new AnimationPacket(0));
        connection.sendPacket(PlayerBlockPlacementPacket.builder()
                .x(currentX)
                .y(Y)
                .z(currentZ)
                .face(1)
                .hand(0)
                .cursorX(0.5f)
                .cursorY(1.0f)
                .cursorZ(0.5f)
                .build());
        nextSand();
        moveToSand();
    }

    private boolean isSandOdd() {
        return currentX % 4 != 0;
    }

    //SAND_END

    private int checkEq() {
        if (!inventory[27].isPresent()) {
            for (int i = 0; i < 36; i++) {
                if (inventory[i].isPresent()) {
                    connection.sendPacket(new ClickWindowPacket(
                            0, i + 9, 0, actionCounter, 1, inventory[i].getData()
                    ));
                    actionCounter++;
                    return 0;
                }
            }
            builderStatus = BuilderStatus.DISABLED;
            System.out.println("Skonczyly sie plytki :(");
            return -1;
        } else {
            return 1;
        }
    }

    private void stop() {
        builderStatus = BuilderStatus.DISABLED;
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

    private void delayNextBuilderAction() {
        timer.setTimeFromNow("nextPossibleBuilderAction", "builderActionDelay");
    }
}
