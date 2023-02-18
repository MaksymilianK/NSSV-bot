package pl.konradmaksymilian.nssvbot.management.command;

public class GateJoinCommand extends JoinCommand {
    public GateJoinCommand(String nickOrAlias) {
        super(nickOrAlias);
    }

    @Override
    public CommandName getName() {
        return CommandName.GATE_JOIN;
    }
}
