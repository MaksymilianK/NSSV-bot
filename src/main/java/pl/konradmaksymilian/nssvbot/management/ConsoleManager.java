package pl.konradmaksymilian.nssvbot.management;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Consumer;

import com.diogonunes.jcdp.color.ColoredPrinter;
import com.diogonunes.jcdp.color.api.Ansi.Attribute;
import com.diogonunes.jcdp.color.api.Ansi.BColor;
import com.diogonunes.jcdp.color.api.Ansi.FColor;

import pl.konradmaksymilian.nssvbot.protocol.ChatComponentStyle;
import pl.konradmaksymilian.nssvbot.protocol.ChatMessage;
import pl.konradmaksymilian.nssvbot.protocol.Colour;

public class ConsoleManager {
    
    private final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    private final ColoredPrinter printer = new ColoredPrinter.Builder(1, false).build();
    private Consumer<String> onInput;
    
    public void onInput(Consumer<String> onInput) {
        this.onInput = onInput;
    }
    
    public void listen() throws InterruptedException, IOException {
        while (true) {
            String line;
            if ((line = in.readLine()) != null) {
                onInput.accept(line);
            }
            Thread.sleep(500);
        }
    }
    
    public void writeLine(String text, boolean prefixed) {
        if (prefixed) {
            printer.println("<NSSVBot> - " + text);
        } else {
            printer.println(text);
        }
    }
    
    public void write(String text, boolean prefixed) {
        if (prefixed) {
            printer.print("<NSSVBot> - " + text);
        } else {
            printer.print(text);
        }
    }
    
    public void nextLine() {
        printer.print("\n");
    }
    
    public void clearScreen() {
        for (int i = 0; i < 15; i++) {
            printer.println("");
        }
    }
    
    public void writeBlankLine() {
        printer.println("");
    }
    
    public void writeChatMessage(ChatMessage message) {
        message.getComponents().forEach(component -> {
            setStyle(component.getStyle());
            //printer.print(component.getText());
            System.out.println(component.getText());
            printer.clear();
        });
        //printer.print("\n");
        System.out.println();
    }
    
    private void setStyle(ChatComponentStyle style) {
        if (style.isBold()) {
            printer.setAttribute(Attribute.BOLD);
        }
        
        if (style.isUnderlined()) {
            printer.setAttribute(Attribute.UNDERLINE);
        }
        
        if (style.getColour().isPresent()) {
            var colour = style.getColour().get();
            
            if (colour.equals(Colour.BLACK.getName())) {
                printer.setForegroundColor(FColor.BLACK);
                printer.setBackgroundColor(BColor.WHITE);
            } else if (colour.equals(Colour.BLUE.getName())) {
                printer.setForegroundColor(FColor.BLUE);
            } else if (colour.equals(Colour.BRIGHT_GREEN.getName())) {
                printer.setForegroundColor(FColor.GREEN);
            } else if (colour.equals(Colour.CYAN.getName())) {
                printer.setForegroundColor(FColor.CYAN);
            } else if (colour.equals(Colour.DARK_BLUE.getName())) {
                printer.setForegroundColor(FColor.BLUE);
            } else if (colour.equals(Colour.DARK_CYAN.getName())) {
                printer.setForegroundColor(FColor.CYAN);
            } else if (colour.equals(Colour.DARK_GRAY.getName())) {
                printer.setForegroundColor(FColor.WHITE);
            } else if (colour.equals(Colour.DARK_GREEN.getName())) {
                printer.setForegroundColor(FColor.GREEN);
            } else if (colour.equals(Colour.DARK_RED.getName())) {
                printer.setForegroundColor(FColor.RED);
            } else if (colour.equals(Colour.GOLD.getName())) {
                printer.setForegroundColor(FColor.YELLOW);
            } else if (colour.equals(Colour.GRAY.getName())) {
                printer.setForegroundColor(FColor.BLACK);
                printer.setForegroundColor(FColor.WHITE);
            } else if (colour.equals(Colour.PINK.getName())) {
                printer.setForegroundColor(FColor.MAGENTA);
            } else if (colour.equals(Colour.PURPLE.getName())) {
                printer.setForegroundColor(FColor.MAGENTA);
            } else if (colour.equals(Colour.RED.getName())) {
                printer.setForegroundColor(FColor.RED);
            } else if (colour.equals(Colour.WHITE.getName())) {
                printer.setForegroundColor(FColor.WHITE);
            } else if (colour.equals(Colour.YELLOW.getName())) {
                printer.setForegroundColor(FColor.YELLOW);
            }
        }
    }
}
