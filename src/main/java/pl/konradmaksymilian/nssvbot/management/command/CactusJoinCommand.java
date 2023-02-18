package pl.konradmaksymilian.nssvbot.management.command;

public class CactusJoinCommand extends JoinCommand {
    public CactusJoinCommand(String nickOrAlias) {
        super(nickOrAlias);
    }

    @Override
    public CommandName getName() {
        return CommandName.CACTUS_JOIN;
    }
}
