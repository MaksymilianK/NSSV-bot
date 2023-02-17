package pl.konradmaksymilian.nssvbot.management.command;

public class SandJoinCommand extends JoinCommand {
    public SandJoinCommand(String nickOrAlias) {
        super(nickOrAlias);
    }

    @Override
    public CommandName getName() {
        return CommandName.SAND_JOIN;
    }
}
