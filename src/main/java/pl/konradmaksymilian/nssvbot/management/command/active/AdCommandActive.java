package pl.konradmaksymilian.nssvbot.management.command.active;

import pl.konradmaksymilian.nssvbot.management.command.Command;
import pl.konradmaksymilian.nssvbot.management.command.CommandName;

public final class AdCommandActive implements Command {

    private final Integer duration;
    private final String text;
    
    public AdCommandActive(Integer duration, String text) {
        this.duration = duration;
        this.text = text;
    }
    
    public Integer getDuration() {
        return duration;
    }
    
    public String getText() {
        return text;
    }

    @Override
    public CommandName getName() {
        return CommandName.AD_ACTIVE;
    }
}
