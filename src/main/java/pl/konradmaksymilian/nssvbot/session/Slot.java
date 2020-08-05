package pl.konradmaksymilian.nssvbot.session;

public class Slot {

    private byte[] data;

    public Slot(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isPresent() {
        return data.length > 2;
    }

    public int getCount() {
        if (data.length < 3) {
            return 0;
        } else {
            return data[2];
        }
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
