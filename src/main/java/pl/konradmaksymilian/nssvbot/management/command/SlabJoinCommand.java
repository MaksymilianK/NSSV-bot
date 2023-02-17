package pl.konradmaksymilian.nssvbot.management.command;

public class SlabJoinCommand extends JoinCommand {
    public SlabJoinCommand(String nickOrAlias) {
        super(nickOrAlias);
    }

    @Override
    public CommandName getName() {
        return CommandName.SLAB_JOIN;
    }
}
