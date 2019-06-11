package ee.ut.merzin.kammkala2.driver;

import ee.ut.merzin.kammkala2.helpers.KammkalaLogger;
import ee.ut.merzin.kammkala2.snmp.SNMP;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.snmp4j.CommunityTarget;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;

/**
 * Model for network switch. Downloads info from actual device using snmp requests.
 * @author markko
 */
public class SwitchSNMP implements Switch {

    private String ip;
    private String community;
    private CommunityTarget target;
    private int status;
    private String statusString;
    private String name;
    private String desc;
    private String location;
    private Set<String> adminMacs;
    private Map<String, Port> indexPort;
    private Set<String> vlans;
    private long runningTime;
    private int id;

    /** Model and crawler for network switch. */
    public SwitchSNMP(String ip, String community) {
        this.ip = ip;
        this.community = community;
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
        throw new UnsupportedOperationException("Not supported yet.");
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
    public Set<Port> getPorts() {
        if (indexPort != null) {
            return new TreeSet<Port>(indexPort.values());
        } else {
            return new TreeSet<Port>();
        }
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
            return;
        } catch (IOException ex) {
            statusString = ip + ": " + ex.getMessage();
            status = Device.STATUS_FAILED;
            return;
        }

        status = Device.STATUS_RUNNING;

        target = new CommunityTarget();
        target.setCommunity(new OctetString(community));
        target.setAddress(new UdpAddress(ip + "/161"));
        target.setTimeout(5000);
        target.setVersion(SnmpConstants.version2c);

        try {
            name = SNMP.get("1.3.6.1.2.1.1.5.0", target);
            if (name == null) {
                target.setVersion(SnmpConstants.version1);
                name = SNMP.get("1.3.6.1.2.1.1.5.0", target);
                if (name == null) {
                    statusString = "Can not get system name. Wrong community string? ip: " + ip;
                    status = Device.STATUS_FAILED;
                    return;
                }
            }

            desc = SNMP.get("1.3.6.1.2.1.1.1.0", target);
            location = SNMP.get("1.3.6.1.2.1.1.6.0", target);
            adminMacs = SNMP.walk2set(SNMP.bulkwalk("1.3.6.1.2.1.2.2.1.6", target));
            String dtype = SNMP.get("1.3.6.1.2.1.1.2.0", target);
            if (dtype != null && dtype.startsWith("1.3.6.1.4.1.9.1.")) {
                ciscoRunner();
            } else {
                genericRunner();
            }

        } catch (IOException ex) {
            statusString = ip + ": " + ex.getMessage();
            status = Device.STATUS_FAILED;
            KammkalaLogger.log(KammkalaLogger.DEBUG, Switch.class.getName(), ex);
            return;
        }

        removeMyMacs();

        removeMulticastMacs();

        statusString = "OK";
        status = Device.STATUS_RESULT_READY;
        runningTime = System.currentTimeMillis() - starttime;

    }

    private void ciscoRunner() throws IOException {
        Set<String> vlanCommunities = SNMP.walk2set(SNMP.bulkwalk("1.3.6.1.2.1.47.1.2.1.1.4", target));
        if (vlanCommunities == null) {
            vlanCommunities = new HashSet<String>();
        }
        if (vlanCommunities.isEmpty()) {
            vlanCommunities.add(community);
        }

        vlans = new HashSet<String>();
        Iterator<String> ci = vlanCommunities.iterator();
        while (ci.hasNext()) {
            String vlc = ci.next();
            if (vlc.contains("@")) {
                String vlan = vlc.substring(vlc.indexOf("@") + 1);
                if (vlan != null && vlan.length() > 0) {
                    vlans.add(vlan);
                }
            }
        }
        if (vlans.isEmpty()) {
            vlans = null;
        }

        indexPort = new HashMap<String, Port>();

        Iterator<String> vlanci = vlanCommunities.iterator();
        while (vlanci.hasNext()) {
            String vlanc = vlanci.next();
            String vlan = null;
            if (vlanc.contains("@")) {
                vlan = vlanc.substring(vlanc.indexOf("@") + 1);
            }

            CommunityTarget newtarget = new CommunityTarget();
            newtarget.setCommunity(new OctetString(vlanc));
            newtarget.setAddress(target.getAddress());
            newtarget.setVersion(target.getVersion());
            newtarget.setTimeout(2000);
            newtarget.setRetries(3);

            String[][] portindexes = SNMP.bulkwalk("1.3.6.1.2.1.17.1.4.1.1", newtarget);
            if (portindexes == null) {
                throw new IOException("portindexes timed out");
            }
            for (int c = 0; c < portindexes.length; c++) {
                String portIndex = portindexes[c][1];
                try {
                    String medIndex = SNMP.get("1.3.6.1.2.1.17.1.4.1.2." + portIndex, newtarget);
                    String portName = SNMP.get("1.3.6.1.2.1.31.1.1.1.1." + medIndex, newtarget);
                    Port port = new Port(portIndex, portName, this);
                    indexPort.put(portIndex, port);
                } catch (NumberFormatException e) {
                    System.err.println(name);
                    e.printStackTrace();
                }
            }
        }

        vlanci = vlanCommunities.iterator();
        while (vlanci.hasNext()) {
            String vlanc = vlanci.next();
            String vlan = null;
            if (vlanc.contains("@")) {
                vlan = vlanc.substring(vlanc.indexOf("@") + 1);
            }

            CommunityTarget newtarget = new CommunityTarget();
            newtarget.setCommunity(new OctetString(vlanc));
            newtarget.setAddress(new UdpAddress(ip + "/161"));
            newtarget.setVersion(SnmpConstants.version2c);

            String[][] oidmac = SNMP.bulkwalk("1.3.6.1.2.1.17.4.3.1.1", newtarget);
            Map<String, String> oidportmap = SNMP.walk2map(SNMP.bulkwalk("1.3.6.1.2.1.17.4.3.1.2", newtarget));
            if (oidmac == null || oidportmap == null) {
                throw new IOException("ciscorunner oidmac or oidportmap null");
            }
            for (int c = 0; c < oidmac.length; c++) {
                if (oidmac[c][0] != null && oidmac[c][1] != null) {
                    String portIndex = oidportmap.get(oidmac[c][0]);
                    if (portIndex != null) {
                        Port p = indexPort.get(portIndex);
                        if (p != null) {
                            p.addMac(oidmac[c][1], vlan);
                        }
                    }
                }
            }
        }

    }

