package pl.konradmaksymilian.nssvbot.session;

import pl.konradmaksymilian.nssvbot.config.DealerConfig;
import pl.konradmaksymilian.nssvbot.connection.ConnectionManager;

import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.ChatMessageClientboundPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.PlayerPositionAndLookClientboundPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.PlayerBlockPlacementPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.PlayerLookPacket;
import pl.konradmaksymilian.nssvbot.utils.Timer;

public class DealerSession extends MovableSession {

    private static final double PLOT_CENTRE_X = -3212.5d;
    private static final double PLOT_CENTRE_Z = 8925.5d;
    private static final double MOVE = 4.25d;

    private final DealerConfig config;

    private DealerStatus dealerStatus;
    private int sector;
    private int chest;
    private int itemsLeftInChest;
    private int itemsInEq;

    public DealerSession(ConnectionManager connection, Timer timer, DealerConfig config) {
        super(connection, timer);
        this.config = config;
    }



    @Override
    protected void onPlayerPositionAndLook(PlayerPositionAndLookClientboundPacket packet) {
        super.onPlayerPositionAndLook(packet);
        if (dealerStatus.equals(DealerStatus.STARTING)) {
            moveToCorner(() -> {

            });
        }
    }

    @Override
    protected void onChatMessage(ChatMessageClientboundPacket packet) {
        super.onChatMessage(packet);
        String message = packet.toString();
        if (message.endsWith("17")) {
            connection.sendPacket(PlayerBlockPlacementPacket.builder()
                    .x(-3216)
                    .y(2)
                    .z(8928)
                    .face(1)
                    .hand(0)
                    .cursorX(0.5f)
                    .cursorY(1.0f)
                    .cursorZ(0.8f)
                    .build()
            );
            System.out.println("dsdsdssdsdasads");
        } else if (message.endsWith("18")) {
            sendChatMessage("/tpa geniuszmistrz");
        } else if (message.endsWith("19")) {
            connection.sendPacket(new PlayerLookPacket(-0.2f, 39.9f, true));
        }
        if (message.endsWith("-!start!-")) {

        } else if (message.endsWith("-!stop!-")) {

        } else if (message.endsWith("-!tpa!-")) {

        }
    }

    private void init() {
        dealerStatus = DealerStatus.DISABLED;
        sector = 0;
        chest = 0;
        itemsLeftInChest = 3456;
        itemsInEq = 0;
    }

    private void afterTpToPlot() {
        moveToCorner(this::buy);
    }

    private void buy() {

    }

    private void nextChest() {
        if (chest % 24 == 23) {
            chest = 0;
            sector++;
            dealerStatus = DealerStatus.MOVING_ON_PLOT;
            moveToCorner(this::buy);
        }
    }

    private void moveToCorner(Runnable onFinish) {
        int corner = sector / 2;
        double xMove = (corner == 0 || corner == 3) ? 1 : -1;
        double zMove = (corner == 0 || corner == 1) ? 1 : -1;

        setNewDestination(PLOT_CENTRE_X + (xMove * MOVE), PLOT_CENTRE_Z + (zMove * MOVE), onFinish);
    }
}
