package ee.ut.merzin.kammkala2.logic;

public class EdgeLabelProvider implements org.jgrapht.ext.EdgeNameProvider<ConnectedPortPair> {

    @Override
    public String getEdgeName(ConnectedPortPair arg0) {

        if (arg0 == null) {
            return "NULL";
        }
        return arg0.toString();
    }
}
