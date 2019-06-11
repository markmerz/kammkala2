package ee.ut.merzin.kammkala2.driver;

import ee.ut.merzin.kammkala2.helpers.KammkalaLogger;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements model for network switch using telnet connection to device.
 * @author markko
 */
public class SwitchCiscoCli implements Switch {

    private String ip;
    private String username;
    private String password;
    private String statusString;
    private int status;
    private String name;
    private String desc;
    private long runningTime;
    private String location;
    private Set<String> adminMacs;
    private Socket clientSocket;    
    private int id;
    private String prompt = null;
    private Map<String, Port> indexPort;
    private Set<String> vlans;

    public SwitchCiscoCli(String ip, String username, String password) {
        this.ip = ip;
        this.username = username;
        this.password = password;
    }

    @Override
    public Set<Port> getPorts() {
        if (indexPort != null) {
            return new TreeSet<Port>(indexPort.values());
        } else {
            return new TreeSet<Port>();
        }
    }

    @Override
    public Set<String> getVlans() {
        return vlans;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public String getStatusString() {
        return statusString;
    }

    @Override
    public String getIp() {
        return ip;
    }

    @Override
    public String getComm() {
        return username;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getPass() {
        return password;
    }

    @Override
    public Set<String> getMyMacs() {
        return adminMacs;
    }

    @Override
    public long getRunningTime() {
        return runningTime;
    }
    
    @Override
    public String toString() {
        return name;
    }

    @Override
    public void run() {
        long starttime = System.currentTimeMillis();
        status = Device.STATUS_RUNNING;
        try {
            InetAddress address = InetAddress.getByName(ip);
            if (!address.isReachable(1000)) {
                statusString = "not reachable? ip: " + ip;
                status = Device.STATUS_FAILED;
                KammkalaLogger.log(KammkalaLogger.ERROR, SwitchCiscoCli.class.getName(), statusString, null);
                return;
            }
        } catch (UnknownHostException ex) {
            statusString = "host unknown: " + ip;
            status = Device.STATUS_FAILED;
            KammkalaLogger.log(KammkalaLogger.ERROR, RouterCiscoCLI.class.getName(), statusString, null);
            return;
        } catch (IOException ex) {
            statusString = ip + ": " + ex.getMessage();
            status = Device.STATUS_FAILED;
            KammkalaLogger.log(KammkalaLogger.ERROR, SwitchCiscoCli.class.getName(), statusString, null);
            return;
        }

        try {
            makeConnection();
            readMyMacs();

            readPortList();
            readPortMacs();

            
            clientSocket.close();
        } catch (IOException ex) {
            statusString = ip + ": " + ex.getMessage();
            status = Device.STATUS_FAILED;
            KammkalaLogger.log(KammkalaLogger.ERROR, RouterCiscoCLI.class.getName(), statusString, null);
            return;
        }

        statusString = "OK";
        status = Device.STATUS_RESULT_READY;
        runningTime = System.currentTimeMillis() - starttime;
    }

    private void makeConnection() throws IOException {

        SocketAllocator sa = new SocketAllocator(ip, clientSocket);
        Thread sat = new Thread(sa);
        sat.start();

        try {
            sat.join(5000);
        } catch (InterruptedException e1) {
            throw new IOException(e1);
        }

        clientSocket = sa.getSocket();

        if (clientSocket != null && clientSocket.isConnected()) {

            readOutPutToLogin();
            clientSocket.getOutputStream().write((username + "\n").getBytes());
            readOutPutToLogin();
            clientSocket.getOutputStream().write((password + "\n").getBytes());
            String output = readOutPutToPromptOrLoginFailure();
            String[] lines = output.split("\n");
            name = lines[lines.length - 1].replace(">", "").replace("#", "").trim();
            clientSocket.getOutputStream().write(("terminal length 0" + "\n").getBytes());
            readOutPutToPrompt();
            clientSocket.getOutputStream().write(("show version" + "\n").getBytes());
            output = readOutPutToPrompt();
            lines = output.split("\n");
            desc = lines[1];

        } else {
            throw new IOException("No socket!");
        }

    }

    private String readOutPutToPrompt() throws IOException {
        StringBuilder sb = new StringBuilder();
        char c = 0;
        while (c != (char) -1 && !sb.toString().endsWith(this.prompt)) {
            c = (char) clientSocket.getInputStream().read();
            if (c == (char) -1) {
                throw new IOException(
                        "Connection lost while waiting for prompt.");
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private String readOutPutToPromptOrLoginFailure() throws IOException {
        StringBuilder sb = new StringBuilder();
        char c = 0;
        while ('>' != c && '#' != c && c != (char) -1) {
            c = (char) clientSocket.getInputStream().read();
            if (c == (char) -1) {
                throw new IOException(
                        "Connection lost while waiting for prompt.");
            }
            sb.append(c);
            if (sb.length() >= 6
                    && sb.substring(sb.length() - 6).toString().toLowerCase().endsWith("failed")) {
                throw new IOException("Login failed. Password incorrect?");
            }
        }
        String[] lines = sb.toString().split("\n");
        this.prompt = lines[lines.length - 1].trim();
        return sb.toString();
    }

    private String readOutPutToLogin() throws IOException {
        StringBuilder sb = new StringBuilder();
        char c = 0;
        while ((c = (char) clientSocket.getInputStream().read()) != -1) {
            sb.append(c);
            if (sb.toString().endsWith(": ")) {
                break;
            }
        }
        return sb.toString();
    }

    private void readMyMacs() throws IOException {

        adminMacs = new HashSet<String>();

        clientSocket.getOutputStream().write(("show interfaces" + "\n").getBytes());
        String output = readOutPutToPrompt();
        String[] lines = output.split("\n");
        Pattern pat = Pattern.compile("^.*address\\s+is\\s+([0-9a-fA-F]{4})\\.([0-9a-fA-F]{4})\\.([0-9a-fA-F]{4}).*$");
        // Pattern pat = Pattern.compile(".*address.*");
        for (int c = 1; c < lines.length; c++) {
            Matcher mat = pat.matcher(lines[c].replace("\r", ""));
            if (mat.matches()) {
                String macr = mat.group(1) + mat.group(2) + mat.group(3);
                String mac = macr.replaceFirst("^(..)(..)(..)(..)(..)(..)$",
                        "$1:$2:$3:$4:$5:$6");
                adminMacs.add(mac.toLowerCase());
            }
        }

    }

    private void readPortList() throws IOException {
        indexPort = new HashMap<String, Port>();
        clientSocket.getOutputStream().write(("show interface status" + "\n").getBytes());
        String output = readOutPutToPrompt();
        String[] lines = output.split("\n");
        Pattern pat = Pattern.compile("^(\\S+)\\s.*$");
        for (int c = 1; c < lines.length; c++) {
            Matcher mat = pat.matcher(lines[c].replace("\r", ""));
            if (mat.matches()) {
                String portname = mat.group(1);
                if (!portname.equals("Port") && !portname.equals("show")) {
                    Port port = new Port(portname, portname, this);
                    indexPort.put(portname, port);
                }
            }
        }
    }

    private void readPortMacs() throws IOException {
        vlans = new HashSet();
        clientSocket.getOutputStream().write(("show mac address" + "\n").getBytes());
        String output = readOutPutToPrompt();
        String[] lines = output.split("\n");
        Pattern pat = Pattern.compile("^\\s*(\\S+)\\s+([0-9a-fA-F]{4})\\.([0-9a-fA-F]{4})\\.([0-9a-fA-F]{4})\\s+.*\\s+(\\S+)\\s*$");
        for (int c = 1; c < lines.length; c++) {
            Matcher mat = pat.matcher(lines[c].replace("\r", ""));
            if (mat.matches()) {
                String vlan = mat.group(1);
                String port = mat.group(5);
                String macr = mat.group(2) + mat.group(3) + mat.group(4);
                String mac = macr.replaceFirst("^(..)(..)(..)(..)(..)(..)$",
                        "$1:$2:$3:$4:$5:$6").toLowerCase();

                indexPort.get(port).addMac(mac, vlan);
                vlans.add(vlan);
            }
        }
    }
   
}