    private void genericRunner() throws IOException {
        String[][] portindexes = SNMP.bulkwalk("1.3.6.1.2.1.17.1.4.1.1", target);
        if (portindexes == null) {
            throw new IOException("portindexes failed!");
        }
        indexPort = new HashMap<String, Port>();
        for (int c = 0; c < portindexes.length; c++) {
            String portIndex = portindexes[c][1];
            if (portIndex == null) {
                throw new IOException("portindex failed!");
            }
            String medIndex = SNMP.get("1.3.6.1.2.1.17.1.4.1.2." + portIndex, target);
            if (medIndex == null) {
                throw new IOException("medindex failed!");
            }
            String portName = SNMP.get("1.3.6.1.2.1.31.1.1.1.1." + medIndex, target);
            if (portName == null || portName.trim().isEmpty()) {
                portName = portIndex;
            }
            Port port = new Port(portIndex, portName, this);
            indexPort.put(portIndex, port);
        }

        String[][] oidmac = SNMP.bulkwalk("1.3.6.1.2.1.17.4.3.1.1", target);
        Map<String, String> oidportmap = SNMP.walk2map(SNMP.bulkwalk("1.3.6.1.2.1.17.4.3.1.2", target));
        if (oidmac == null || oidportmap == null) {
            throw new IOException("macwalk timed out");
        }
        for (int c = 0; c < oidmac.length; c++) {
            if (oidmac[c][0] != null && oidmac[c][1] != null) {
                String portIndex = oidportmap.get(oidmac[c][0]);
                if (portIndex != null) {
                    Port p = indexPort.get(portIndex);
                    if (p != null) {
                        p.addMac(oidmac[c][1], null);
                    }
                }
            }
        }

    }

    /** Some switches list their own macs in ports. Remove them. */
    private void removeMyMacs() {
        Iterator<Port> pi = indexPort.values().iterator();
        while (pi.hasNext()) {
            Port p = pi.next();
            Set<String> macs = p.getMacs(null);
            if (macs != null && getMyMacs() != null) {
                macs.removeAll(getMyMacs());
            }
            if (vlans != null) {
                Iterator<String> vlani = vlans.iterator();
                while (vlani.hasNext()) {
                    String vlan = vlani.next();
                    Set<String> macsv = p.getMacs(vlan);
                    if (macsv != null) {
                        macsv.removeAll(getMyMacs());
                    }
                }
            }
        }
    }

    /** Remove multicast and local macs. */
    private void removeMulticastMacs() {
        Iterator<Port> pi = indexPort.values().iterator();
        while (pi.hasNext()) {
            Port p = pi.next();
            Set<String> macs = p.getMacs(null);
            if (macs != null) {
                Iterator<String> macsi = macs.iterator();
                while (macsi.hasNext()) {
                    String mac = macsi.next();
                    String secondByte = mac.substring(1, 2).toLowerCase();
                    try {
                        if ((Integer.parseInt(secondByte, 16) & 3) != 0) {
                            macsi.remove();
                        }
                    } catch (NumberFormatException ex) {
                        macsi.remove();
                    }
                }
            }
            if (vlans != null) {
                Iterator<String> vlani = vlans.iterator();
                while (vlani.hasNext()) {
                    String vlan = vlani.next();
                    Set<String> macsv = p.getMacs(vlan);
                    if (macsv != null) {
                        Iterator<String> macsi = macsv.iterator();
                        while (macsi.hasNext()) {
                            String mac = macsi.next();
                            String secondByte = mac.substring(1, 2).toLowerCase();
                            try {
                                if ((Integer.parseInt(secondByte, 16) & 3) != 0) {
                                    macsi.remove();
                                }
                            } catch (NumberFormatException ex) {
                                macsi.remove();
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public Set<String> getMyMacs() {
        return adminMacs;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public Set<String> getVlans() {
        return vlans;
    }
}
