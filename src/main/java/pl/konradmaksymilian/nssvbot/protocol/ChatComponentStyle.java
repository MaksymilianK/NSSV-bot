package pl.konradmaksymilian.nssvbot.protocol;

import java.util.Optional;

public final class ChatComponentStyle {
   
    private final boolean bold;
    private final boolean underlined;
    private final boolean strikethrough;
    private final boolean italic;
    private final boolean obfuscated;
    private String colour;
    
    public ChatComponentStyle(boolean bold, boolean underlined, boolean strikethrough, boolean italic,
            boolean obfuscated, String colour) {
        this.bold = bold;
        this.underlined = underlined;
        this.strikethrough = strikethrough;
        this.italic = italic;
        this.obfuscated = obfuscated;
        this.colour = colour;
    }

    public Optional<String> getColour() {
        return Optional.ofNullable(colour);
    }
    
    public void setColour(String colour) {
        this.colour = colour;
    }
    
    public boolean isBold() {
        return bold;
    }
    
    public boolean isUnderlined() {
        return underlined;
    }
    
    public boolean isStrikethrough() {
        return strikethrough;
    }
    
    public boolean isItalic() {
        return italic;
    }
    
    public boolean isObfuscated() {
        return obfuscated;
    }
}
