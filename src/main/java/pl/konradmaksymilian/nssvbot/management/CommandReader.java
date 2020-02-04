package pl.konradmaksymilian.nssvbot.management;

import java.util.Arrays;
import java.util.Optional;

import pl.konradmaksymilian.nssvbot.management.command.AttachCommand;
import pl.konradmaksymilian.nssvbot.management.command.Command;
import pl.konradmaksymilian.nssvbot.management.command.CommandReadingException;
import pl.konradmaksymilian.nssvbot.management.command.DetachCommand;
import pl.konradmaksymilian.nssvbot.management.command.JoinCommand;
import pl.konradmaksymilian.nssvbot.management.command.active.AdCommandActive;
import pl.konradmaksymilian.nssvbot.management.command.active.LeaveCommandActive;
import pl.konradmaksymilian.nssvbot.management.command.detached.AdCommandDetached;
import pl.konradmaksymilian.nssvbot.management.command.detached.LeaveCommandDetached;

public final class CommandReader {
    
    private CommandReader() {}
    
    /**
     * @return optional of command or empty optional if it is not a command
     * @throws {@link CommandReadingException} if command is invalid
     */
    public static Optional<Command> read(String line, boolean isSessionActive) {
        if (!startsWithExclamationMark(line)) {
            return Optional.empty();
        }
        
        String[] parts = line.substring(1).split(" "); //the first sign is '!' so it needs to be removed
        var commandPayload = Arrays.copyOfRange(parts, 1, parts.length);
        
        Command command;
        switch (parts[0]) {
            case "join":
                command = buildJoinCommand(commandPayload);
                break;
            case "attach":
                command = buildAttachCommand(commandPayload);
                break;
            case "detach":
                command = buildDetachCommand(commandPayload);
                break;
            case "ad":
                if (isSessionActive) {
                    command = buildAdCommandActive(commandPayload);
                } else {
                    command = buildAdCommandDetached(commandPayload);
                }
                break;
            case "leave":
                if (isSessionActive) {
                    command = buildLeaveCommandActive(commandPayload);
                } else {
                    command = buildLeaveCommandDetached(commandPayload);
                }
                break;
            default:
                throw new CommandReadingException("The name: '" + parts[0] + "' is unknown - there is no such a command");
        }
        return Optional.of(command);
    }
    
    private static boolean startsWithExclamationMark(String line) {
        return line.startsWith("!");
    }
    
    private static JoinCommand buildJoinCommand(String[] parts) {
        if (parts.length == 1) {
            return new JoinCommand(parts[0]);
        } else {
            throw new CommandReadingException("Command 'join [nickOrAlias]' is too long");
        }
        
    }
    
    private static AttachCommand buildAttachCommand(String[] parts) {
        if (parts.length == 1) {
            return new AttachCommand(parts[0]);
        }
        if (parts.length < 1) {
            throw new CommandReadingException("Command 'attach [nickOrAlias]' is too short");
        } else {
            throw new CommandReadingException("Command 'attach [nickOrAlias]' is too long");
        }
    }
    
    private static DetachCommand buildDetachCommand(String[] parts) {
        if (parts.length == 0) {
            return new DetachCommand();
        } else {
            throw new CommandReadingException("Command 'detach (no params)' is too long");
        }
    }
    
    private static AdCommandActive buildAdCommandActive(String[] parts) {
        if (parts.length < 2) {
            throw new CommandReadingException("Command 'ad [duration] [text]' is too short");
        }
        
        Integer duration;
        try {
            duration = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            throw new CommandReadingException("Command 'ad [duration (in seconds)] [text]' - parameter 'duration' should"
                    + " be a number!");
        }

        String text = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
       
        return new AdCommandActive(duration, text); 
    }
    
    private static AdCommandDetached buildAdCommandDetached(String[] parts) {
        if (parts.length < 3) {
            throw new CommandReadingException("Command 'ad [nickOrAlias] [duration (in seconds)] [text]' is too short");
        }
        
        Integer duration = null;        
        try {
            duration = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new CommandReadingException("Command 'ad [nickOrAlias] [duration (in seconds)] [text]' - parameter "
                    + "'duration' should be a number!");
        }

        String text = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));
       
        return new AdCommandDetached(parts[0], duration, text); 
    }
    
    private static LeaveCommandActive buildLeaveCommandActive(String[] parts) {
        if (parts.length == 0) {
            return new LeaveCommandActive();
        } else {
            throw new CommandReadingException("Command 'leave (no params)' is too long");
        }
    }
    
    private static LeaveCommandDetached buildLeaveCommandDetached(String[] parts) {
        if (parts.length == 1) {
            return new LeaveCommandDetached(parts[0]);
        } else if (parts.length < 1) {
            throw new CommandReadingException("Command 'leave [nickOrAlias]' is too short");
        } else {
            throw new CommandReadingException("Command 'leave [nickOrAlias]' is too long");
        }
    }
}
