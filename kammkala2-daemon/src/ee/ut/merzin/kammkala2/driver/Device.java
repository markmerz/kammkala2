package ee.ut.merzin.kammkala2.driver;

import java.io.Serializable;
import java.util.Set;

/** Interface for information collectors. */
public interface Device extends Runnable, Serializable {
	public static final int STATUS_CREATED = 0;
	public static final int STATUS_RUNNING = 1;
	public static final int STATUS_RESULT_READY = 2;
	public static final int STATUS_FAILED = 3;
		
	public int getStatus();
	public String getStatusString();

	
	public abstract String getIp();
	public abstract String getComm();
	public abstract String getName();
	public abstract String getDesc();
	public abstract String getLocation();
	public abstract void setId(int id);
        public abstract int getId();
	public abstract String getPass();
        public abstract Set<String> getMyMacs();
        public abstract long getRunningTime();
	
}
