package pl.konradmaksymilian.nssvbot.session;

public class Advert {

    private final int duration;
    private final String text;
    
    public Advert(int duration, String text) {
        this.duration = duration;
        this.text = text;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public String getText() {
        return text;
    }
}
