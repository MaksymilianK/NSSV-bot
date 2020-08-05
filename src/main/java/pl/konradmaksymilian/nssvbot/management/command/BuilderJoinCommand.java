package pl.konradmaksymilian.nssvbot.management.command;

public class BuilderJoinCommand extends JoinCommand {
    public BuilderJoinCommand(String nickOrAlias) {
        super(nickOrAlias);
    }

    @Override
    public CommandName getName() {
        return CommandName.BUILDER_JOIN;
    }
}
