package ee.ut.merzin.kammkala2.test;

import ee.ut.merzin.kammkala2.driver.Router;
import ee.ut.merzin.kammkala2.driver.RouterCiscoCLI;
import ee.ut.merzin.kammkala2.driver.Switch;
import ee.ut.merzin.kammkala2.driver.SwitchCiscoCli;
import ee.ut.merzin.kammkala2.driver.SwitchSNMP;
import java.io.IOException;

/**
 *
 * @author markko
 */
public class TestMain {
    public static void main(String[] args) throws InterruptedException, IOException {
//        CommunityTarget target = new CommunityTarget();
//        target.setCommunity(new OctetString("muudetud"));
//        target.setAddress(new UdpAddress("ak-gw.ut.ee/161"));
//        target.setVersion(SnmpConstants.version2c);
//
//        try {
//            String result = SNMP.get("1.3.6.1.2.1.1.5.0", target);
//            System.out.println(result);
//            
//            String[][] macoids = SNMP.bulkwalk("1.3.6.1.2.1.4.22.1.2", target);
//            HashMap<String,String> oidMacIndex = new HashMap<String,String>((int) (macoids.length / 0.7), 0.75f);
//            for (int c = 0; c < macoids.length; c++) {
//                oidMacIndex.put(macoids[c][0], macoids[c][1]);
//            }
//            
//            String[][] ipoids = SNMP.bulkwalk("1.3.6.1.2.1.4.22.1.3", target);
//            for (int c = 0; c < ipoids.length; c++) {
//                String ip = ipoids[c][1];
//                String mac = oidMacIndex.get(ipoids[c][0]);
//                System.out.println(ip + " -> " + mac);
//            }
//            
//        } catch (IOException ex) {
//            Logger.getLogger(TestMain.class.getName()).log(Level.SEVERE, null, ex);
//        }
        
//        Switch ts = new Switch("10.0.3.10", "muudetud");
//        Thread tst = new Thread(ts);
//        tst.start();
//        try {
//            tst.join();
//        } catch (InterruptedException ex) {
//            Logger.getLogger(TestMain.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        
//        System.out.println(ts.getPorts());
        
//          Router r = new RouterCiscoCLI("ak-gw.ut.ee", "kammkala", "d00ra3anna");
//          r.setIpv4(true);
//          r.setIpv6(true);
//          Thread rt = new Thread(r);
//          rt.start();
//          rt.join();
        
        Switch s = new SwitchCiscoCli("10.0.3.7", "kammkala", "secret");
        Thread st = new Thread(s);
        st.start();
        st.join();
        System.out.println(s);
        
    }
}
