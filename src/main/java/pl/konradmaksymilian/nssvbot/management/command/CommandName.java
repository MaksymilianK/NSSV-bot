package pl.konradmaksymilian.nssvbot.management.command;

public enum CommandName {

    JOIN ("join"),
    DEALER_JOIN ("dealer"),
    BUILDER_JOIN ("builder"),
    TEST_JOIN ("test"),
    ATTACH ("attach"),
    DETACH ("detach"),
    AD_ACTIVE ("ad"),
    AD_DETACHED ("ad"),
    LEAVE_ACTIVE ("leave"),
    LEAVE_DETACHED ("leave"),
    DIGGER_JOIN("digger"),
    FENCE_JOIN("fence");

    private final String label;

    CommandName(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
