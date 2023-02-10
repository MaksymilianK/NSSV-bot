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

public class CobbleBuilderSession extends MovableSession {
    private static final int FIRST_X = 283399;
    private static final int FIRST_Z = -5568;
    private static final int LAST_X = FIRST_X + 160;
    private static final int LAST_Z = FIRST_Z + 160;
    private static final int[] COORDS_X = new int[640];
    private static final int[] COORDS_Z = new int[640];

    static {
        COORDS_X[0] = FIRST_X;
        COORDS_Z[0] = FIRST_Z;

        for (int i = 1; i < 161; i++) {
            COORDS_X[i] = COORDS_X[i - 1];
            COORDS_Z[i] = COORDS_Z[i - 1] - 1;
        }
        for (int i = 161; i < 321; i++) {
            COORDS_X[i] = COORDS_X[i - 1] + 1;
            COORDS_Z[i] = COORDS_Z[i - 1];
        }
        for (int i = 321; i < 481; i++) {
            COORDS_X[i] = COORDS_X[i - 1];
            COORDS_Z[i] = COORDS_Z[i - 1] + 1;
        }
        for (int i = 481; i < 640; i++) {
            COORDS_X[i] = COORDS_X[i - 1] - 1;
            COORDS_Z[i] = COORDS_Z[i - 1];
        }
    }

    private CobbleBuilderStatus cobbleBuilderStatus = CobbleBuilderStatus.STOPPED;

    private int heldItemSlot = 0;

    private int current;
    private int next;

    private Slot[] inventory;

    public CobbleBuilderSession(ConnectionManager connection, Timer timer) {
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
        } else if (packet.getName().equals(PacketName.HELD_ITEM_CHANGE_CLIENTBOUNT)) {
            onHeldItemChange((HeldItemChangeClientboundPacket) packet);
        }
    }

    private void onHeldItemChange(HeldItemChangeClientboundPacket packet) {
        if (!status.equals(Status.GAME)) {
            return;
        }

        heldItemSlot = packet.getSlot();
    }

    protected void onCobbleBuilderStatusChange() {
        if (!status.equals(Status.GAME)) {
            return;
        }

        if (cobbleBuilderStatus.equals(CobbleBuilderStatus.TP_TO_BUILD)) {
            connection.sendPacket(new ChatMessageServerboundPacket("/home"));
        } else if (cobbleBuilderStatus.equals(CobbleBuilderStatus.BUILDING)) {
            int currentX = (int) Math.floor(x);
            int currentZ = (int) Math.floor(z);
            current = -1;
            for (int i = 0; i < 640; i++) {
                if (COORDS_X[i] == currentX && COORDS_Z[i] == currentZ) {
                    current = i;
                    next = (i + 1) % 640;
                    break;
                }
            }
            if (current == -1) {
                throw new RuntimeException("Cannot find current block");
            }

            setNewDestination(COORDS_X[next] + 0.5, COORDS_Z[next] + 0.5,
                    getYaw(x, z, COORDS_X[next] + 0.5f, COORDS_Z[next] + 0.5f),
                    getPitch(x, z, COORDS_X[next] + 0.5f, (float) feetY, COORDS_Z[next] + 0.5f));
        }
    }

    @Override
    protected void onEveryCheck() {
        super.onEveryCheck();

        if (!status.equals(Status.GAME) || cobbleBuilderStatus.equals(CobbleBuilderStatus.STOPPED)) {
            return;
        }

        if (cobbleBuilderStatus.equals(BuilderStatus.MOVING) && !isMoving()) {
            connection.sendPacket(PlayerBlockPlacementPacket.builder()
                    .x(COORDS_X[current])
                    .y((int) feetY - 1)
                    .z(COORDS_Z[current])
                    .face(1)
                    .hand(0)
                    .cursorX(0.5f)
                    .cursorY(1.0f)
                    .cursorZ(0.5f)
                    .build());
            connection.sendPacket(new AnimationPacket(0));
        }
    }

    private void onBlockChange(BlockChangePacket packet) {
        if (!status.equals(Status.GAME) || !cobbleBuilderStatus.equals(CobbleBuilderStatus.BUILDING)) {
            return;
        }

        if (packet.getPosition().getX() != COORDS_X[current] || packet.getPosition().getY() != (int) feetY ||
                packet.getPosition().getZ() != COORDS_Z[current]) {
            return;
        }

        if (packet.getStateID() == )
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

        if (!status.equals(Status.GAME)) {
            return;
        }

        String message = ChatFormatter.getPureText(packet.getMessage().getComponents());
        if (!message.endsWith("-")) {
            return;
        }

        if (message.endsWith("--start--")) {
            changeCobbleBuilderStatus(CobbleBuilderStatus.TP_TO_BUILD);
        } else if (message.endsWith("--stop--")) {
            connection.sendPacket(new ChatMessageServerboundPacket("/sethome"));
            move = null;
            changeCobbleBuilderStatus(CobbleBuilderStatus.STOPPED);
        }
    }

    private void changeCobbleBuilderStatus(CobbleBuilderStatus cobbleBuilderStatus) {
        this.cobbleBuilderStatus = cobbleBuilderStatus;
        onCobbleBuilderStatusChange();
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
        if (!status.equals(Status.GAME)) {
            return;
        }

        if (cobbleBuilderStatus.equals(CobbleBuilderStatus.TP_TO_BUILD)) {
            changeCobbleBuilderStatus(CobbleBuilderStatus.BUILDING);
        } else if (builderStatus.equals(BuilderStatus.TP_TO_WORK) && currentX != 8645.5d) {
            currentX = cancelledX.poll();
            currentZ = cancelledZ.poll();
            moveToSand();
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
        connection.sendPacket(new HeldItemChangeServerboundPacket((short) 0));
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
        connection.sendPacket(new HeldItemChangeServerboundPacket((short) 0));
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
        connection.sendPacket(new HeldItemChangeServerboundPacket((short) 0));
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
