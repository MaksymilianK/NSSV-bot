package pl.konradmaksymilian.nssvbot.utils;

import java.util.List;

import pl.konradmaksymilian.nssvbot.protocol.ChatComponent;

public final class ChatFormatter {
    
    private ChatFormatter() {}
    
    public static String getPureText(List<ChatComponent> components) {
        var builder = new StringBuilder();
        components.forEach(component -> builder.append(component.getText()));
        return builder.toString();
    }
}
