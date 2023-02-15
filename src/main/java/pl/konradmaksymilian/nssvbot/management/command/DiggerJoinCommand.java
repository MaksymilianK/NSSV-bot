package pl.konradmaksymilian.nssvbot.management.command;

public class DiggerJoinCommand extends JoinCommand {
    public DiggerJoinCommand(String nickOrAlias) {
        super(nickOrAlias);
    }

    @Override
    public CommandName getName() {
        return CommandName.DIGGER_JOIN;
    }
}
