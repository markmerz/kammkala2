package ee.ut.merzin.kammkala2.logic;

import ee.ut.merzin.kammkala2.controller.DeviceController;
import ee.ut.merzin.kammkala2.controller.StorageController;
import ee.ut.merzin.kammkala2.driver.Port;
import ee.ut.merzin.kammkala2.driver.Switch;
import ee.ut.merzin.kammkala2.test.TopologyExporter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jgrapht.Graphs;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.Subgraph;

/**
 *
 * @author markko
 */
public class Topology implements Callable {

    private String vlan;
    private List<Switch> switches;

    public Topology(List<Switch> switches, String vlan) {
        this.switches = switches;
        this.vlan = vlan;
    }

    @Override
    public Object call() throws Exception {
        return Topology.calculateTopology(switches, vlan);
    }

    /** Controls topology maping threads. After this method finishes, link ports are marked as such. */
    public static void mapTopology(List<Switch> switches, Timestamp pulseStart) throws InterruptedException, ExecutionException, Exception {
        Set<String> vlans = new HashSet<String>();
        Iterator<Switch> si2 = switches.iterator();
        while (si2.hasNext()) {
            Switch s = si2.next();
            if (s.getVlans() != null) {
                vlans.addAll(s.getVlans());
            }
        }

        vlans.removeAll(DeviceController.getBannedVLANs());
        
        SimpleGraph<Object, DefaultEdge> finalTopology = new SimpleGraph<Object, DefaultEdge>(DefaultEdge.class);

        ExecutorService epool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        List<Future> results = new ArrayList<Future>();
        Iterator<String> vlansi = vlans.iterator();
        while (vlansi.hasNext()) {
            String vlan = vlansi.next();
            Topology topex = new Topology(switches, vlan);
            Future f = epool.submit(topex);
            results.add(f);
        }

//        Topology topex = new Topology(switches, null);
//        Future fu = epool.submit(topex);
//        results.add(fu);
        
        epool.shutdown();
        epool.awaitTermination(10, TimeUnit.MINUTES);

        Iterator<Future> fsi = results.iterator();
        while (fsi.hasNext()) {
            Future f = fsi.next();
            if (f != null) {
                SimpleGraph<Object, DefaultEdge> topology = (SimpleGraph<Object, DefaultEdge>) f.get();
                Graphs.addGraph(finalTopology, topology);
            }
        }

        //FIXME: test
        // SimpleGraph<Object, DefaultEdge> finalReducedTopology = LongestPathReduction.reduce(finalTopology);
        SimpleGraph<Object, DefaultEdge> finalReducedTopology = new SimpleGraph<Object, DefaultEdge>(DefaultEdge.class);
        Graphs.addGraph(finalReducedTopology, finalTopology);

        
        Set toRemove = new HashSet();
        Iterator vi = finalReducedTopology.vertexSet().iterator();
        while (vi.hasNext()) {
            Object v = vi.next();
            if (v instanceof Port) {
                if (finalReducedTopology.degreeOf(v) < 2) {
                    toRemove.add(v);
                } else {
                    ((Port) v).setType(Port.LINK);
                }
            }
        }
        finalReducedTopology.removeAllVertices(toRemove);


        Map<String, List<Port>> macPortIndex = new HashMap<String, List<Port>>();
        Iterator<Switch> swsi = switches.iterator();
        while (swsi.hasNext()) {
            Switch sw = swsi.next();
            Iterator<Port> psi = sw.getPorts().iterator();
            while (psi.hasNext()) {
                Port pr = psi.next();
                if (pr.getType() == Port.ACCESS) {
                    Set ms = pr.getMacs(null);
                    if (ms != null) {
                        Iterator<String> msi = ms.iterator();
                        while (msi.hasNext()) {
                            String m = msi.next();
                            List ports = macPortIndex.get(m);
                            if (ports == null) {
                                ports = new ArrayList<Port>();
                                macPortIndex.put(m, ports);
                            }
                            ports.add(pr);
                            pr.floatingcount++;
                        }
                    }
                }
            }
        }


        Set<Port> floatingPortsSet = new HashSet<Port>();
        Iterator<String> mi = macPortIndex.keySet().iterator();
        while (mi.hasNext()) {
            String m = mi.next();
            List<Port> ports = macPortIndex.get(m);
            if (ports.size() > 1) {
                Iterator<Port> psi = ports.iterator();
                Port minFloating = null;
                while (psi.hasNext()) {
                    Port p = psi.next();
                    if (minFloating == null || p.floatingcount < minFloating.floatingcount) {
                        minFloating = p;
                    }
                }
                ports.remove(minFloating);
                floatingPortsSet.addAll(ports);
            }
        }

        Iterator<Port> fpi = floatingPortsSet.iterator();
        while (fpi.hasNext()) {
            Port fp = fpi.next();
            fp.setType(Port.CLOUD);

            if (!finalReducedTopology.containsVertex(fp.getSwitch())) {
                finalReducedTopology.addVertex(fp.getSwitch());
            }

            if (!finalReducedTopology.containsVertex(fp)) {
                finalReducedTopology.addVertex(fp);
            }

            if (!finalReducedTopology.containsEdge(fp.getSwitch(), fp)) {
                finalReducedTopology.addEdge(fp.getSwitch(), fp);
            }
        }

        // System.out.println("ports floating: " + floatingPortsSet);
        components:
        while (true) {
            ConnectivityInspector ci = new ConnectivityInspector(finalReducedTopology);
            List<Set<Object>> conncomps = ci.connectedSets();
            Collections.sort(conncomps, new SetLenComparator());
            Set[] ccma = conncomps.toArray(new Set[0]); // no generics for reason
            for (int c = 0; c < ccma.length - 1; c++) {
                Set floatingPorts1 = SetOperations.intersection(ccma[c], floatingPortsSet);
                Set<String> macs1 = new HashSet<String>();
                Iterator vs1i = ccma[c].iterator();
                while (vs1i.hasNext()) {
                    Object v1 = vs1i.next();
                    if (v1 instanceof Switch) {
                        Switch sw1 = (Switch) v1;
                        Iterator<Port> psi = sw1.getPorts().iterator();
                        while (psi.hasNext()) {
                            Port p1 = psi.next();
                            if (p1.getType() == Port.ACCESS) {
                                if (p1.getMacs(null) != null) {
                                    macs1.addAll(p1.getMacs(null));
                                }
                            }
                        }
                    }
                }

                for (int d = c + 1; d < ccma.length; d++) {
                    Set floatingPorts2 = SetOperations.intersection(ccma[d], floatingPortsSet);
                    Set<String> macs2 = new HashSet<String>();
                    Iterator vs2i = ccma[d].iterator();
                    while (vs2i.hasNext()) {
                        Object v2 = vs2i.next();
                        if (v2 instanceof Switch) {
                            Switch sw2 = (Switch) v2;
                            Iterator<Port> psi = sw2.getPorts().iterator();
                            while (psi.hasNext()) {
                                Port p2 = psi.next();
                                if (p2.getType() == Port.ACCESS) {
                                    if (p2.getMacs(null) != null) {
                                        macs2.addAll(p2.getMacs(null));
                                    }
                                }
                            }
                        }
                    }

                    Set<Port> candidateConnectionPorts1 = new HashSet<Port>();
                    Iterator fp1i = floatingPorts1.iterator();
                    while (fp1i.hasNext()) {
                        Port fp1 = (Port) fp1i.next();
                        Set<String> commonMacs = SetOperations.intersection(fp1.getMacs(null), macs2);
                        if (commonMacs.size() > 0) {
                            candidateConnectionPorts1.add(fp1);
                        }
                    }

                    if (candidateConnectionPorts1.size() == 1) {
                        Set<Port> candidateConnectionPorts2 = new HashSet<Port>();
                        Iterator fp2i = floatingPorts2.iterator();
                        while (fp2i.hasNext()) {
                            Port fp2 = (Port) fp2i.next();
                            Set<String> commonMacs = SetOperations.intersection(fp2.getMacs(null), macs1);
                            if (commonMacs.size() > 0) {
                                candidateConnectionPorts2.add(fp2);
                            }
                        }
                        if (candidateConnectionPorts2.size() == 1) {

                            Port cp1 = candidateConnectionPorts1.toArray(new Port[0])[0];
                            Port cp2 = candidateConnectionPorts2.toArray(new Port[0])[0];
                            finalReducedTopology.addEdge(cp1, cp2);
                            cp1.setType(Port.LINK);
                            cp2.setType(Port.LINK);
                            continue components;
                        }
                    }
                }
            }
            break;
        }


        macPortIndex.clear();
        swsi = switches.iterator();
        while (swsi.hasNext()) {
            Switch sw = swsi.next();
            Iterator<Port> psi = sw.getPorts().iterator();
            while (psi.hasNext()) {
                Port pr = psi.next();
                if (pr.getType() == Port.ACCESS) {
                    Set ms = pr.getMacs(null);
                    if (ms != null) {
                        Iterator<String> msi = ms.iterator();
                        while (msi.hasNext()) {
                            String m = msi.next();
                            List ports = macPortIndex.get(m);
                            if (ports == null) {
                                ports = new ArrayList<Port>();
                                macPortIndex.put(m, ports);
                            }
                            ports.add(pr);
                        }
                    }
                }
            }
        }
        
        StorageController.storeTopology(TopologyExporter.exportTopology(finalReducedTopology), pulseStart);
    }

