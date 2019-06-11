package ee.ut.merzin.kammkala2.logic;

import ee.ut.merzin.kammkala2.driver.Switch;
import org.jgrapht.ext.VertexNameProvider;

public class VertexIDProvider implements VertexNameProvider<Switch> {

    @Override
    public String getVertexName(Switch arg0) {

        if (arg0 == null) {
            return "-1";
        }
        return Integer.toString(arg0.hashCode());
    }
}
