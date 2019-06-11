package ee.ut.merzin.kammkala2.logic;

import ee.ut.merzin.kammkala2.driver.Port;
import org.jgrapht.graph.DefaultEdge;

/**
 *
 * @author markko
 */
public class PseudoDirectedEdge extends DefaultEdge {

    private Object pseudoSource = null;
    private Object pseudoTarget = null;
    private Port port1;
    private Port port2;

    /**
     * @return the pseudoSource
     */
    public Object getPseudoSource() {
        return pseudoSource;
    }

    /**
     * @param pseudoSource the pseudoSource to set
     */
    public void setPseudoSource(Object pseudoSource) {
        this.pseudoSource = pseudoSource;
    }

    /**
     * @return the pseudoTarget
     */
    public Object getPseudoTarget() {
        return pseudoTarget;
    }

    /**
     * @param pseudoTarget the pseudoTarget to set
     */
    public void setPseudoTarget(Object pseudoTarget) {
        this.pseudoTarget = pseudoTarget;
    }

    /**
     * @return the port1
     */
    public Port getPort1() {
        return port1;
    }

    /**
     * @param port1 the port1 to set
     */
    public void setPort1(Port port1) {
        this.port1 = port1;
    }

    /**
     * @return the port2
     */
    public Port getPort2() {
        return port2;
    }

    /**
     * @param port2 the port2 to set
     */
    public void setPort2(Port port2) {
        this.port2 = port2;
    }
}
