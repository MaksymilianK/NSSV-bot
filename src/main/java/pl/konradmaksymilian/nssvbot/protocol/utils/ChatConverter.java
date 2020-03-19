package pl.konradmaksymilian.nssvbot.protocol.utils;

import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import pl.konradmaksymilian.nssvbot.protocol.ChatComponent;
import pl.konradmaksymilian.nssvbot.protocol.ChatComponentStyle;
import pl.konradmaksymilian.nssvbot.protocol.ChatMessage;
import pl.konradmaksymilian.nssvbot.protocol.Colour;
import pl.konradmaksymilian.nssvbot.protocol.Style;
import pl.konradmaksymilian.nssvbot.utils.InvalidFormatCodeException;

public final class ChatConverter {

    private static final char FORMAT_SYMBOL = 167;
    private static final ObjectReader READER;
    
    static {
        var mapper = new ObjectMapper();
        READER = mapper.reader();
    }
    
    private ChatConverter() {}
    
    public static ChatMessage convert(String jsonData) throws IOException {
        var builder = ChatMessage.builder();
        var tree = READER.readTree(jsonData);

        appendChatComponent(tree, new ChatComponentStyle(false, false, false, false, false, null), builder);
        return builder.build();
    }
    
    private static void appendChatComponent(JsonNode jsonData, ChatComponentStyle inheritedStyle, 
            ChatMessage.Builder builder) {
        if (jsonData.isObject()) {
            String text = "";
            var textData = jsonData.get("text");
            if (textData != null) {
                if (!textData.isNull()) {
                    text = textData.asText();
                }
            }
            
            var style = style(inheritedStyle, jsonData.get("color"), jsonData.get("bold"), 
                    jsonData.get("strikethrough"), jsonData.get("italic"), jsonData.get("obfuscated"), 
                    jsonData.get("underlined"));
            
            if (!text.isEmpty()) {
                appendStringComponent(text, style, builder);
            }

            var extra = jsonData.get("extra");
            if (extra != null) {
               if (!extra.isNull()) {
                   extra.elements().forEachRemaining(json -> appendChatComponent(json, style, builder));
               }
            }
        } else {
            appendStringComponent(jsonData.asText(), inheritedStyle, builder);
        }
    }
    
