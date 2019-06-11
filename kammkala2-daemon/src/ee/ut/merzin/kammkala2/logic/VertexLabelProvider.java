package ee.ut.merzin.kammkala2.logic;

import ee.ut.merzin.kammkala2.driver.Switch;

public class VertexLabelProvider implements org.jgrapht.ext.VertexNameProvider<Switch> {

    @Override
    public String getVertexName(Switch arg0) {
        if (arg0 == null) {
            return "NULL";
        }
        if (arg0.getName() == null) {
            return "null";
        }
        return arg0.getName();
    }
}
