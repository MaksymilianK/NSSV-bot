package pl.konradmaksymilian.nssvbot.config;

public final class DealerConfig {

    private final String tpToPlot;
    private final String tpToShop;

    public DealerConfig(String tpToPlot, String tpToShop) {
        this.tpToPlot = tpToPlot;
        this.tpToShop = tpToShop;
    }

    public String getTpToPlot() {
        return tpToPlot;
    }

    public String getTpToShop() {
        return tpToShop;
    }
}
