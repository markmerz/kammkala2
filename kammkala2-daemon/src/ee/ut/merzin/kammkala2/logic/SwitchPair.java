package ee.ut.merzin.kammkala2.logic;

import ee.ut.merzin.kammkala2.driver.Switch;

/**
 *
 * @author markko
 */
public class SwitchPair {

    private Switch s1;
    private Switch s2;

    public SwitchPair(Switch s1, Switch s2) {
        if (s1.getId() < s2.getId()) {
            this.s1 = s1;
            this.s2 = s2;
        } else {
            this.s1 = s2;
            this.s2 = s1;
        }
    }

    public Switch getS1() {
        return s1;
    }

    public Switch getS2() {
        return s2;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SwitchPair) {
            SwitchPair sp = (SwitchPair) o;
            if (sp.getS1().getId() == s1.getId() && sp.getS2().getId() == s2.getId()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return s1.hashCode() + s2.hashCode();
    }
}