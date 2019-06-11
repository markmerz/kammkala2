package ee.ut.merzin.kammkala2.driver;

import java.io.IOException;
import java.util.Set;

/** Interface to router device. */
public interface Router extends Device {
	public static final int TYPE_SNMP = 1;
	public static final int TYPE_CISCOCLI = 2;
	public static final int IPV4 = 1;
	public static final int IPV6 = 2;
	
	public abstract boolean getIpv4();
	public abstract void setIpv4(boolean doipv4) throws IOException;
	public abstract boolean getIpv6();
	public abstract void setIpv6(boolean doipv6) throws IOException;
	
        public abstract Set<String> getIpByMac(String mac);
        public abstract String getMacByIp(String ip);
        public abstract Set<String> getIps();
}