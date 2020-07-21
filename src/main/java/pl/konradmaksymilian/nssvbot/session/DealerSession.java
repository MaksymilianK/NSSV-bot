package pl.konradmaksymilian.nssvbot.session;

import pl.konradmaksymilian.nssvbot.config.DealerConfig;
import pl.konradmaksymilian.nssvbot.connection.ConnectionManager;

import pl.konradmaksymilian.nssvbot.protocol.ChatMessage;
import pl.konradmaksymilian.nssvbot.protocol.Colour;
import pl.konradmaksymilian.nssvbot.protocol.Position;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.ChatMessageClientboundPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.PlayerPositionAndLookClientboundPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.EntityActionPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.PlayerBlockPlacementPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.PlayerDiggingPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.PlayerLookPacket;
import pl.konradmaksymilian.nssvbot.utils.ChatFormatter;
import pl.konradmaksymilian.nssvbot.utils.Timer;

import java.time.Duration;

public class DealerSession extends MovableSession {

    private static final int[] FIRST_CHEST_X = {8727, 8720, 8714, 8714, 8715, 8722, 8728, 8728};
    private static final int FIRST_CHEST_Y = 5;
    private static final int[] FIRST_CHEST_Z = {8830, 8830, 8829, 8822, 8816, 8816, 8817, 8824};
    private static final int[] DIRECTION_X = {-1, -1, 0, 0, 1, 1, 0, 0};
    private static final int[] DIRECTION_Z = {0, 0, -1, -1, 0, 0, 1, 1};

    private final DealerConfig config;

    private DealerStatus dealerStatus;
    private int sector;
    private int chest;
    private int itemsLeftInChest;
    private int itemsInEq;

    public DealerSession(ConnectionManager connection, Timer timer, DealerConfig config) {
        super(connection, timer);
        this.config = config;
        dealerStatus = DealerStatus.DISABLED;
    }

    @Override
    public void setUpTimer() {
        super.setUpTimer();
        timer.setDuration("tpDelay", Duration.ofMillis(1500));
        timer.setDuration("dealerActionDelay", Duration.ofMillis(100));
    }

    @Override
    protected void onPlayerPositionAndLook(PlayerPositionAndLookClientboundPacket packet) {
        super.onPlayerPositionAndLook(packet);
        if (dealerStatus.equals(DealerStatus.TP_TO_PLOT)) {
            changeDealerStatus(DealerStatus.AFTER_TP_TO_PLOT);
        } else if (dealerStatus.equals(DealerStatus.TP_TO_SHOP)) {
            changeDealerStatus(DealerStatus.AFTER_TP_TO_SHOP);
        }
    }

    @Override
    protected void onEveryCheck() {
        super.onEveryCheck();
        checkDealerStatus();
    }

    @Override
    protected void onChatMessage(ChatMessageClientboundPacket packet) {
        super.onChatMessage(packet);
        String message = ChatFormatter.getPureText(packet.getMessage().getComponents());

        if (isNsMessage(packet.getMessage())) {
            if (message.endsWith("obronic!")) {
                connection.sendPacket(new EntityActionPacket(playerEid, 0));
                connection.sendPacket(new EntityActionPacket(playerEid, 1));
            } else if (dealerStatus.equals(DealerStatus.BUYING)) {
                if (message.endsWith("wysprzedany.")) {
                    onChestEmpty();
                } else if (message.contains(" Kupiles")) {
                    onBuyingMessage(message);
                }
            } else if (dealerStatus.equals(DealerStatus.SELLING) && message.contains(" Sprzedales")) {
                changeDealerStatus(DealerStatus.TP_TO_PLOT);
                itemsInEq = 0;
            } else if (message.endsWith("w ekwipunku.")) {
                changeDealerStatus(DealerStatus.TP_TO_SHOP);
            }
        }

        if (message.endsWith("--start--")) {
            startDealing();
        } else if (message.endsWith("--stop--")) {
            stopDealing();
        } else if (message.endsWith("--tpa--")) {
            sendChatMessage("/tpa geniusz");
        }
    }

    private boolean isNsMessage(ChatMessage message) {
        return message.getComponents().size() > 4
                && message.getComponents().get(1).getStyle().getColour().isPresent()
                && message.getComponents().get(1).getStyle().getColour().get().equals(Colour.GOLD.getName())
                && message.getComponents().get(1).getText().equals("N")
                && message.getComponents().get(2).getStyle().getColour().isPresent()
                && message.getComponents().get(2).getStyle().getColour().get().equals(Colour.YELLOW.getName())
                && message.getComponents().get(2).getText().equals("S");
    }

    private void startDealing() {
        sector = 0;
        chest = 0;
        itemsLeftInChest = 3456;
        itemsInEq = 0;
        timer.setTimeToNow("nextPossibleTp");
        timer.setTimeToNow("nextPossibleDealerAction");
        changeDealerStatus(DealerStatus.TP_TO_PLOT);
    }

