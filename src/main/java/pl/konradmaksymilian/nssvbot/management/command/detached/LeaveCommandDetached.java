package pl.konradmaksymilian.nssvbot.management.command.detached;

import pl.konradmaksymilian.nssvbot.management.command.Command;
import pl.konradmaksymilian.nssvbot.management.command.CommandName;

public final class LeaveCommandDetached implements Command {

    private final String nickOrAlias;
    
    public LeaveCommandDetached(String nickOrAlias) {
        this.nickOrAlias = nickOrAlias;
    }
    
    public String getNickOrAlias() {
        return nickOrAlias;
    }
    
    @Override
    public CommandName getName() {
        return CommandName.LEAVE_DETACHED;
    }
}
