package ee.ut.merzin.kammkala2.test;

import ee.ut.merzin.kammkala2.driver.Port;
import ee.ut.merzin.kammkala2.driver.Switch;
import ee.ut.merzin.kammkala2.logic.ConnectedPortPair;
import ee.ut.merzin.kammkala2.logic.SpecialEdge;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jgrapht.graph.SimpleGraph;
import ee.ut.merzin.kammkala2.logic.SpecialVertex;
import java.io.StringWriter;
import org.jgrapht.graph.SimpleDirectedGraph;

public class TopologyExporter {
    
    public static void exportTopology(SimpleGraph sg, String file) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            out.append(exportTopology(sg));
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
    }

    public static String exportTopology(SimpleGraph sg) {
        try {
            // BufferedWriter out = new BufferedWriter(new FileWriter(file));
            StringWriter out = new StringWriter();
            out.append("graph G {\n");

            Iterator vsi = sg.vertexSet().iterator();
            while (vsi.hasNext()) {
                Object v = vsi.next();
                out.append(v.toString().replaceAll("\\s+", "_").replaceAll("-", "_").replaceAll("#", "_").replaceAll(":", "__").replaceAll("/", "_").replaceAll("\\.", "_"));
                if (v instanceof Switch) {
                    out.append(" [shape=box];\n");
                } else if (v instanceof Port) {
                    Port p = (Port) v;
                    if (p.getType() == Port.CLOUD) {
                        out.append(" [shape=tripleoctagon];\n");
                    } else {
                        out.append(" [shape=circle];\n");
                    }
                } else if (v instanceof SpecialVertex) {
                    out.append(" [shape=diamond];\n");
                } else if (v instanceof ConnectedPortPair) {
                    out.append(" [shape=pentagon];\n");
                } else {
                    out.append(";\n");
                }
            }

            Iterator esi = sg.edgeSet().iterator();
            while (esi.hasNext()) {
                Object e = esi.next();
                out.append(sg.getEdgeSource(e).toString().replaceAll("\\s+", "_").replaceAll("-", "_").replaceAll("#", "_").replaceAll(":", "__").replaceAll("/", "_").replaceAll("\\.", "_"));
                out.append(" -- ");
                out.append(sg.getEdgeTarget(e).toString().replaceAll("\\s+", "_").replaceAll("-", "_").replaceAll("#", "_").replaceAll(":", "__").replaceAll("/", "_").replaceAll("\\.", "_"));
                out.append(";\n");
            }

            out.append("}\n");
            out.close();
            
            return out.toString();
            
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        return null;
    }

    
    public static void exportTopology(SimpleDirectedGraph sg, String file) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            out.append("digraph G {\n");

            Iterator vsi = sg.vertexSet().iterator();
            while (vsi.hasNext()) {
                Object v = vsi.next();
                out.append(v.toString().replaceAll("\\s+", "_").replaceAll("-", "_").replaceAll("#", "_").replaceAll(":", "__"));
                if (v instanceof Switch) {
                    out.append(" [shape=box];\n");
                } else if (v instanceof Port) {
                    out.append(" [shape=circle];\n");
                } else if (v instanceof SpecialVertex) {
                    out.append(" [shape=diamond];\n");
                } else {
                    out.append(";\n");
                }
            }

            Iterator esi = sg.edgeSet().iterator();
            while (esi.hasNext()) {
                Object e = esi.next();
                out.append(sg.getEdgeSource(e).toString().replaceAll("\\s+", "_").replaceAll("-", "_").replaceAll("#", "_").replaceAll(":", "__"));
                out.append(" -> ");
                out.append(sg.getEdgeTarget(e).toString().replaceAll("\\s+", "_").replaceAll("-", "_").replaceAll("#", "_").replaceAll(":", "__"));
                if (e instanceof SpecialEdge) {
                    out.append(" [style=dotted]");
                }
                out.append(";\n");
            }

            out.append("}\n");
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }     
     
}
