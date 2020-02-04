package pl.konradmaksymilian.nssvbot.management.command.detached;

import pl.konradmaksymilian.nssvbot.management.command.Command;
import pl.konradmaksymilian.nssvbot.management.command.CommandName;

public final class AdCommandDetached implements Command {

    private final String nickOrAlias;
    private final Integer duration;
    private final String text;
    
    public AdCommandDetached(String nickOrAlias, Integer duration, String text) {
        this.nickOrAlias = nickOrAlias;
        this.duration = duration;
        this.text = text;
    }
    
    public String getNickOrAlias() {
        return nickOrAlias;
    }
    
    public Integer getDuration() {
        return duration;
    }
    
    public String getText() {
        return text;
    }

    @Override
    public CommandName getName() {
        return CommandName.AD_DETACHED;
    }
}