    private void checkDealerStatus() {
        if (dealerStatus.equals(DealerStatus.DISABLED) || !timer.isNowAfter("nextPossibleDealerAction")) {
            return;
        }

        if (dealerStatus.equals(DealerStatus.AFTER_TP_TO_PLOT)) {
            dealerStatus = DealerStatus.MOVING_ON_PLOT;
            moveToChest();
        } else if (dealerStatus.equals(DealerStatus.MOVING_ON_PLOT) && !isMoving()) {
            changeDealerStatus(DealerStatus.BUYING);
            connection.sendPacket(new PlayerLookPacket(yaw, pitch, true));
        } else if (dealerStatus.equals(DealerStatus.BUYING)) {
            buy();
            delayNextDealingAction();
        } else if (dealerStatus.equals(DealerStatus.TP_TO_PLOT) && timer.isNowAfter("nextPossibleTp")) {
            tpToPlot();
        } else if (dealerStatus.equals(DealerStatus.TP_TO_SHOP) && timer.isNowAfter("nextPossibleTp")) {
            tpToShop();
        } else if (dealerStatus.equals(DealerStatus.AFTER_TP_TO_SHOP)) {
            changeDealerStatus(DealerStatus.SELLING);
        } else if (dealerStatus.equals(DealerStatus.SELLING)) {
            delayNextDealingAction();
            sell();
        }
    }

    private void changeDealerStatus(DealerStatus status) {
        dealerStatus = status;
        delayNextDealingAction();
    }

    private void sell() {
        connection.sendPacket(new EntityActionPacket(playerEid, 0));
        connection.sendPacket(new PlayerDiggingPacket(0, new Position(-50, 192, -150), 4));
        connection.sendPacket(new PlayerDiggingPacket(1, new Position(-50, 192, -150), 4));
        connection.sendPacket(new EntityActionPacket(playerEid, 1));
    }

    private float getYaw(double x0, double z0, float x, float z) {
        double dx = x + 0.5f - x0;
        double dz = z + 0.5f - z0;

        if (dx < 0) {
            return (float) ((Math.atan(dz / dx) + Math.PI * 5f / 2f) / Math.PI * 180);

        } else {
            return (float) ((Math.atan(dz / dx) + Math.PI * 3f / 2f) / Math.PI * 180);
        }
    }

    private float getPitch(double x0, double z0, float x, float y, float z) {
        double dx = x + 0.5f - x0;
        double dy = (float) (y + 0.5f - feetY - 1.62f);
        double dz = z + 0.5f - z0;
        float r = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        return (float) (-Math.asin(dy/r) / Math.PI * 180);
    }

    private void onBuyingMessage(String message) {
        int i = 15;
        while (message.charAt(i) != ' ') {
            i++;
        }
        int quantity = Integer.parseInt(message.substring(14, i));
        itemsInEq += quantity;

        if (itemsLeftInChest <= 0 || quantity < 1152) {
            nextChest();
        } else {
            itemsLeftInChest -= quantity;
        }

        if (itemsInEq <= 1152) {
            dealerStatus = DealerStatus.MOVING_ON_PLOT;
            moveToChest();
        } else {
            changeDealerStatus(DealerStatus.TP_TO_SHOP);
            removeDestination();
        }
    }

    private void tpToShop() {
        if (nextTp()) {
            sendChatMessage(config.getTpToShop());
        }
    }

    private void tpToPlot() {
        if (nextTp()) {
            sendChatMessage(config.getTpToPlot());
        }
    }

    private boolean nextTp() {
        if (timer.isNowAfter("nextPossibleTp")) {
            timer.setTimeFromNow("nextPossibleTp", "tpDelay");
            return true;
        } else {
            return false;
        }
    }

    private void stopDealing() {
        dealerStatus = DealerStatus.DISABLED;

    }

    private void buy() {
        connection.sendPacket(PlayerBlockPlacementPacket.builder()
                .position(getChestPosition())
                .face(0)
                .hand(0)
                .cursorX(0.5f)
                .cursorY(0.5f)
                .cursorZ(0.5f)
                .build()
        );
    }

    private Position getChestPosition() {
        return new Position(
                FIRST_CHEST_X[sector] + (DIRECTION_X[sector] * chest / 4),
                FIRST_CHEST_Y - (chest % 4),
                FIRST_CHEST_Z[sector] + (DIRECTION_Z[sector] * chest / 4)
        );
    }

    private void nextChest() {
        if (chest % 24 == 23) {
            chest = 0;
            if (sector == 7) {
                sector = 0;
            } else {
                sector++;
            }
        } else {
            chest++;
        }
        itemsLeftInChest = 3456;
    }

    private void onChestEmpty() {
        nextChest();
        dealerStatus = DealerStatus.MOVING_ON_PLOT;
        moveToChest();
    }

    private void moveToChest() {
        var position = getChestPosition();
        double newX;
        double newZ;

        if (DIRECTION_X[sector] == 0) {
            newZ = position.getZ() + 0.5d;
        } else if (DIRECTION_X[sector] > 0) {
            newZ = position.getZ() + 1.5d;
        } else {
            newZ = position.getZ() - 1.5d;
        }

        if (DIRECTION_Z[sector] == 0) {
            newX = position.getX() + 0.5d;
        } else if (DIRECTION_Z[sector] > 0) {
            newX = position.getX() - 1.5d;
        } else {
            newX = position.getX() + 1.5d;
        }

        setNewDestination(newX, newZ, getYaw(newX, newZ, position.getX(), position.getZ()),
                getPitch(newX, newZ, position.getX(), position.getY(), position.getZ()));
    }

    private void delayNextDealingAction() {
        timer.setTimeFromNow("nextPossibleDealerAction", "dealerActionDelay");
    }
}
