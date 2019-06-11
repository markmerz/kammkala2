package ee.ut.merzin.kammkala2.driver;

import ee.ut.merzin.kammkala2.helpers.KammkalaLogger;
import ee.ut.merzin.kammkala2.snmp.SNMP;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.snmp4j.CommunityTarget;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;

/**
 * Implements router device using snmp requests.
 * @author markko
 */
public class RouterSNMP implements Router {

    private final String ip;
    private final String community;
    private String statusString;
    private int status;
    private String name;
    private String desc;
    private long runningTime;
    private String location;
    private Set<String> adminMacs;
    private HashMap<String, Set<String>> macIpIndex;
    private HashMap<String, String> ipMacIndex;
    private int id;

    public RouterSNMP(String ip, String community) {
        this.ip = ip;
        this.community = community;
    }

    @Override
    public boolean getIpv4() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setIpv4(boolean doipv4) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getIpv6() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setIpv6(boolean doipv6) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
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
        return community;
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
        return null;
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
        try {
            InetAddress address = InetAddress.getByName(ip);
            if (!address.isReachable(1000)) {
                statusString = "not reachable? ip: " + ip;
                status = Device.STATUS_FAILED;
                return;
            }
        } catch (UnknownHostException ex) {
            statusString = "host unknown: " + ip;
            status = Device.STATUS_FAILED;
            KammkalaLogger.log(KammkalaLogger.ERROR, RouterSNMP.class.getName(), statusString, null);
            return;
        } catch (IOException ex) {
            statusString = ip + ": " + ex.getMessage();
            status = Device.STATUS_FAILED;
            KammkalaLogger.log(KammkalaLogger.ERROR, RouterSNMP.class.getName(), statusString, null);
            return;
        }

        status = Device.STATUS_RUNNING;

        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString(community));
        target.setAddress(new UdpAddress(ip + "/161"));
        target.setTimeout(1000);
        target.setVersion(SnmpConstants.version2c);

        try {
            name = SNMP.get("1.3.6.1.2.1.1.5.0", target);
            if (name == null) {
                target.setVersion(SnmpConstants.version1);
                name = SNMP.get("1.3.6.1.2.1.1.5.0", target);
                if (name == null) {
                    statusString = "Can not get system name. Wrong community string? ip: " + ip;
                    status = Device.STATUS_FAILED;
                    KammkalaLogger.log(KammkalaLogger.ERROR, RouterSNMP.class.getName(), statusString, null);
                    return;
                }
            }

            desc = SNMP.get("1.3.6.1.2.1.1.1.0", target);
            location = SNMP.get("1.3.6.1.2.1.1.6.0", target);
            adminMacs = SNMP.walk2set(SNMP.bulkwalk("1.3.6.1.2.1.2.2.1.6", target));

            macIpIndex = new HashMap<String, Set<String>>();
            String[][] oidmacs = SNMP.bulkwalk("1.3.6.1.2.1.4.22.1.2", target);
            Map<String, String> oidIpMap = SNMP.walk2map(SNMP.bulkwalk("1.3.6.1.2.1.4.22.1.3", target));
            if (oidmacs == null || oidIpMap == null) {
		throw new IOException("oid-mac table failed!");
            }

            for (int c = 0; c < oidmacs.length; c++) {
                String ip2 = oidIpMap.get(oidmacs[c][0]);
                if (ip2 != null) {
                    Set<String> ips = macIpIndex.get(oidmacs[c][1]);
                    if (ips == null) {
                        ips = new HashSet<String>();
                        macIpIndex.put(oidmacs[c][1], ips);
                    }
                    ips.add(ip2);
                }
            }

        } catch (IOException ex) {
            statusString = ip + ": " + ex.getMessage();
            status = Device.STATUS_FAILED;
            KammkalaLogger.log(KammkalaLogger.ERROR, RouterSNMP.class.getName(), statusString, null);
            return;
        }
        
        calculateIpMacIndex();
        
        statusString = "OK";
        status = Device.STATUS_RESULT_READY;
        runningTime = System.currentTimeMillis() - starttime;
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
