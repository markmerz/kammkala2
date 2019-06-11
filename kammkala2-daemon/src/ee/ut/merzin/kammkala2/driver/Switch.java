package ee.ut.merzin.kammkala2.driver;

import java.util.Set;

/**
 * Model for network switch.
 * @author markko
 */
public interface Switch extends Device {
    public static final int TYPE_SNMP = 1;
    public static final int TYPE_CISCOCLI = 2;
        
    Set<Port> getPorts();
    Set<String> getVlans();
}
