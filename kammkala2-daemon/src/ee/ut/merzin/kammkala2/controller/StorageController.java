package ee.ut.merzin.kammkala2.controller;

import ee.ut.merzin.kammkala2.driver.DNSResolver;
import ee.ut.merzin.kammkala2.driver.Port;
import ee.ut.merzin.kammkala2.driver.Router;
import ee.ut.merzin.kammkala2.driver.Switch;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Manages storage of collected information to database.
 * @author markko
 */
public class StorageController {

    /** Strores collected correlation ifo to database. */
    public static void store(List<Switch> switches, List<Router> routers, DNSResolver dns, Timestamp pulseStart) throws Exception {

        Connection conn = DataBaseConnection.getInstance().getConnection();
        conn.setAutoCommit(false);
        PreparedStatement pstmt_correlations = conn.prepareStatement("insert into correlations (ip, mac, switch, port, time, name, syslocation) values (?, ?, ?, ?, ?, ?, ?)");

        Iterator<Switch> si = switches.iterator();
        while (si.hasNext()) {
            Switch s = si.next();
            String switchName = s.getName();
            String syslocation = s.getLocation();
            Iterator<Port> spi = s.getPorts().iterator();
            while (spi.hasNext()) {
                Port p = spi.next();
                if (p.getType() == Port.NORMAL) {
                    String portName = p.getName();
                    if (p.getMacs(null) != null) {
                        Iterator<String> mi = p.getMacs(null).iterator();
                        while (mi.hasNext()) {
                            String mac = mi.next();
                            Set<String> ips = getIpByMac(mac, routers);
                            if (ips != null && ips.size() > 0) {
                                Iterator<String> ipi = ips.iterator();
                                while (ipi.hasNext()) {
                                    String ip = ipi.next();
                                    pstmt_correlations.setString(1, ip);
                                    pstmt_correlations.setString(2, mac);
                                    pstmt_correlations.setString(3, switchName);
                                    pstmt_correlations.setString(4, portName);
                                    pstmt_correlations.setTimestamp(5, pulseStart);
                                    pstmt_correlations.setString(6, dns.getNameByIP(ip));
                                    pstmt_correlations.setString(7, syslocation);
                                    pstmt_correlations.executeUpdate();
                                }
                            } else {
                                pstmt_correlations.setString(1, null);
                                pstmt_correlations.setString(2, mac);
                                pstmt_correlations.setString(3, switchName);
                                pstmt_correlations.setString(4, portName);
                                pstmt_correlations.setTimestamp(5, pulseStart);
                                pstmt_correlations.setString(6, null);
                                pstmt_correlations.setString(7, syslocation);
                                pstmt_correlations.executeUpdate();
                            }
                        }
                    }
                }
            }
        }

        conn.commit();
        pstmt_correlations.close();
        // conn.close();


    }

    private static Set<String> getIpByMac(String mac, List<Router> routers) {
        Set<String> ips = new HashSet<String>();
        Iterator<Router> ri = routers.iterator();
        while (ri.hasNext()) {
            Router r = ri.next();
            Set<String> ipss = r.getIpByMac(mac);
            if (ipss != null) {
                ips.addAll(ipss);
            }
        }
        return ips;
    }

    /** Stores current topology to database. */
    public static void storeTopology(String exportTopology, Timestamp pulseStart) throws Exception {

        Connection conn = DataBaseConnection.getInstance().getConnection();
        conn.setAutoCommit(false);
        PreparedStatement instop = conn.prepareStatement("insert into topology (topology, time) values (?, ?)");
        instop.setString(1, exportTopology);
        instop.setTimestamp(2, pulseStart);
        instop.executeUpdate();
        conn.commit();
        instop.close();
    }

    /** Stores collected raw information to database. */
    public static void storeRawSwitches(List switches, Timestamp pulseStart) throws Exception {
        Connection conn = DataBaseConnection.getInstance().getConnection();
        conn.setAutoCommit(false);
       
        PreparedStatement insertSwitch = conn.prepareStatement("insert into raw_switches (name, time, id) values (?, ?, ?)");
        PreparedStatement insertPort = conn.prepareStatement("insert into raw_switchports (switchid, time, portname, porttype) values (?, ?, ?, ?)");
        PreparedStatement insertMac = conn.prepareStatement("insert into raw_macs (portname, time, mac, vlan, switchid) values (?, ?, ?, ?, ?)");

        Iterator<Switch> si = switches.iterator();
        while (si.hasNext()) {
            Switch s = si.next();
            String name = s.getName();
            int switchid = s.getId();
            insertSwitch.setString(1, name);
            insertSwitch.setTimestamp(2, pulseStart);
            insertSwitch.setInt(3, switchid);
            insertSwitch.executeUpdate();

            Iterator<Port> pi = s.getPorts().iterator();
            while (pi.hasNext()) {
                Port p = pi.next();
                String portname = p.getName();
                int porttype = p.getType();
                insertPort.setInt(1, switchid);
                insertPort.setTimestamp(2, pulseStart);
                insertPort.setString(3, portname);
                insertPort.setInt(4, porttype);                
                insertPort.executeUpdate();

                Set<String> vlans = s.getVlans();
                if (vlans == null) {
                    Set<String> ms = p.getMacs(null);
                    if (ms != null) {
                        Iterator<String> mi = ms.iterator();
                        while (mi.hasNext()) {
                            String mac = mi.next();
                            insertMac.setString(1, portname);
                            insertMac.setTimestamp(2, pulseStart);
                            insertMac.setString(3, mac);
                            insertMac.setString(4, "DEFAULT");
                            insertMac.setInt(5, switchid);
                            insertMac.executeUpdate();
                        }
                    }
                } else {
                    Iterator<String> vlani = vlans.iterator();
                    while (vlani.hasNext()) {
                        String vlan = vlani.next();
                        Set<String> ms = p.getMacs(vlan);
                        if (ms != null) {
                            Iterator<String> mi = ms.iterator();
                            while (mi.hasNext()) {
                                String mac = mi.next();
                                insertMac.setString(1, portname);
                                insertMac.setTimestamp(2, pulseStart);
                                insertMac.setString(3, mac);
                                insertMac.setString(4, vlan);
                                insertMac.setInt(5, switchid);
                                insertMac.executeUpdate();
                            }
                        }
                    }
                }


            }
        }


        conn.commit();
        insertSwitch.close();
        insertPort.close();
        insertMac.close();
    }

    /** Stores raw info from routers to database. */
    public static void storeRawArp(List<Router> routers, Timestamp pulseStart) throws Exception {
        Connection conn = DataBaseConnection.getInstance().getConnection();
        conn.setAutoCommit(false);


        PreparedStatement insertArp = conn.prepareStatement("insert into raw_arp (routername, time, ip, mac) values (?, ?, ?, ?)");
        
        if (routers != null) {
            Iterator<Router> ri = routers.iterator();
            while (ri.hasNext()) {
                Router r = ri.next();
                String routername = r.getName();
                Set<String> ips = r.getIps();
                if (ips != null) {
                    Iterator<String> ipi = ips.iterator();
                    while (ipi.hasNext()) {
                        String ip = ipi.next();
                        String mac = r.getMacByIp(ip);
                        insertArp.setString(1, routername);
                        insertArp.setTimestamp(2, pulseStart);
                        insertArp.setString(3, ip);
                        insertArp.setString(4, mac);
                        insertArp.executeUpdate();
                    }
                }
            }
        }
        
        conn.commit();
        insertArp.close();

    }
}
