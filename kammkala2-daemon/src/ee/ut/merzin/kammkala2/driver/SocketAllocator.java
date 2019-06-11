
package ee.ut.merzin.kammkala2.driver;

import ee.ut.merzin.kammkala2.helpers.KammkalaLogger;
import java.net.Socket;

/**
 *
 * @author markko
 */
public class SocketAllocator implements Runnable {

    private Socket socket;
    private String ip;

    public SocketAllocator(String ip, Socket socket) {
        super();
        this.ip = ip;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            socket = new Socket(ip, 23);
        } catch (Exception ex) {
            KammkalaLogger.log(KammkalaLogger.ERROR, SocketAllocator.class.getName(), ex);
        }
    }

    public Socket getSocket() {
        return socket;
    }
}