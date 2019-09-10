package pl.konradmaksymilian.nssvbot.protocol;

import pl.konradmaksymilian.nssvbot.ObjectConstructionException;

public final class ChatComponent {

    private final String text;
    private final ChatComponentStyle style;
    
    private ChatComponent(String text, ChatComponentStyle style) {
        this.text = text;
        this.style = style;
    }
    
    public String getText() {
        return text;
    }
    
    public ChatComponentStyle getStyle() {
        return style;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static final class Builder {
        
        private String text;
        private Boolean bold;
        private Boolean underlined;
        private Boolean strikethrough;
        private Boolean italic;
        private Boolean obfuscated;
        private String colour;
        
        Builder() {}
        
        public Builder text(String text) {
            this.text = text;
            return this;
        }
        
        public Builder bold(boolean bold) {
            this.bold = bold;
            return this;
        }
        
        public Builder underlined(boolean underlined) {
            this.underlined = underlined;
            return this;
        }
        
        public Builder strikethrough(boolean strikethrough) {
            this.strikethrough = strikethrough;
            return this;
        }
        
        public Builder italic(boolean italic) {
            this.italic = italic;
            return this;
        }
        
        public Builder obfuscated(boolean obfuscated) {
            this.obfuscated = obfuscated;
            return this;
        }
        
        public Builder colour(String colour) {
            this.colour = colour;
            return this;
        }

        public ChatComponent build() {
            if (text == null || bold == null || underlined == null || strikethrough == null || italic == null 
                    || obfuscated == null) {
                throw new ObjectConstructionException("Cannot build ChatComponent - styles cannot be null");
            }
            var style = new ChatComponentStyle(bold, underlined, strikethrough, italic, obfuscated, colour);
            return new ChatComponent(text, style);
        }
    }
}
