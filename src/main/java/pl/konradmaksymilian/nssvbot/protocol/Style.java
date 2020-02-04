package pl.konradmaksymilian.nssvbot.protocol;


public enum Style {

    RANDOM ('k'),
    BOLD ('l'),
    STRIKETHROUGH ('m'),
    UNDERLINED ('n'),
    ITALIC ('o'),
    RESET ('r');
    
    private final char code;
    
    private Style(char code) {
        this.code = code;
    }
    
    public char getCode() {
        return code;
    }
}
