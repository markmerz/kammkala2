package ee.ut.merzin.kammkala2.logic;

import ee.ut.merzin.kammkala2.driver.Port;
import ee.ut.merzin.kammkala2.driver.Switch;
import org.jgrapht.graph.DefaultEdge;

/**
 *
 * @author markko
 */
public class ConnectedPortPair extends DefaultEdge {

    private Port pr1;
    private Port pr2;
    private Object pseudoSource;
    private Object pseudoTarget;
   
       
    public ConnectedPortPair(Port pr1, Port pr2) {
        super();
        if (pr1.getSwitch().getId() < pr2.getSwitch().getId()) {
            this.pr1 = pr1;
            this.pr2 = pr2;
        } else {
            this.pr1 = pr2;
            this.pr2 = pr1;
        }
    }

    public Switch getV1() {
        return pr1.getSwitch();
    }

    public Switch getV2() {
        return pr2.getSwitch();
    }

    @Override
    public String toString() {
        return pr1.toString() + "___" + pr2.toString();
    }

    public Port getPr1() {
        return pr1;
    }

    public Port getPr2() {
        return pr2;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ConnectedPortPair) {
            ConnectedPortPair pair = (ConnectedPortPair) o;
            if (pair.getPr1().equals(pr1) && pair.getPr2().equals(pr2)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + (this.pr1 != null ? this.pr1.hashCode() : 0);
        hash = 97 * hash + (this.pr2 != null ? this.pr2.hashCode() : 0);
        return hash;
    }

    Port getOther(Port p) {
        if (pr1.equals(p)) {
            return pr2;
        }
        if (pr2.equals(p)) {
            return pr1;
        }
        return null;
    }

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
}
