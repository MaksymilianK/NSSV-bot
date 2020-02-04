package pl.konradmaksymilian.nssvbot.management.command;

public final class AttachCommand implements Command {

    private final String nickOrAlias;
    
    public AttachCommand(String nickOrAlias) {
        this.nickOrAlias = nickOrAlias;
    }
    
    public String getNickOrAlias() {
        return nickOrAlias;
    }

    @Override
    public CommandName getName() {
        return CommandName.ATTACH;
    }
}
