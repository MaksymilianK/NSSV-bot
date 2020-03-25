package pl.konradmaksymilian.nssvbot.management.command;

public class JoinCommand implements Command {

    private final String nickOrAlias;
    
    public JoinCommand(String nickOrAlias) {
        this.nickOrAlias = nickOrAlias;
    }
    
    public String getNickOrAlias() {
        return nickOrAlias;
    }

    public CommandName getName() {
        return CommandName.JOIN;
    }
}
