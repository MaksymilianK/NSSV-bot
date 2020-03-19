package pl.konradmaksymilian.nssvbot.session;

import pl.konradmaksymilian.nssvbot.connection.ConnectionManager;
import pl.konradmaksymilian.nssvbot.utils.Timer;

import java.time.Duration;

public class BasicAfkSession extends Session {

    private Advert advert;

    public BasicAfkSession(ConnectionManager connection, Timer timer) {
        super(connection, timer);
    }

    public void setAd(Advert advert) {
        if (advert.getDuration() < 0) {
            this.advert = null;
        } else if (advert.getDuration() >= 60) {
            this.advert = advert;
            timer.setTimeToNow("lastAdvertising");
            timer.setDuration("advertising", Duration.ofSeconds(advert.getDuration()));
            sendChatMessage(advert.getText());
        } else {
            throw new IllegalArgumentException("Advert cannot have positive duration that is less than 60s");
        }
    }

    @Override
    protected void onEveryCheck() {
        super.onEveryCheck();
        checkAdvert();
    }

    private void checkAdvert() {
        if (advert != null) {
            if (timer.isNowAfterDuration("lastAdvertising", "advertising")) {
                timer.setTimeToNow("lastAdvertising");
                sendChatMessage(advert.getText());
            }
        }
    }
}