    private static SimpleGraph<Object, DefaultEdge> calculateTopology(List<Switch> switches, String vlan) throws TreeLikeReductionException {

        // Generate mac-switchport index
        // In same pass, add all adminmacs to set.
        Map<String, Set<Port>> macPortIndex = new HashMap<String, Set<Port>>();
        Set<String> allAdminMacs = new HashSet<String>();
        Iterator<Switch> si = switches.iterator();
        while (si.hasNext()) {
            Switch s = si.next();
            if (s.getMyMacs() != null) {
                allAdminMacs.addAll(s.getMyMacs());
            }
            Set<Port> ports = s.getPorts();
            Iterator<Port> porti = ports.iterator();
            while (porti.hasNext()) {
                Port p = porti.next();
                Set<String> macs = p.getMacs(vlan);
                if (macs != null) {
                    Iterator<String> maci = macs.iterator();
                    while (maci.hasNext()) {
                        String mac = maci.next();
                        Set<Port> ps = macPortIndex.get(mac);
                        if (ps == null) {
                            ps = new HashSet<Port>();
                            macPortIndex.put(mac, ps);
                        }
                        ps.add(p);
                    }
                }
            }
        }

        // Remove adminmacs from list.
        // macPortIndex.keySet().removeAll(allAdminMacs);
        
        // FIXME: test!
        // macPortIndex.keySet().remove("00:0c:cf:9c:3b:80");

        // Remove macs which are subset of another mac. It takes light O(n^2) to do that
        // but it releaves volume from heavy O(n^2) which comes later.

        Iterator<String> m1i = macPortIndex.keySet().iterator();
        while (m1i.hasNext()) {
            String mac1 = m1i.next();
            Set<Port> mac1ports = macPortIndex.get(mac1);
            Iterator<String> m2i = macPortIndex.keySet().iterator();
            while (m2i.hasNext()) {
                String mac2 = m2i.next();
                if (!mac1.equals(mac2)) {
                    Set<Port> mac2ports = macPortIndex.get(mac2);
                    if (SetOperations.isSubset(mac1ports, mac2ports)) {
                        m1i.remove();
                        break;
                    }
                }
            }
        }

        // Generate mac-switch index.
        Map<String, Set<Switch>> macSwitchIndex = new HashMap<String, Set<Switch>>();
        Iterator<String> mi = macPortIndex.keySet().iterator();
        while (mi.hasNext()) {
            String mac = mi.next();
            Set<Port> ports = macPortIndex.get(mac);
            Set<Switch> sws = new HashSet<Switch>();
            Iterator<Port> pi = ports.iterator();
            while (pi.hasNext()) {
                Port p = pi.next();
                sws.add(p.getSwitch());
            }
            macSwitchIndex.put(mac, sws);
        }

        // Generate mac-switch-port index.
        Map<String, Map<Switch, Port>> macSwitchPortIndex = new HashMap<String, Map<Switch, Port>>();
        mi = macPortIndex.keySet().iterator();
        while (mi.hasNext()) {
            String mac = mi.next();
            Set<Port> ports = macPortIndex.get(mac);
            Map<Switch, Port> swpr = new HashMap<Switch, Port>();
            Iterator<Port> prsi = ports.iterator();
            while (prsi.hasNext()) {
                Port pr = prsi.next();
                Switch sr = pr.getSwitch();
                swpr.put(sr, pr);
            }
            macSwitchPortIndex.put(mac, swpr);
        }

        String[] macs = macPortIndex.keySet().toArray(new String[0]);
        Set<ConnectedPortPair> connectedpairs = new HashSet<ConnectedPortPair>();
        Set<SwitchPair> switchPairs = new HashSet<SwitchPair>();



        // 4-macs algorithm by me :) O(n^4).
        for (int c = 0; c < macs.length - 3; c++) {
            String mac1 = macs[c];
            Set<Switch> sw1 = macSwitchIndex.get(mac1);
            for (int d = c + 1; d < macs.length - 2; d++) {
                String mac2 = macs[d];
                Set<Switch> sw2 = macSwitchIndex.get(mac2);
                Set<Switch> commonSwitches = SetOperations.intersection(sw1, sw2);
                if (commonSwitches.size() >= 2) {
                    Switch[] cs = commonSwitches.toArray(new Switch[0]);
                    for (int e = 0; e < cs.length - 1; e++) {
                        Switch s1 = cs[e];
                        Port s1p1 = macSwitchPortIndex.get(mac1).get(s1);
                        Port s1p2 = macSwitchPortIndex.get(mac2).get(s1);
                        if (!s1p1.equals(s1p2)) {
                            for (int f = e + 1; f < cs.length; f++) {
                                Switch s2 = cs[f];
                                if (!switchPairs.contains(new SwitchPair(s1, s2))) {
                                    Port s2p1 = macSwitchPortIndex.get(mac1).get(s2);
                                    Port s2p2 = macSwitchPortIndex.get(mac2).get(s2);
                                    if (!s2p1.equals(s2p2)) {
                                        thirdmac:
                                        for (int g = d + 1; g < macs.length - 1; g++) {
                                            String mac3 = macs[g];
                                            Set<Switch> sw3 = macSwitchIndex.get(mac3);
                                            if (SetOperations.isSubset(commonSwitches, sw3)) {
                                                Port s1p3 = macSwitchPortIndex.get(mac3).get(s1);
                                                if (!s1p3.equals(s1p1) && !s1p3.equals(s1p2)) {
                                                    fourthmac1:
                                                    for (int h = g + 1; h < macs.length; h++) {
                                                        String mac4 = macs[h];
                                                        Set<Switch> sw4 = macSwitchIndex.get(mac4);
                                                        if (SetOperations.isSubset(commonSwitches, sw4)) {
                                                            Port s2p3 = macSwitchPortIndex.get(mac4).get(s2);
                                                            if (!s2p3.equals(s2p1) && !s2p3.equals(s2p2)) {
                                                                // System.out.println("" + s1p1 + s1p2 + s1p3 + "<>" + s2p1 + s2p2 + s2p3);
                                                                Set<Port> ps = new HashSet<Port>();
                                                                String[] mac4a = {mac1, mac2, mac3, mac4};
                                                                Port p4s1 = null;
                                                                for (int i = 0; i < mac4a.length; i++) {
                                                                    String m = mac4a[i];
                                                                    p4s1 = macSwitchPortIndex.get(m).get(s1);
                                                                    ps.add(p4s1);
                                                                    if (ps.size() < i + 1) {
                                                                        break;
                                                                    }
                                                                }
                                                                if (ps.size() <= 3) {
                                                                    ps.clear();
                                                                    Port p4s2 = null;
                                                                    for (int i = 0; i < mac4a.length; i++) {
                                                                        String m = mac4a[i];
                                                                        p4s2 = macSwitchPortIndex.get(m).get(s2);
                                                                        ps.add(p4s2);
                                                                        if (ps.size() < i + 1) {
                                                                            break;
                                                                        }
                                                                    }
                                                                    if (ps.size() <= 3) {
                                                                        ConnectedPortPair cpp = new ConnectedPortPair(p4s1, p4s2);
                                                                        connectedpairs.add(cpp);
                                                                        switchPairs.add(new SwitchPair(p4s1.getSwitch(), p4s2.getSwitch()));                                                                        
                                                                        break thirdmac;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    Port s2p3 = macSwitchPortIndex.get(mac3).get(s2);
                                                    if (!s2p3.equals(s2p1) && !s2p3.equals(s2p2)) {
                                                        fourthmac2:
                                                        for (int h = g + 1; h < macs.length; h++) {
                                                            String mac4 = macs[h];
                                                            Set<Switch> sw4 = macSwitchIndex.get(mac4);
                                                            if (SetOperations.isSubset(commonSwitches, sw4)) {
                                                                s1p3 = macSwitchPortIndex.get(mac4).get(s1);
                                                                if (!s1p3.equals(s1p1) && !s1p3.equals(s1p2)) {
                                                                    // System.out.println("" + s1p1 + s1p2 + s1p3 + "<>" + s2p1 + s2p2 + s2p3);
                                                                    Set<Port> ps = new HashSet<Port>();
                                                                    String[] mac4a = {mac1, mac2, mac3, mac4};
                                                                    Port p4s1 = null;
                                                                    for (int i = 0; i < mac4a.length; i++) {
                                                                        String m = mac4a[i];
                                                                        p4s1 = macSwitchPortIndex.get(m).get(s1);
                                                                        ps.add(p4s1);
                                                                        if (ps.size() < i + 1) {
                                                                            break;
                                                                        }
                                                                    }
                                                                    if (ps.size() <= 3) {
                                                                        ps.clear();
                                                                        Port p4s2 = null;
                                                                        for (int i = 0; i < mac4a.length; i++) {
                                                                            String m = mac4a[i];
                                                                            p4s2 = macSwitchPortIndex.get(m).get(s2);
                                                                            ps.add(p4s2);
                                                                            if (ps.size() < i + 1) {
                                                                                break;
                                                                            }
                                                                        }
                                                                        if (ps.size() <= 3) {
                                                                            ConnectedPortPair cpp = new ConnectedPortPair(p4s1, p4s2);
                                                                            connectedpairs.add(cpp);
                                                                            switchPairs.add(new SwitchPair(p4s1.getSwitch(), p4s2.getSwitch()));                                                                            
                                                                            break thirdmac;
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        
        // 4-macs continues. 
        // Now we have candidate connections. We have to
        // do something similar to transitive reduction but with some edges missing.
        // We assume that our graph is a tree. It does not have to be in general but
        // per VLAN it is so.



        SimpleGraph<Object, DefaultEdge> rawtopology = new SimpleGraph<Object, DefaultEdge>(DefaultEdge.class);
        Iterator<ConnectedPortPair> cppi = connectedpairs.iterator();
        while (cppi.hasNext()) {
            ConnectedPortPair cpp = cppi.next();
            Switch sw1 = cpp.getPr1().getSwitch();
            Switch sw2 = cpp.getPr2().getSwitch();
            Port p1 = cpp.getPr1();
            Port p2 = cpp.getPr2();

            if (!rawtopology.containsVertex(sw1)) {
                rawtopology.addVertex(sw1);
            }
            if (!rawtopology.containsVertex(sw2)) {
                rawtopology.addVertex(sw2);
            }
            if (!rawtopology.containsVertex(p1)) {
                rawtopology.addVertex(p1);
            }
            if (!rawtopology.containsVertex(p2)) {
                rawtopology.addVertex(p2);
            }

            if (!rawtopology.containsEdge(sw1, p1)) {
                rawtopology.addEdge(sw1, p1);
            }
            if (!rawtopology.containsEdge(p1, p2)) {
                rawtopology.addEdge(p1, p2);
            }
            if (!rawtopology.containsEdge(p2, sw2)) {
                rawtopology.addEdge(p2, sw2);
            }
        }

        SimpleGraph<Object, DefaultEdge> reducedTopology = new SimpleGraph<Object, DefaultEdge>(DefaultEdge.class);

        ConnectivityInspector ci = new ConnectivityInspector(rawtopology);
        List<Set<Object>> conncomps = ci.connectedSets();
        Iterator<Set<Object>> ccsi = conncomps.iterator();
        while (ccsi.hasNext()) {
            Set<Object> cc = ccsi.next();
            // Graph<Object,PseudoDirectedEdge> subg = new Subgraph(rawtopology, cc);
            SimpleGraph<Object, DefaultEdge> sg = new SimpleGraph<Object, DefaultEdge>(DefaultEdge.class);
            Graphs.addGraph(sg, new Subgraph(rawtopology, cc));
            
            // SimpleGraph<Object, DefaultEdge> reducedsg = TreeLikeGraphReduction.reduce(sg);
            SimpleGraph<Object, DefaultEdge> reducedsg = LongestPathReduction.reduce(sg);
            
            Graphs.addGraph(reducedTopology, reducedsg);
            // System.out.println(reducedsg.toString());
        }


        // return topology;
        
        return reducedTopology;
    }
}

class SetLenComparator implements Comparator {

    @Override
    public int compare(Object o1, Object o2) {
        Set l1 = (Set) o1;
        Set l2 = (Set) o2;
        int gp1len = l1.size();
        int gp2len = l2.size();
        if (gp1len > gp2len) {
            return -1;
        } else if (gp1len < gp2len) {
            return 1;
        }
        return 0;
    }
}

class FloatingPortComparator implements Comparator {

    @Override
    public int compare(Object o1, Object o2) {
        Port p1 = (Port) o1;
        Port p2 = (Port) o2;
        return (new Integer(p2.floatingcount)).compareTo(new Integer(p1.floatingcount));
    }
}
