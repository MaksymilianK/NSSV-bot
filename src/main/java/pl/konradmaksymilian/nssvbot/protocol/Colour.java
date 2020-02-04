package pl.konradmaksymilian.nssvbot.protocol;


public enum Colour {

    BLACK ('0', "black"),
    DARK_BLUE ('1', "dark_blue"),
    DARK_GREEN ('2', "dark_green"),
    DARK_CYAN ('3', "dark_aqua"),
    DARK_RED ('4', "dark_red"),
    PURPLE ('5', "dark_purple"),
    GOLD ('6', "gold"),
    GRAY ('7', "gray"),
    DARK_GRAY ('8', "dark_gray"),
    BLUE ('9', "blue"),
    BRIGHT_GREEN ('a', "green"),
    CYAN ('b', "aqua"),
    RED ('c', "red"),
    PINK ('d', "light_purple"),
    YELLOW ('e', "yellow"),
    WHITE ('f', "white");
    
    private final char code;
    private final String name;
    
    private Colour(char code, String name) {
        this.code = code;
        this.name = name;
    }
    
    public char getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
}
