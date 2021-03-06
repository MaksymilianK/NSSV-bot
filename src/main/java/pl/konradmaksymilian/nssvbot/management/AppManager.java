package pl.konradmaksymilian.nssvbot.management;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executors;

import pl.konradmaksymilian.nssvbot.management.command.AttachCommand;
import pl.konradmaksymilian.nssvbot.management.command.Command;
import pl.konradmaksymilian.nssvbot.management.command.CommandReadingException;
import pl.konradmaksymilian.nssvbot.management.command.JoinCommand;
import pl.konradmaksymilian.nssvbot.management.command.active.AdCommandActive;
import pl.konradmaksymilian.nssvbot.management.command.active.LeaveCommandActive;
import pl.konradmaksymilian.nssvbot.management.command.detached.AdCommandDetached;
import pl.konradmaksymilian.nssvbot.management.command.detached.LeaveCommandDetached;
import pl.konradmaksymilian.nssvbot.protocol.ChatMessage;
import pl.konradmaksymilian.nssvbot.session.SessionException;
import pl.konradmaksymilian.nssvbot.session.SessionsManager;

public class AppManager {
    
    private final ConsoleManager console;
    private final SessionsManager sessionsManager;
    
    public AppManager(ConsoleManager console, SessionsManager sessionsManager) {
        this.console = console;
        this.sessionsManager = sessionsManager;
        initSessionsManager();
        initConsole();
        writeStatus();
    }
    
    private void initSessionsManager() {
        var config = new Properties();
        try {
            config.load(new FileInputStream("./players.properties"));
        } catch (FileNotFoundException e) {
            console.writeLine("File with players data has not been found", true);
        } catch (IOException e) {
            console.writeLine("Error while reading players data", true);
        } finally {
            sessionsManager.onMessage(this::onMessage);
            sessionsManager.setPlayers(config);
        }
    }

    private void initConsole() {
        console.onInput(this::onInput);
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                console.listen();
            } catch (Exception e) {
                System.exit(0);
            }
        });
    }
    
    private synchronized void onInput(String line) {
        if (sessionsManager.waitsForPassword()) {
            sessionsManager.join(line);
        } else {
            try {
                var command = CommandReader.read(line, sessionsManager.isAnyActive());
                if (command.isPresent()) {
                    onCommand(command.get());
                } else if (sessionsManager.isAnyActive()) {
                    sessionsManager.sendMessage(line);
                } else {
                    console.writeLine("It is not a valid input!", true);
                }
            } catch (SessionException | CommandReadingException e) {
                console.writeLine(e.getMessage(), true);
            }
        }
    }
    
    private void onMessage(Object message) {
        if (message instanceof String) {
            console.writeLine((String) message, true);
        } else {
            console.writeChatMessage(((ChatMessage) message));
        }
    }

    private void onCommand(Command command) {
        switch (command.getName()) {
            case JOIN:
                onJoin((JoinCommand) command);
                break;
            case ATTACH:
                onAttach((AttachCommand) command);
                break;
            case DETACH:
                onDetach();
                break;
            case AD_ACTIVE:
                onAdActive((AdCommandActive) command);
                break;
            case AD_DETACHED:
                onAdDetached((AdCommandDetached) command);
                break;
            case LEAVE_ACTIVE:
                onLeaveActive((LeaveCommandActive) command);
                break;
            case LEAVE_DETACHED:
                onLeaveDetached((LeaveCommandDetached) command);
                break;
            default:
                throw new RuntimeException("Command name: '" + command.getName() + "' is unrecognized!");
        }
    }

    private void onJoin(JoinCommand command) {
        if (sessionsManager.join(command)) {
            console.writeLine("Player '" + command.getNickOrAlias() + "' is joining server...", true);
            console.writeBlankLine();
        } else {
            console.writeLine("Password: ", false);
        }
    }
    
    private void onAttach(AttachCommand command) {
        sessionsManager.attach(command);
        console.clearScreen();
    }

    private void onDetach() {
        if (sessionsManager.detach()) {
            console.clearScreen();
        }
        writeStatus();
    }

    private void onAdActive(AdCommandActive command) {
        if (!sessionsManager.setAdActive(command)) {
            console.writeLine("Advertising has been stopped", true);
        }
    }
    
    private void onAdDetached(AdCommandDetached command) {
        if (!sessionsManager.setAdDetached(command)) {
            console.writeLine("Advertising has been stopped", true);
        }
    }
    
    private void onLeaveActive(LeaveCommandActive command) {
        sessionsManager.leaveActive();
        console.clearScreen();
        writeStatus();
    }
    
    private void onLeaveDetached(LeaveCommandDetached command) {
        sessionsManager.leaveDetached(command);
        console.writeLine("The player has left the server", true);
    }
    
    private void writeStatus() {
        var allSessions = sessionsManager.getStatus();
        if (allSessions.isEmpty()) {
            console.writeLine("There are no sessions", true);
            return;
        }
        
        console.writeLine("Status:", false);
        console.writeBlankLine();
        
        allSessions.forEach((player, status) -> {
            console.write("    " + player.getNick(), false);
            player.getAlias().ifPresent(alias -> console.write(" <" + alias + ">", false));
            console.write(" " + status, false);
            console.nextLine();
        });
    }
}
