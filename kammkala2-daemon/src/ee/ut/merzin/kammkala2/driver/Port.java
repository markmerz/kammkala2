package ee.ut.merzin.kammkala2.driver;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Model for switch port.
 * @author markko
 */
public class Port implements Comparable<Port>, Serializable {
    public static final int NORMAL = 0;
    public static final int ACCESS = 0;
    public static final int LINK = 1;
    public static final int CLOUD = 2;
    
    private String index;
    private String name;
    private Switch mySwitch;
    private Map<String, Set<String>> vlanMacs;
    private Set<String> macs;
    private int type = Port.ACCESS;
    
    public int floatingcount = 0; // we use this field later in topology algorithm, for detecting floating ports.
    
    public Port(String index, String name, Switch mySwitch) {
        this.index = index;
        this.name = name;
        this.mySwitch = mySwitch;
    }
    
    /** Return list of MAC-addresses in that port. */
    public Set<String> getMacs(String vlan) {
        if (vlan == null || vlanMacs == null) {
            return macs;
        } else {            
            return vlanMacs.get(vlan);
        }
    }
    
    @Override
    public boolean equals (Object o) {
        if (o instanceof Port) {
            Port p = (Port) o;
            if (p.getIndex().equals(index) && p.getSwitch().equals(mySwitch) ) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + (this.index != null ? this.index.hashCode() : 0);
        hash = 37 * hash + (this.mySwitch != null ? this.mySwitch.hashCode() : 0);
        return hash;
    }
    
    @Override
    public String toString() {
        // return mySwitch + ":" + name + "(" + (macs != null ? macs.size() : "-") + ")";
        return mySwitch + ":" + name;
    }

    public String getIndex() {
        return index;
    }

    public Switch getSwitch() {
        return mySwitch;
    }

    /** Adds MAC-address to thet port. */
    public void addMac(String mac, String vlan) {
        if (vlan != null) {
            if (vlanMacs == null) {
                vlanMacs = new HashMap<String, Set<String>>();
            }
            Set<String> macss = vlanMacs.get(vlan);
            if (macss == null) {
                macss = new HashSet<String>();
                vlanMacs.put(vlan, macss);
            }
            macss.add(mac);
        }
        
        if (macs == null) {
            macs = new HashSet<String>();
        }
        macs.add(mac);
    }

    @Override
    public int compareTo(Port o) {
        return name.compareTo(o.getName());
    }

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
   
}
