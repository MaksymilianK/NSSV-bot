package pl.konradmaksymilian.nssvbot.management.command.active;

import pl.konradmaksymilian.nssvbot.management.command.Command;
import pl.konradmaksymilian.nssvbot.management.command.CommandName;

public final class LeaveCommandActive implements Command {

    @Override
    public CommandName getName() {
        return CommandName.LEAVE_ACTIVE;
    }
}
