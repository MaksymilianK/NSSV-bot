package pl.konradmaksymilian.nssvbot.management;

import java.util.Arrays;
import java.util.Optional;

import pl.konradmaksymilian.nssvbot.management.command.*;
import pl.konradmaksymilian.nssvbot.management.command.active.AdCommandActive;
import pl.konradmaksymilian.nssvbot.management.command.active.LeaveCommandActive;
import pl.konradmaksymilian.nssvbot.management.command.detached.AdCommandDetached;
import pl.konradmaksymilian.nssvbot.management.command.detached.LeaveCommandDetached;

public final class CommandReader {
    
    private CommandReader() {}
    
    /**
     * @return optional of command or empty optional if it is not a command
     * @throws {@link CommandReadingException} if the command is invalid
     */
    public static Optional<Command> read(String line, boolean isSessionActive) {
        if (!startsWithExclamationMark(line)) {
            return Optional.empty();
        }
        
        String[] parts = line.substring(1).split(" "); //the first sign is '!' so it needs to be removed
        var commandPayload = Arrays.copyOfRange(parts, 1, parts.length);

        return Optional.of(buildCommand(parts[0], commandPayload, isSessionActive));
    }

    private static Command buildCommand(String name, String[] payload, boolean isSessionActive) {
        Command command;

        switch (name) {
            case "join":
                command = buildJoinCommand(payload);
                break;
            case "dealer":
                command = buildDealerJoinCommand(payload);
                break;
            case "builder":
                command = buildBuilderJoinCommand(payload);
                break;
            case "attach":
                command = buildAttachCommand(payload);
                break;
            case "detach":
                command = buildDetachCommand(payload);
                break;
            case "ad":
                if (isSessionActive) {
                    command = buildAdCommandActive(payload);
                } else {
                    command = buildAdCommandDetached(payload);
                }
                break;
            case "leave":
                if (isSessionActive) {
                    command = buildLeaveCommandActive(payload);
                } else {
                    command = buildLeaveCommandDetached(payload);
                }
                break;
            default:
                throw new CommandReadingException("The name: '" + name + "' is unknown - there is no such a command");
        }
        return command;
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

    private static DealerJoinCommand buildDealerJoinCommand(String[] parts) {
        if (parts.length == 1) {
            return new DealerJoinCommand(parts[0]);
        } else {
            throw new CommandReadingException("Command 'dealer [nickOrAlias]' is too long");
        }
    }

    private static BuilderJoinCommand buildBuilderJoinCommand(String[] parts) {
        if (parts.length == 1) {
            return new BuilderJoinCommand(parts[0]);
        } else {
            throw new CommandReadingException("Command 'builder [nickOrAlias]' is too long");
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