    private static void appendStringComponent(String text, ChatComponentStyle inheritedStyle, 
            ChatMessage.Builder builder) {
        var componentBuilder = inheritedStyleBuilder(inheritedStyle);
        boolean isStylingNow = text.charAt(0) == FORMAT_SYMBOL;
        var textBuilder = new StringBuilder();
        
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            
            if (character == FORMAT_SYMBOL) {
                if (!isStylingNow) {
                    componentBuilder.text(textBuilder.toString());
                    textBuilder = new StringBuilder();
                    var component = componentBuilder.build();
                    componentBuilder = inheritedStyleBuilder(component.getStyle());
                    builder.append(component);
                    isStylingNow = true;
                }
                i++;
                addStyle(text.charAt(i), componentBuilder);
            } else {
                if (isStylingNow) {
                    isStylingNow = false;
                }
                textBuilder.append(character);
            }
            
            if (i == text.length() - 1) {
                componentBuilder.text(textBuilder.toString());
                builder.append(componentBuilder.build());
            }
        }
    }
    
    private static ChatComponent.Builder inheritedStyleBuilder(ChatComponentStyle inheritedStyle) {
        return ChatComponent.builder()
                .colour(inheritedStyle.getColour().orElse(null))
                .bold(inheritedStyle.isBold())
                .italic(inheritedStyle.isItalic())
                .obfuscated(inheritedStyle.isObfuscated())
                .strikethrough(inheritedStyle.isStrikethrough())
                .underlined(inheritedStyle.isUnderlined());
    }
    
    private static ChatComponentStyle style(ChatComponentStyle inheritedStyle, JsonNode colour, JsonNode bold, 
            JsonNode strikethrough, JsonNode italic, JsonNode obfuscated, JsonNode underlined) {
        var builder = ChatComponent.builder();
        
        builder.text("");
        
        if (colour == null) {
            builder.colour(inheritedStyle.getColour().orElse(null));
        } else if (colour.isNull()) {
            builder.colour(inheritedStyle.getColour().orElse(null));
        } else {
            builder.colour(colour.asText());
        }
        
        if (bold == null) {
            builder.bold(inheritedStyle.isBold());
        } else if (bold.isNull()) {
            builder.bold(inheritedStyle.isBold());
        } else {
            builder.bold(bold.asBoolean());
        }
        
        if (strikethrough == null) {
            builder.strikethrough(inheritedStyle.isStrikethrough());
        } else if (strikethrough.isNull()) {
            builder.strikethrough(inheritedStyle.isStrikethrough());
        } else {
            builder.strikethrough(strikethrough.asBoolean());
        }
        
        if (italic == null) {
            builder.italic(inheritedStyle.isItalic());
        } else if (italic.isNull()) {
            builder.italic(inheritedStyle.isItalic());
        } else {
            builder.italic(italic.asBoolean());
        }
        
        if (obfuscated == null) {
            builder.obfuscated(inheritedStyle.isObfuscated());
        } else if (obfuscated.isNull()) {
            builder.obfuscated(inheritedStyle.isObfuscated());
        } else {
            builder.obfuscated(obfuscated.asBoolean());
        }
        
        if (underlined == null) {
            builder.underlined(inheritedStyle.isUnderlined());
        } else if (underlined.isNull()) {
            builder.underlined(inheritedStyle.isUnderlined());
        } else {
            builder.underlined(underlined.asBoolean());
        }
        
        return builder.build().getStyle();
    }
    
    private static void addStyle(char code, ChatComponent.Builder builder) {
        if (code == Colour.BLACK.getCode()) {
            addColour(Colour.BLACK, builder);
        } else if (code == Colour.BLUE.getCode()) {
            addColour(Colour.BLUE, builder);
        } else if (code == Colour.BRIGHT_GREEN.getCode()) {
            addColour(Colour.BRIGHT_GREEN, builder);
        } else if (code == Colour.CYAN.getCode()) {
            addColour(Colour.CYAN, builder);
        } else if (code == Colour.DARK_BLUE.getCode()) {
            addColour(Colour.DARK_BLUE, builder);
        } else if (code == Colour.DARK_CYAN.getCode()) {
            addColour(Colour.DARK_CYAN, builder);
        } else if (code == Colour.DARK_GRAY.getCode()) {
            addColour(Colour.DARK_GRAY, builder);
        } else if (code == Colour.DARK_GREEN.getCode()) {
            addColour(Colour.DARK_GREEN, builder);
        } else if (code == Colour.DARK_RED.getCode()) {
            addColour(Colour.DARK_RED, builder);
        } else if (code == Colour.GOLD.getCode()) {
            addColour(Colour.GOLD, builder);
        } else if (code == Colour.GRAY.getCode()) {
            addColour(Colour.GRAY, builder);
        } else if (code == Colour.PINK.getCode()) {
            addColour(Colour.PINK, builder);
        } else if (code == Colour.PURPLE.getCode()) {
            addColour(Colour.PURPLE, builder);
        } else if (code == Colour.RED.getCode()) {
            addColour(Colour.RED, builder);
        } else if (code == Colour.WHITE.getCode()) {
            addColour(Colour.WHITE, builder);
        } else if (code == Colour.YELLOW.getCode()) {
            addColour(Colour.YELLOW, builder);
        } else if (code == Style.BOLD.getCode()) {
            builder.bold(true);   
        } else if (code == Style.ITALIC.getCode()) {
            builder.italic(true);   
        } else if (code == Style.RANDOM.getCode()) {
            builder.obfuscated(true);   
        } else if (code == Style.STRIKETHROUGH.getCode()) {
            builder.strikethrough(true);   
        } else if (code == Style.UNDERLINED.getCode()) {
            builder.underlined(true);   
        } else if (code == Style.RESET.getCode()) {
            resetStyle(builder);
        } else {
            throw new InvalidFormatCodeException("Cannot recognize the format code: " + code);
        }
    }
    
    private static void resetStyle(ChatComponent.Builder builder) {
        builder.colour(null);
        builder.italic(false);
        builder.obfuscated(false);
        builder.strikethrough(false);
        builder.underlined(false);
        builder.bold(false);
    }
    
    private static void addColour(Colour colour, ChatComponent.Builder builder) {
        builder.colour(colour.getName());
        builder.italic(false);
        builder.obfuscated(false);
        builder.strikethrough(false);
        builder.underlined(false);
        builder.bold(false);
    }
}
