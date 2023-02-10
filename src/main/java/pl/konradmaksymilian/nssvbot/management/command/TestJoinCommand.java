package pl.konradmaksymilian.nssvbot.management.command;

public final class TestJoinCommand extends JoinCommand {

    public TestJoinCommand(String nickOrAlias) {
        super(nickOrAlias);
    }

    @Override
    public CommandName getName() {
        return CommandName.TEST_JOIN;
    }
}
