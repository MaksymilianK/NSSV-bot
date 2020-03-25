package pl.konradmaksymilian.nssvbot.management.command;

public final class DealerJoinCommand extends JoinCommand {

    public DealerJoinCommand(String nickOrAlias) {
        super(nickOrAlias);
    }

    @Override
    public CommandName getName() {
        return CommandName.DEALER_JOIN;
    }
}
