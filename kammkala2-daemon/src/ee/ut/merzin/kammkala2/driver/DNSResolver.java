
package ee.ut.merzin.kammkala2.driver;

import ee.ut.merzin.kammkala2.helpers.KammkalaLogger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Manages DNS resolving.
 * @author markko
 */
public class DNSResolver implements Runnable {
    private Set<String> ips;
    private Map<String,String> ipName = Collections.synchronizedMap(new HashMap<String,String>());
    
    public DNSResolver(Set ipss) {
        ips = ipss;
        
    }
    
    @Override
    public void run() {
        ExecutorService pool = null;
        try {
            pool = Executors.newFixedThreadPool(20);
            Iterator<String> ipi = ips.iterator();
            while (ipi.hasNext()) {
                DNSResolverWorker rw = new DNSResolverWorker(ipi.next(), ipName);
                pool.submit(rw);
            }
            
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            KammkalaLogger.log(KammkalaLogger.ERROR, DNSResolver.class.getName(), ex);
            pool.shutdownNow();
        }
        
    }
    
    public String getNameByIP(String ip) {
        return ipName.get(ip);
    }
    
}

class DNSResolverWorker implements Runnable {
    private String ip;
    private Map<String, String> ipName;
    DNSResolverWorker(String ip, Map<String,String> ipName) {
        this.ip = ip;
        this.ipName = ipName;
    }

    @Override
    public void run() {
        try {
            InetAddress ia = InetAddress.getByName(ip);
            if (ia != null && ia.getCanonicalHostName() != null && !ia.getCanonicalHostName().equals(ip)) {
                ipName.put(ip, ia.getCanonicalHostName());
            }
        } catch (UnknownHostException ex) {
            
        }
    }
}