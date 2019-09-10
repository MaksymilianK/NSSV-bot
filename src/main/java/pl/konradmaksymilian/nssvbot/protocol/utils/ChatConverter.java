package pl.konradmaksymilian.nssvbot.protocol.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import pl.konradmaksymilian.nssvbot.protocol.ChatComponent;
import pl.konradmaksymilian.nssvbot.protocol.ChatComponentStyle;
import pl.konradmaksymilian.nssvbot.protocol.ChatMessage;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.ChatMessageClientboundPacket;
import pl.konradmaksymilian.nssvbot.utils.InvalidFormatCodeException;

public final class ChatConverter {

    private static final char FORMAT_SYMBOL = '§';
    private static final ObjectReader READER;
    
    static {
        var mapper = new ObjectMapper();
        READER = mapper.reader();
    }
    
    private ChatConverter() {}
    
    public static ChatMessage convert(String jsonData) throws IOException {
        var tree = READER.readTree(jsonData);
        var builder = ChatMessage.builder();
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
        switch (code) {
            case '0':
                addColour("black", builder);
                break;
            case '1':
                addColour("dark_blue", builder);
                break;
            case '2':
                addColour("dark_green", builder);
                break;
            case '3':
                addColour("dark_aqua", builder);
                break;
            case '4':
                addColour("dark_red", builder);
                break;
            case '5':
                addColour("dark_purple", builder);
                break;
            case '6':
                addColour("gold", builder);
                break;
            case '7':
                addColour("gray", builder);
                break;   
            case '8':
                addColour("dark_gray", builder);
                break;
            case '9':
                addColour("blue", builder);
                break;
            case 'a':
                addColour("green", builder);
                break;
            case 'b':
                addColour("aqua", builder);
                break;
            case 'c':
                addColour("red", builder);
                break;
            case 'd':
                addColour("light_purple", builder);
                break;
            case 'e':
                addColour("yellow", builder);
                break;
            case 'f':
                addColour("white", builder);
                break;
            case 'k':
                builder.obfuscated(true);
                break;
            case 'l':
                builder.bold(true);
                break;
            case 'm':
                builder.strikethrough(true);
                break;
            case 'n':
                builder.underlined(true);
                break;
            case 'o':
                builder.italic(true);
                break;
            case 'r':
                resetStyle(builder);
                break;
            default:
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
    
    private static void addColour(String colour, ChatComponent.Builder builder) {
        builder.colour(colour);
        builder.italic(false);
        builder.obfuscated(false);
        builder.strikethrough(false);
        builder.underlined(false);
        builder.bold(false);
    }
}
