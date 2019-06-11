package ee.ut.merzin.kammkala2.driver;

import ee.ut.merzin.kammkala2.helpers.KammkalaLogger;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements router using telnet connection to router device.
 * @author markko
 */
public class RouterCiscoCLI implements Router {

    private String ip;
    private String statusString;
    private int status;
    private String name;
    private String desc;
    private long runningTime;
    private String location;
    private Set<String> adminMacs;
    private Map<String, Set<String>> macIpIndex;
    private String username;
    private String password;
    private Socket clientSocket;
    private boolean getipv4;
    private boolean getipv6;
    private Map<String, String> ipMacIndex;
    private int id;
    private String prompt = null;

    public RouterCiscoCLI(String ip, String username, String password) {
        this.ip = ip;
        this.username = username;
        this.password = password;
    }

    @Override
    public boolean getIpv4() {
        return getipv4;
    }

    @Override
    public void setIpv4(boolean doipv4) throws IOException {
        getipv4 = doipv4;
    }

    @Override
    public boolean getIpv6() {
        return getipv6;
    }

    @Override
    public void setIpv6(boolean doipv6) throws IOException {
        getipv6 = doipv6;
    }

    @Override
    public Set<String> getIpByMac(String mac) {
        return macIpIndex.get(mac);
    }

    @Override
    public String getMacByIp(String ip) {
        return ipMacIndex.get(ip);
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
        return null;
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
    public void run() {
        long starttime = System.currentTimeMillis();
        status = Device.STATUS_RUNNING;
        try {
            InetAddress address = InetAddress.getByName(ip);
            if (!address.isReachable(1000)) {
                statusString = "not reachable? ip: " + ip;
                status = Device.STATUS_FAILED;
                KammkalaLogger.log(KammkalaLogger.ERROR, RouterCiscoCLI.class.getName(), statusString, null);
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
            KammkalaLogger.log(KammkalaLogger.ERROR, RouterCiscoCLI.class.getName(), statusString, null);
            return;
        }
        
        try {
            makeConnection();
            readMyMacs();
            macIpIndex = new HashMap<String, Set<String>>();
            if (getipv4) {
                clientSocket.getOutputStream().write("show arp\n".getBytes());
                String output = readOutPutToPrompt();
                String[] lines = output.split("\n");
                for (int c = 0; c < lines.length; c++) {
                    String[] line = lines[c].split("\\s+");

                    if (line.length > 3
                            && line[1].matches("\\d+\\.\\d+\\.\\d+\\.\\d+")
                            && line[3].replace(".", "").toUpperCase().matches("[\\d[A-F]]{12}")) {
                        String ip2 = line[1].trim();
                        String mac = line[3].replace(".", "").trim().replaceFirst("^(..)(..)(..)(..)(..)(..)$",
                                "$1:$2:$3:$4:$5:$6").toLowerCase();

                        Set<String> ips = macIpIndex.get(mac);
                        if (ips == null) {
                            ips = new HashSet<String>();
                            macIpIndex.put(mac, ips);
                        }
                        ips.add(ip2);
                    }
                }
            }
            if (getipv6) {
                clientSocket.getOutputStream().write("show ipv6 neighbors\n".getBytes());
                String output = readOutPutToPrompt();
                long time = System.currentTimeMillis();
                String[] lines = output.split("\n");
                for (int c = 0; c < lines.length; c++) {
                    String[] line = lines[c].split("\\s+");
                    if (line.length > 2
                            && line[0].toUpperCase().matches("^[\\d[A-F]:]+$")) {
                        StringBuilder sb = new StringBuilder();
                        String[] ipv6address = line[0].toUpperCase().split(":");
                        for (int d = 0; d < ipv6address.length; d++) {
                            int len;
                            if ((len = ipv6address[d].length()) > 0) {
                                while (4 - len > 0) {
                                    sb.append("0");
                                    len++;
                                }
                                sb.append(ipv6address[d]);
                                sb.append(":");
                            } else {
                                // :: lahendamine
                                int iplen = ipv6address.length;
                                while (8 - iplen >= 0) {
                                    sb.append("0000:");
                                    iplen++;
                                }
                            }
                        }
                        sb.deleteCharAt(sb.length() - 1);
                        String ip2 = sb.toString().toLowerCase();
                        String mac = line[2].replace(".", "").trim().replaceFirst("^(..)(..)(..)(..)(..)(..)$",
                                "$1:$2:$3:$4:$5:$6").toLowerCase();

                        Set<String> ips = macIpIndex.get(mac);
                        if (ips == null) {
                            ips = new HashSet<String>();
                            macIpIndex.put(mac, ips);
                        }
                        ips.add(ip2);

                    }

                }
            }
            clientSocket.close();
        } catch (IOException ex) {
            statusString = ip + ": " + ex.getMessage();
            status = Device.STATUS_FAILED;
            KammkalaLogger.log(KammkalaLogger.ERROR, RouterCiscoCLI.class.getName(), statusString, null);
            return;
        }

        calculateIpMacIndex();
        
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

    private void calculateIpMacIndex() {
        ipMacIndex = new HashMap<String, String>();
        Iterator<String> mi = macIpIndex.keySet().iterator();
        while (mi.hasNext()) {
            String mac = mi.next();
            Set<String> ips = macIpIndex.get(mac);
            Iterator<String> ipsi = ips.iterator();
            while (ipsi.hasNext()) {
                String ip2 = ipsi.next();
                ipMacIndex.put(ip2, mac);
            }
        }
    }

    @Override
    public int getId() {
        return id;
    }
    
    @Override
    public Set<String> getIps() {
        return ipMacIndex.keySet();
    }
}

