package ee.ut.merzin.kammkala2.controller;

import ee.ut.merzin.kammkala2.driver.DNSResolver;
import ee.ut.merzin.kammkala2.driver.Device;
import ee.ut.merzin.kammkala2.driver.Router;
import ee.ut.merzin.kammkala2.driver.Switch;
import ee.ut.merzin.kammkala2.helpers.Config;
import ee.ut.merzin.kammkala2.helpers.KammkalaLogger;
import ee.ut.merzin.kammkala2.logic.Topology;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Main loop. */
public class MainPulseController {

    public static final int PULSEDIV = 5;
    public static final int NUMTHREADS = 20;
    static int currentpulse = -1; // who could think that pulse lasts less then minute? :)

    /** Main loop.
     * @param args
     */
    public static void main(String[] args) {

        System.setProperty("networkaddress.cache.ttl", "10");
        System.setProperty("networkaddress.cache.negative.ttl", "10");
        try {
            Config.loadBootstrapConfig(args[0]);
        } catch (Exception ex) {
            System.err.println("Configfile as a first parameter!");
            KammkalaLogger.log2StdErr(KammkalaLogger.FATAL, MainPulseController.class.getName(), ex.getMessage(), null);
            System.exit(1);
        }

        KammkalaLogger.log(KammkalaLogger.DEBUG, MainPulseController.class.getName(), "Startup... Bootstrap loaded.", null);

        // DeviceController.setDebug(true);

        while (true) {

            Timestamp pulseStart = new Timestamp(System.currentTimeMillis());



            try {

                KammkalaLogger.log(KammkalaLogger.DEBUG, MainPulseController.class.getName(), "Pulse start.", null);

                Config.getConfig().loadConfig();

                KammkalaLogger.log(KammkalaLogger.DEBUG, MainPulseController.class.getName(), "Running config loaded.", null);

                List<Router> routers = DeviceController.getRouters();
                List<Switch> switches = DeviceController.getSwitches();
                // List<Switch> switches = (List<Switch>) listReadUp("/tmp/switches.ser");

                DeviceController.pingDevices(routers);
                DeviceController.pingDevices(switches);

                KammkalaLogger.log(KammkalaLogger.DEBUG, MainPulseController.class.getName(), "Devices loaded and pinged.", null);

                KammkalaLogger.log(KammkalaLogger.DEBUG, MainPulseController.class.getName(), "Launcing routers.", null);

                DeviceController.setCurrentRouters(routers);
                ExecutorService pool = Executors.newFixedThreadPool(NUMTHREADS);
                Iterator<Router> ri = routers.iterator();
                while (ri.hasNext()) {
                    Router r = ri.next();
                    pool.execute(r);
                }
                pool.shutdown();
                pool.awaitTermination(10, TimeUnit.MINUTES);
                DeviceController.updateRouterStatus(routers);

                ri = routers.iterator();
                while (ri.hasNext()) {
                    Router r = ri.next();
                    if (r.getStatus() != Device.STATUS_RESULT_READY) {
                        ri.remove();
                    }
                }
                
                KammkalaLogger.log(KammkalaLogger.DEBUG, MainPulseController.class.getName(), "Routers done.", null);
                
                KammkalaLogger.log(KammkalaLogger.DEBUG, MainPulseController.class.getName(), "Launching dns resolve.", null);

                Set<String> ips = new HashSet<String>();
                ri = routers.iterator();
                while (ri.hasNext()) {
                    Router r = ri.next();
                    ips.addAll(r.getIps());
                }

                DNSResolver dns = new DNSResolver(ips);
                Thread dnsth = new Thread(dns);
                dnsth.start();

                // MassClientPinger.pingAllKnowClients(routers);

                // Thread.sleep(1000);
                
                KammkalaLogger.log(KammkalaLogger.DEBUG, MainPulseController.class.getName(), "Launching switches.", null);

                Iterator<Switch> si = switches.iterator();

                pool = Executors.newFixedThreadPool(NUMTHREADS);



                while (si.hasNext()) {
                    Switch s = si.next();
                    pool.execute(s);
                }
                pool.shutdown();
                pool.awaitTermination(10, TimeUnit.MINUTES);
                DeviceController.updateSwitchStatus(switches);



                si = switches.iterator();
                while (si.hasNext()) {
                    Switch s = si.next();
                    if (s.getStatus() != Device.STATUS_RESULT_READY) {
                        si.remove();
                    }
                }
                
                KammkalaLogger.log(KammkalaLogger.DEBUG, MainPulseController.class.getName(), "Switches done.", null);

                dnsth.join(5 * 60 * 1000);
                
                KammkalaLogger.log(KammkalaLogger.DEBUG, MainPulseController.class.getName(), "Dns resolve done.", null);
                
                KammkalaLogger.log(KammkalaLogger.DEBUG, MainPulseController.class.getName(), "Launching topology.", null);

                Topology.mapTopology(switches, pulseStart);
                
                KammkalaLogger.log(KammkalaLogger.DEBUG, MainPulseController.class.getName(), "Topology done.", null);
                
                KammkalaLogger.log(KammkalaLogger.DEBUG, MainPulseController.class.getName(), "Launching storage to db.", null);

                StorageController.store(switches, routers, dns, pulseStart);
                StorageController.storeRawSwitches(switches, pulseStart);
                StorageController.storeRawArp(routers, pulseStart);
                
                KammkalaLogger.log(KammkalaLogger.DEBUG, MainPulseController.class.getName(), "Storage to db done.", null);

            } catch (Throwable e) {
                KammkalaLogger.log(KammkalaLogger.ERROR, e.getMessage(), e);
            }

            KammkalaLogger.log(KammkalaLogger.DEBUG, MainPulseController.class.getName(), "Starting garbage collector.", null);
            System.gc();
            KammkalaLogger.log(KammkalaLogger.DEBUG, MainPulseController.class.getName(), "Garbage collector done.", null);
            KammkalaLogger.log(KammkalaLogger.DEBUG, MainPulseController.class.getName(), "Pulse done! Sleeping to next pulse.", null);
            sleepToNextPulse();

        }
    }

    private static void sleepToNextPulse() {
        while (true) {
            SimpleDateFormat sdf = new SimpleDateFormat("mm");
            String dates = sdf.format(new Date(System.currentTimeMillis()));
            int minut = Integer.parseInt(dates);
            if (currentpulse != minut && minut % MainPulseController.PULSEDIV == 0) {
                currentpulse = minut;
                return;
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException ex) {
            }
        }
    }
}
