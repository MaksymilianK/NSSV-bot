package pl.konradmaksymilian.nssvbot.management.command;

public enum CommandName {

    JOIN ("join"),
    DEALER_JOIN ("dealer"),
    ATTACH ("attach"),
    DETACH ("detach"),
    AD_ACTIVE ("ad"),
    AD_DETACHED ("ad"),
    LEAVE_ACTIVE ("leave"),
    LEAVE_DETACHED ("leave");

    private final String label;

    CommandName(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
