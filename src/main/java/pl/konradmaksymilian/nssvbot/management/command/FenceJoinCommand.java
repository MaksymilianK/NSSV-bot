package pl.konradmaksymilian.nssvbot.management.command;

public class FenceJoinCommand extends JoinCommand {
    public FenceJoinCommand(String nickOrAlias) {
        super(nickOrAlias);
    }

    @Override
    public CommandName getName() {
        return CommandName.FENCE_JOIN;
    }
}
