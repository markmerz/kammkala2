package ee.ut.merzin.kammkala2.snmp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;

/**
 * Convenience methods for snmp.
 * @author markko
 */
public class SNMP {

    /** SNMP get. */
    public static String get(String oid, CommunityTarget target) throws IOException {
        PDU pdu = new PDU();
        try {
            pdu.add(new VariableBinding(new OID(oid)));
        } catch (NumberFormatException ex) {
            return null;
        }
        ResponseEvent response = SNMPFactory.getInstance().getSnmp().get(pdu, target);
        PDU pdur = response.getResponse();
        if (pdur != null) {
            VariableBinding vb = pdur.get(0);
            if (vb != null) {
                Variable v = vb.getVariable();
                if (v != null) {
                    return v.toString();
                }
            }
        }
        return null;
    }

    /** SNMP walk. Supports both nextwalk and bulkwalk. Returns 2-dimensional array of Strings, column 0 for oid, column 1 for result. */
    public static String[][] bulkwalk(String oid, CommunityTarget target) throws IOException {
        int pdutype;
        if (target.getVersion() == SnmpConstants.version1) {
            pdutype = PDU.GETNEXT;
        } else {
            pdutype = PDU.GETBULK;
        }
        TreeUtils treeutils = new TreeUtils(SNMPFactory.getInstance().getSnmp(), new DefaultPDUFactory(pdutype));
        List<TreeEvent> results = treeutils.getSubtree(target, new OID(oid));
        ArrayList<String[]> returns = new ArrayList<String[]>();
        Iterator<TreeEvent> ri = results.iterator();
        while (ri.hasNext()) {
            TreeEvent te = ri.next();
            VariableBinding[] vbs = te.getVariableBindings();
            if (vbs == null) {
                return null;
            }
            for (int c = 0; c < vbs.length; c++) {
                String[] ret = new String[2];
                ret[0] = vbs[c].getOid().toString().replace(oid, "").replaceFirst("\\.", "");
                ret[1] = vbs[c].getVariable().toString();
                returns.add(ret);
            }
        }

        return returns.toArray(new String[0][]);
    }

    /** Wraps snmp walk result to set. */
    public static Set<String> walk2set(String[][] walkresult) {
        if (walkresult == null) {
            return null;
        }
        Set<String> rset = new HashSet<String>();
        for (int c = 0; c < walkresult.length; c++) {
            rset.add(walkresult[c][1]);
        }
        return rset;
    }

    /** Wraps snmp walk to oid->value map. */
    public static Map<String, String> walk2map(String[][] walkresult) {
        if (walkresult == null) {
            return null;
        }
        Map rhash = new HashMap<String, String>();
        for (int c = 0; c < walkresult.length; c++) {
            rhash.put(walkresult[c][0], walkresult[c][1]);
        }
        return rhash;
    }
}

class SNMPFactory {

    private static SNMPFactory instance = new SNMPFactory();
    private TransportMapping transport;
    private Snmp snmp;

    private SNMPFactory() {
        try {
            transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            snmp.listen();
        } catch (IOException ex) {
            Logger.getLogger(SNMPFactory.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1); // This can happen only when program starts.
            // When we cannot start snmp listener socket 
            // then there is no reason to start at all.
        }
    }

    public static SNMPFactory getInstance() {
        return instance;
    }

    public Snmp getSnmp() {
        return snmp;
    }
}
