package pl.konradmaksymilian.nssvbot.connection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

class Connection implements AutoCloseable {
    
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    
    public Connection(String host, int port) {
        try {
            socket = new Socket(host, port);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            throw new ConnectionException("Connection has been lost", e);
        }
    }
    
    public DataInputStream getIn() {
        return in;
    }
    
    public DataOutputStream getOut() {
        return out;
    }
    
    @Override
    public void close() {
        try {
            if (in != null) {
                in.close();
            }
            
            if (out != null) {
                out.close();
            }
            
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            throw new ConnectionException("Error while closing connection", e);
        }
    }
}
