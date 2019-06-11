package ee.ut.merzin.kammkala2.logic;

import ee.ut.merzin.kammkala2.test.TopologyExporter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.KShortestPaths;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.Subgraph;

/**
 *
 * @author markko
 */
public class TreeLikeGraphReduction {

    private static int specialvertexnumber = 0;

    /** 
     * Calculates transitive reduction from treelike undirected graph. Throws exception if graph is not treelike. 
     * Algorithm adapted from: "Sabine Cornelsen, Gabriele Di Stefano. Treelike Comparability Graphs: Characterization, Recognition, and Applications."
     */
    public static SimpleGraph<Object, DefaultEdge> reduce(SimpleGraph<Object, PseudoDirectedEdge> gr) throws TreeLikeReductionException {

        if (gr.vertexSet().size() == gr.edgeSet().size() + 1) {
            SimpleGraph<Object, DefaultEdge> ret = new SimpleGraph<Object, DefaultEdge>(DefaultEdge.class);
            Graphs.addAllVertices(ret, gr.vertexSet());
            Iterator<PseudoDirectedEdge> ei = gr.edgeSet().iterator();
            while (ei.hasNext()) {
                PseudoDirectedEdge e = ei.next();
                ret.addEdge(gr.getEdgeSource(e), gr.getEdgeTarget(e));
            }

            return ret;
        }

        specialvertexnumber = 0;
        ArrayList<SimpleGraph<Object, PseudoDirectedEdge>> splitComponents = new ArrayList();
        ArrayList<SimpleGraph<Object, PseudoDirectedEdge>> minimalSplitComponents = new ArrayList();
        ArrayList<Object[]> specialEdges = new ArrayList<Object[]>();

        splitComponents.add(gr);

        do {
            SimpleGraph<Object, PseudoDirectedEdge> g = splitComponents.remove(0);

            Object[] splitcomps = splitGraph(g);

            specialEdges.add((Object[]) splitcomps[2]);

            if (isStar((SimpleGraph) splitcomps[0]) || isClique((SimpleGraph) splitcomps[0])) {
                minimalSplitComponents.add((SimpleGraph) splitcomps[0]);
            } else {
                splitComponents.add((SimpleGraph) splitcomps[0]);
            }

            if (isStar((SimpleGraph) splitcomps[1]) || isClique((SimpleGraph) splitcomps[1])) {
                minimalSplitComponents.add((SimpleGraph) splitcomps[1]);
            } else {
                splitComponents.add((SimpleGraph) splitcomps[1]);
            }

        } while (!splitComponents.isEmpty());




        Map<Object, SimpleGraph<Object, PseudoDirectedEdge>> vertexComponentIndex = new HashMap<Object, SimpleGraph<Object, PseudoDirectedEdge>>();

        Iterator<SimpleGraph<Object, PseudoDirectedEdge>> scsi = minimalSplitComponents.iterator();
        while (scsi.hasNext()) {
            SimpleGraph<Object, PseudoDirectedEdge> sc = scsi.next();
            Iterator<Object> vsi = sc.vertexSet().iterator();
            while (vsi.hasNext()) {
                Object v = vsi.next();
                vertexComponentIndex.put(v, sc);
            }
        }


        /*        {
        SimpleGraph<Object, PseudoDirectedEdge> sg = new SimpleGraph<Object, PseudoDirectedEdge>(PseudoDirectedEdge.class);
        Iterator<SimpleGraph<Object, PseudoDirectedEdge>> msci = minimalSplitComponents.iterator();
        while (msci.hasNext()) {
        SimpleGraph<Object, PseudoDirectedEdge> msc = msci.next();
        Graphs.addGraph(sg, msc);
        }
        
        Iterator<Object[]> spei = specialEdges.iterator();
        while (spei.hasNext()) {
        Object[] se = spei.next();
        
        Object sv1 = se[0];
        Object sv2 = se[1];
        
        List sv1Neighbours = Graphs.neighborListOf(sg, sv1);
        List sv2Neighbours = Graphs.neighborListOf(sg, sv2);
        sg.removeVertex(sv1);
        sg.removeVertex(sv2);
        for (Object v1 : sv1Neighbours) {
        for (Object v2 : sv2Neighbours) {
        sg.addEdge(v1, v2);
        }
        }
        
        }
        TopologyExporter.exportTopology(sg, "/home/markko/reduction2/recomposed.dot");
        
        if (gr.vertexSet().containsAll(sg.vertexSet()) && sg.vertexSet().containsAll(gr.vertexSet())) {
        Iterator<PseudoDirectedEdge> ei = gr.edgeSet().iterator();
        while (ei.hasNext()) {
        PseudoDirectedEdge e = ei.next();
        if (!sg.containsEdge(gr.getEdgeSource(e), gr.getEdgeTarget(e))) {
        System.out.println("err2");
        }
        }
        
        ei = sg.edgeSet().iterator();
        while (ei.hasNext()) {
        PseudoDirectedEdge e = ei.next();
        if (!gr.containsEdge(sg.getEdgeSource(e), sg.getEdgeTarget(e))) {
        System.out.println("err3");
        }
        }
        
        } else {
        System.out.println("err1");
        }
        
        } */



        SimpleDirectedGraph<Object, PseudoDirectedEdge> dg = new SimpleDirectedGraph<Object, PseudoDirectedEdge>(PseudoDirectedEdge.class);
        ArrayList<Object> queue = new ArrayList<Object>();

        Object[] spe = specialEdges.get(0);
        queue.add(spe[0]);
        queue.add(spe[1]);

        dg.addVertex(spe[0]);
        dg.addVertex(spe[1]);
        dg.addEdge(spe[0], spe[1]);

        while (!queue.isEmpty()) {
            Object w = queue.remove(0);
            SimpleGraph<Object, PseudoDirectedEdge> gH = vertexComponentIndex.get(w);
            List<Object> nv = Graphs.neighborListOf(gH, w);
            for (Object v : nv) {
                PseudoDirectedEdge e = gH.getEdge(v, w);
                e.setPseudoSource(w);
                e.setPseudoTarget(v);
            }

            if (isStar(gH)) {
                Object sv = getStarCenter(gH);

                Object wprim = getOtherSpecialEdgeNode(w, specialEdges);
                SimpleGraph<Object, PseudoDirectedEdge> g2 = vertexComponentIndex.get(wprim);
                Object sv2 = getStarCenter(g2);
                if (sv.equals(w) && sv2.equals(wprim)) {
                    throw new TreeLikeReductionException("Not treelike:  (d) i");
                }

                List<Object> svns = Graphs.neighborListOf(gH, sv);
                String sourceOrTarget = null;
                for (Object svn : svns) {
                    PseudoDirectedEdge e = gH.getEdge(sv, svn);
                    if (e.getPseudoSource() != null && e.getPseudoTarget() != null) {
                        if (e.getPseudoSource().equals(sv)) {
                            sourceOrTarget = "source";
                        } else {
                            sourceOrTarget = "target";
                        }
                        break;
                    }
                }

                for (Object svn : svns) {
                    PseudoDirectedEdge e = gH.getEdge(sv, svn);
                    if (sourceOrTarget.equals("source")) {
                        e.setPseudoSource(sv);
                        e.setPseudoTarget(svn);
                    } else if (sourceOrTarget.equals("target")) {
                        e.setPseudoSource(svn);
                        e.setPseudoTarget(sv);
                    } else {
                        throw new TreeLikeReductionException("Programming error? 1");
                    }
                }

            } else if (isClique(gH)) { // clique

                /* 6. (e) i */
                int specialCount = 0;
                Iterator<Object> vsi = gH.vertexSet().iterator();
                while (vsi.hasNext()) {
                    Object v = vsi.next();
                    if (isSpecialVertex(v)) {
                        specialCount++;
                    }
                }
                if (specialCount > 2) {
                    throw new TreeLikeReductionException("Not treelike! (e) i");
                }

                /* 6. (e) ii and iii */
                List<Object> notsvs = new ArrayList<Object>();
                List<Object> svs = new ArrayList<Object>();
                Iterator<Object> vsi2 = gH.vertexSet().iterator();
                while (vsi2.hasNext()) {
                    Object v = vsi2.next();
                    if (!isSpecialVertex(v)) {
                        notsvs.add(v);
                    } else {
                        svs.add(v);
                    }
                }

                if (svs.size() == 2) {
                    Object sv1 = svs.remove(0);
                    Object sv2 = svs.remove(0);
                    KShortestPaths ksps = new KShortestPaths(gH, sv1, gH.vertexSet().size());
                    List<GraphPath> gpths = ksps.getPaths(sv2);
                    Iterator<GraphPath> gpthsi = gpths.iterator();
                    while (gpthsi.hasNext()) {
                        GraphPath gp = gpthsi.next();
                        List<Object> gpvs = Graphs.getPathVertexList(gp);
                        if (gpvs.containsAll(notsvs)) {
                            for (int c = 0; c < gpvs.size() - 1; c++) {
                                PseudoDirectedEdge e = gH.getEdge(gpvs.get(c), gpvs.get(c + 1));
                                e.setPseudoSource(gpvs.get(c));
                                e.setPseudoTarget(gpvs.get(c + 1));
                            }

                            Iterator<PseudoDirectedEdge> pdesi = gH.edgeSet().iterator();
                            Set<PseudoDirectedEdge> toremove = new HashSet<PseudoDirectedEdge>();
                            while (pdesi.hasNext()) {
                                PseudoDirectedEdge pde = pdesi.next();
                                if (!gp.getEdgeList().contains(pde)) {
                                    toremove.add(pde);
                                }
                            }
                            gH.removeAllEdges(toremove);
                            break;
                        }
                    }
                } else { // svs.size == 1.
                    Iterator<PseudoDirectedEdge> pdesi = gH.edgeSet().iterator();
                    Set<PseudoDirectedEdge> toremove = new HashSet<PseudoDirectedEdge>();
                    while (pdesi.hasNext()) {
                        PseudoDirectedEdge pde = pdesi.next();
                        if (pde.getPseudoSource() == null || pde.getPseudoTarget() == null) {
                            toremove.add(pde);
                        }
                    }
                    gH.removeAllEdges(toremove);
                }
            } else {
                throw new TreeLikeReductionException("Not star or clique?");
            }

            /* 6. (f) */
            Iterator<Object> vsi = gH.vertexSet().iterator();
            while (vsi.hasNext()) {
                Object v = vsi.next();
                if (!v.equals(w) && isSpecialVertex(v)) {
                    List<Object> vns = Graphs.neighborListOf(gH, v);
                    Object v2 = vns.get(0); // all edges adj to special vertex should have head or tail directed to spc vert.
                    PseudoDirectedEdge pde = gH.getEdge(v, v2);
                    for (int c = 0; c < specialEdges.size(); c++) {
                        Object[] sped = specialEdges.get(c);
                        if (sped[0].equals(v) || sped[1].equals(v)) {
                            if (pde.getPseudoTarget().equals(v)) {
                                Object tmp = sped[0];
                                sped[0] = sped[1];
                                sped[1] = tmp;
                                specialEdges.set(c, sped);
                            }
                            Object w2 = getOtherSpecialEdgeNode(v, specialEdges);
                            queue.add(w2);
                        }
                    }
                }
            }

        }

        // recompose
        SimpleDirectedGraph<Object, DefaultEdge> diG = new SimpleDirectedGraph<Object, DefaultEdge>(DefaultEdge.class);
        Iterator<SimpleGraph<Object, PseudoDirectedEdge>> mscsi = minimalSplitComponents.iterator();
        while (mscsi.hasNext()) {
            SimpleGraph<Object, PseudoDirectedEdge> gH = mscsi.next();
            Graphs.addAllVertices(diG, gH.vertexSet());
            Iterator<PseudoDirectedEdge> pdesi = gH.edgeSet().iterator();
            while (pdesi.hasNext()) {
                PseudoDirectedEdge pde = pdesi.next();
                diG.addEdge(pde.getPseudoSource(), pde.getPseudoTarget());
            }
        }



        for (Object[] se : specialEdges) {
            diG.addEdge(se[0], se[1], new SpecialEdge());
        }

        TopologyExporter.exportTopology(diG, "/home/markko/reduction2/directed1.dot");

        for (Object[] se : specialEdges) {
            List<Object> vs1 = Graphs.neighborListOf(diG, se[0]);
            List<Object> vs2 = Graphs.neighborListOf(diG, se[1]);
            for (Object v1 : vs1) {
                for (Object v2 : vs2) {
                    diG.addEdge(v1, v2);
                }
            }
            diG.removeVertex(se[0]);
            diG.removeVertex(se[1]);
        }

        TopologyExporter.exportTopology(diG, "/home/markko/reduction2/directed2.dot");

        // reduce
        boolean didsomething = false;
        do {
            didsomething = false;
            Iterator<DefaultEdge> dei = diG.edgeSet().iterator();
            while (dei.hasNext()) {
                DefaultEdge de = dei.next();
                Object src = diG.getEdgeSource(de);
                Object dst = diG.getEdgeTarget(de);
                KShortestPaths ksps = new KShortestPaths(diG, src, diG.vertexSet().size());
                List<GraphPath> gpths = ksps.getPaths(dst);
                if (gpths.size() > 1) {
                    diG.removeEdge(de);
                    didsomething = true;
                    break;
                }
            }
        } while (didsomething);

        TopologyExporter.exportTopology(diG, "/home/markko/reduction2/directed-reduced.dot");

        SimpleGraph<Object, DefaultEdge> rG = new SimpleGraph<Object, DefaultEdge>(DefaultEdge.class);
        Graphs.addAllVertices(rG, diG.vertexSet());
        Graphs.addAllEdges(rG, diG, diG.edgeSet());

        return rG;
    }

    /** From Cunningham. Decomposition on directed graphs. */
    private static boolean P(Object x, Object y, Object p, Object q, SimpleGraph sg) {
        boolean left = sg.containsEdge(p, q);
        boolean right1 = sg.containsEdge(p, y);
        boolean right2 = sg.containsEdge(x, q);

        boolean right = right1 && right2;

        if (left && right) {
            return false;
        }

        if (!left && !right) {
            return false;
        }

        return true;

    }

    /** From Cunningham. Decomposition on directed graphs. */
    private static boolean isSplit(Set S, Object x, Object y, SimpleGraph<Object, PseudoDirectedEdge> g) {
        Set T = new HashSet();
        T.addAll(S);
        while (!T.isEmpty()) {
            Object p = T.toArray(new Object[0])[0];
            T.remove(p);
            for (Object q : g.vertexSet()) {
                if (!S.contains(q) && P(x, y, p, q, g)) {
                    S.add(q);
                    T.add(q);
                }
            }
        }

        if (S.contains(y) || S.size() == g.vertexSet().size() - 1) {
            return false;
        }
        return true;
    }

    private static Object[] splitGraph(SimpleGraph<Object, PseudoDirectedEdge> g) throws TreeLikeReductionException {
        boolean found = false;
        Set S = new HashSet();
        edges:
        for (PseudoDirectedEdge e : g.edgeSet()) {
            Object v = g.getEdgeSource(e);
            Object x = g.getEdgeTarget(e);
            List xNeighbours = Graphs.neighborListOf(g, x);
            for (Object y : xNeighbours) {
                if (y.equals(v)) {
                    continue;
                }
                S.clear();
                S.addAll(Arrays.asList(v, x));
                if (isSplit(S, x, y, g)) {
                    found = true;
                    break edges;
                }
            }

            // lets try other way around
            x = g.getEdgeSource(e);
            v = g.getEdgeTarget(e);
            xNeighbours = Graphs.neighborListOf(g, x);
            for (Object y : xNeighbours) {
                if (y.equals(v)) {
                    continue;
                }
                S.clear();
                S.addAll(Arrays.asList(v, x));
                if (isSplit(S, x, y, g)) {
                    found = true;
                    break edges;
                }
            }
        }

        /*
        if (!found) {
            // no luck? try harder!
            List<List> vsubsets = SetOperations.allSubSets(new ArrayList(g.vertexSet()));
            Iterator<List> li = vsubsets.iterator();
            subsets:
            while (li.hasNext()) {
                List vl = li.next();
                if (vl.size() >= 2 && vl.size() <= g.vertexSet().size() - 2) {
                    Set V_S = SetOperations.difference(g.vertexSet(), S);
                    Object[] Sa = S.toArray(new Object[0]);
                    for (Object x : Sa) {
                        for (Object y : V_S) {
                            if (g.containsEdge(x, y)) {
                                S.clear();
                                S.addAll(vl);
                                if (isSplit(S, x, y, g)) {
                                    found = true;
                                    break subsets;
                                }
                            }
                        }
                    }
                }
            }
        }
         */
        
        if (!found) {
            all:
            for (PseudoDirectedEdge e : g.edgeSet()) {
                Object v = g.getEdgeSource(e);
                Object x = g.getEdgeTarget(e);
                List xNeighbours = Graphs.neighborListOf(g, x);
                for (Object y : xNeighbours) {
                    if (y.equals(v)) {
                        continue;
                    }
                    S.clear();
                    S.addAll(Arrays.asList(v, x));
                    if (isSplit(S, x, y, g)) {
                        found = true;
                        break all;
                    }
                }
            }
            TopologyExporter.exportTopology(g, "/home/markko/reduction2/nosplit.dot");
            throw new TreeLikeReductionException("No split?");
        }

        SimpleGraph<Object, PseudoDirectedEdge> gS = graphFromSubgraph(new Subgraph(g, S));
        SimpleGraph<Object, PseudoDirectedEdge> gV_S = graphFromSubgraph(new Subgraph(g, SetOperations.difference(g.vertexSet(), S)));

        SpecialVertex sv1 = new SpecialVertex("sv" + Integer.toString(specialvertexnumber++));
        gS.addVertex(sv1);
        SpecialVertex sv2 = new SpecialVertex("sv" + Integer.toString(specialvertexnumber++));
        gV_S.addVertex(sv2);
        for (Object a : gS.vertexSet()) {
            for (Object b : gV_S.vertexSet()) {
                if (g.containsEdge(a, b)) {
                    gS.addEdge(a, sv1);
                    gV_S.addEdge(b, sv2);
                }
            }
        }

        Object[] ret = new Object[3];
        ret[0] = gS;
        ret[1] = gV_S;
        ret[2] = new Object[]{sv1, sv2};

        return ret;

    }

    private static Object[] splitGraph6(SimpleGraph<Object, PseudoDirectedEdge> g) throws TreeLikeReductionException {
        PseudoDirectedEdge[] pdea = g.edgeSet().toArray(new PseudoDirectedEdge[0]);

        for (int c = 0; c < pdea.length - 1; c++) {
            PseudoDirectedEdge e1 = pdea[c];
            Object x1 = g.getEdgeSource(e1);
            Object y1 = g.getEdgeTarget(e1);
            for (int d = c + 1; d < pdea.length; d++) {
                PseudoDirectedEdge e2 = pdea[d];
                Object x2 = g.getEdgeSource(e2);
                Object y2 = g.getEdgeTarget(e2);

                if (g.containsEdge(x1, y1) || g.containsEdge(x2, y2)) {
                } else {
                    continue;
                }

                Set t = new HashSet();
                t.addAll(Arrays.asList(x1, y1, x2, y2));
                if (t.size() == 4) { // all objects are different, good
                    Set S = new HashSet();
                    S.add(x1);
                    S.add(y2);
                    Set T = new HashSet();
                    T.addAll(S);
                    while (!T.isEmpty()) {
                        Object p = T.toArray(new Object[0])[0];
                        T.remove(p);
                        Iterator qi = g.vertexSet().iterator();
                        while (qi.hasNext()) {
                            Object q = qi.next();
                            if (!g.containsVertex(q) && (P(x1, y1, p, q, g) || P(x2, y2, q, p, g))) {
                                S.add(q);
                                T.add(q);
                            }
                        }
                    }
                    if (!(S.contains(x2) || S.contains(y1) || S.size() == g.vertexSet().size() - 1)) {
                        SimpleGraph g1 = new SimpleGraph<Object, PseudoDirectedEdge>(PseudoDirectedEdge.class);
                        Graphs.addAllVertices(g1, S);
                        SpecialVertex sv1 = new SpecialVertex("sv" + Integer.toString(specialvertexnumber++));
                        g1.addVertex(sv1);
                        Iterator<PseudoDirectedEdge> gesi = g.edgeSet().iterator();
                        while (gesi.hasNext()) {
                            PseudoDirectedEdge g1e = gesi.next();
                            Object v1 = g.getEdgeSource(g1e);
                            Object v2 = g.getEdgeTarget(g1e);
                            if (g1.containsVertex(v1) && g1.containsVertex(v2)) {
                                g1.addEdge(v1, v2, g1e);
                            } else if (g1.containsVertex(v1) && !g1.containsVertex(v2)) {
                                g1.addEdge(v1, sv1);
                            } else if (!g1.containsVertex(v1) && g1.containsVertex(v2)) {
                                g1.addEdge(v2, sv1);
                            }
                        }

                        SimpleGraph g2 = new SimpleGraph<Object, PseudoDirectedEdge>(PseudoDirectedEdge.class);
                        Graphs.addAllVertices(g2, SetOperations.difference(g.vertexSet(), S));
                        SpecialVertex sv2 = new SpecialVertex("sv" + Integer.toString(specialvertexnumber++));
                        g2.addVertex(sv2);
                        gesi = g.edgeSet().iterator();
                        while (gesi.hasNext()) {
                            PseudoDirectedEdge g2e = gesi.next();
                            Object v1 = g.getEdgeSource(g2e);
                            Object v2 = g.getEdgeTarget(g2e);
                            if (g2.containsVertex(v1) && g2.containsVertex(v2)) {
                                g2.addEdge(v1, v2, g2e);
                            } else if (g2.containsVertex(v1) && !g2.containsVertex(v2)) {
                                g2.addEdge(v1, sv2);
                            } else if (!g2.containsVertex(v1) && g2.containsVertex(v2)) {
                                g2.addEdge(v2, sv2);
                            }
                        }

                        Object[] ret = new Object[3];
                        ret[0] = g1;
                        ret[1] = g2;
                        ret[2] = new Object[]{sv1, sv2};

                        return ret;
                    }
                }

            }
        }
        // falltrought. bad.
        throw new TreeLikeReductionException("No split!");
    }

    private static Object[] splitGraph5(SimpleGraph<Object, PseudoDirectedEdge> g) throws TreeLikeReductionException {
        PseudoDirectedEdge[] pdea = g.edgeSet().toArray(new PseudoDirectedEdge[0]);
        for (int c = 0; c < pdea.length - 1; c++) {
            PseudoDirectedEdge e1 = pdea[c];
            Object x1 = g.getEdgeSource(e1);
            Object y1 = g.getEdgeTarget(e1);
            for (int d = c + 1; d < pdea.length; d++) {
                PseudoDirectedEdge e2 = pdea[d];
                Object x2 = g.getEdgeSource(e2);
                Object y2 = g.getEdgeTarget(e2);
                Set t = new HashSet();
                t.addAll(Arrays.asList(x1, y1, x2, y2));
                if (t.size() == 4) { // all objects are different, good
                    Set S = new HashSet();
                    S.add(x1);
                    S.add(y2);
                    Set T = new HashSet();
                    T.addAll(S);
                    while (!T.isEmpty()) {
                        Object p = T.toArray(new Object[0])[0];
                        T.remove(p);
                        Iterator qi = g.vertexSet().iterator();
                        while (qi.hasNext()) {
                            Object q = qi.next();
                            if (!g.containsVertex(q) && (P(x1, y1, p, q, g) || P(x2, y2, q, p, g))) {
                                S.add(q);
                                T.add(q);
                            }
                        }
                    }
                    if (!(S.contains(x2) || S.contains(y1) || S.size() == g.vertexSet().size() - 1)) {
                        SimpleGraph g1 = new SimpleGraph<Object, PseudoDirectedEdge>(PseudoDirectedEdge.class);
                        Graphs.addAllVertices(g1, S);
                        SpecialVertex sv1 = new SpecialVertex("sv" + Integer.toString(specialvertexnumber++));
                        g1.addVertex(sv1);
                        Iterator<PseudoDirectedEdge> gesi = g.edgeSet().iterator();
                        while (gesi.hasNext()) {
                            PseudoDirectedEdge g1e = gesi.next();
                            Object v1 = g.getEdgeSource(g1e);
                            Object v2 = g.getEdgeTarget(g1e);
                            if (g1.containsVertex(v1) && g1.containsVertex(v2)) {
                                g1.addEdge(v1, v2, g1e);
                            } else if (g1.containsVertex(v1) && !g1.containsVertex(v2)) {
                                g1.addEdge(v1, sv1);
                            } else if (!g1.containsVertex(v1) && g1.containsVertex(v2)) {
                                g1.addEdge(v2, sv1);
                            }
                        }

                        SimpleGraph g2 = new SimpleGraph<Object, PseudoDirectedEdge>(PseudoDirectedEdge.class);
                        Graphs.addAllVertices(g2, SetOperations.difference(g.vertexSet(), S));
                        SpecialVertex sv2 = new SpecialVertex("sv" + Integer.toString(specialvertexnumber++));
                        g2.addVertex(sv2);
                        gesi = g.edgeSet().iterator();
                        while (gesi.hasNext()) {
                            PseudoDirectedEdge g2e = gesi.next();
                            Object v1 = g.getEdgeSource(g2e);
                            Object v2 = g.getEdgeTarget(g2e);
                            if (g2.containsVertex(v1) && g2.containsVertex(v2)) {
                                g2.addEdge(v1, v2, g2e);
                            } else if (g2.containsVertex(v1) && !g2.containsVertex(v2)) {
                                g2.addEdge(v1, sv2);
                            } else if (!g2.containsVertex(v1) && g2.containsVertex(v2)) {
                                g2.addEdge(v2, sv2);
                            }
                        }

                        Object[] ret = new Object[3];
                        ret[0] = g1;
                        ret[1] = g2;
                        ret[2] = new Object[]{sv1, sv2};

                        return ret;
                    }
                }

            }
        }
        // falltrought. bad.
        throw new TreeLikeReductionException("No split!");
    }

    private static Object[] splitGraph4(SimpleGraph<Object, PseudoDirectedEdge> g) throws TreeLikeReductionException {
        PseudoDirectedEdge[] pdea = g.edgeSet().toArray(new PseudoDirectedEdge[0]);
        for (int c = 0; c < pdea.length - 1; c++) {
            PseudoDirectedEdge e1 = pdea[c];
            Object x1 = g.getEdgeSource(e1);
            Object y1 = g.getEdgeTarget(e1);
            for (int d = c + 1; d < pdea.length; d++) {
                PseudoDirectedEdge e2 = pdea[d];
                Object x2 = g.getEdgeSource(e2);
                Object y2 = g.getEdgeTarget(e2);
                Set t = new HashSet();
                t.addAll(Arrays.asList(x1, y1, x2, y2));
                if (t.size() == 4) {

                    Set S = new HashSet();
                    S.add(x1);
                    S.add(y2);
                    Set T = new HashSet();
                    T.addAll(S);
                    while (!T.isEmpty()) {
                        Object p = T.toArray(new Object[0])[0];
                        T.remove(p);
                        Iterator qi = g.vertexSet().iterator();
                        while (qi.hasNext()) {
                            Object q = qi.next();
                            if (!g.containsVertex(q) && (P(x1, y1, p, q, g) || P(x2, y2, q, p, g))) {
                                S.add(q);
                                T.add(q);
                            }
                        }
                    }
                    if (S.contains(x2) || S.contains(y1) || S.size() == g.vertexSet().size() - 1) {
                        throw new TreeLikeReductionException("No split!");
                    }


                    SimpleGraph g1 = new SimpleGraph<Object, PseudoDirectedEdge>(PseudoDirectedEdge.class);
                    Graphs.addAllVertices(g1, S);
                    SpecialVertex sv1 = new SpecialVertex("sv" + Integer.toString(specialvertexnumber++));
                    g1.addVertex(sv1);
                    Iterator<PseudoDirectedEdge> gesi = g.edgeSet().iterator();
                    while (gesi.hasNext()) {
                        PseudoDirectedEdge g1e = gesi.next();
                        Object v1 = g.getEdgeSource(g1e);
                        Object v2 = g.getEdgeTarget(g1e);
                        if (g1.containsVertex(v1) && g1.containsVertex(v2)) {
                            g1.addEdge(v1, v2, g1e);
                        } else if (g1.containsVertex(v1) && !g1.containsVertex(v2)) {
                            g1.addEdge(v1, sv1);
                        } else if (!g1.containsVertex(v1) && g1.containsVertex(v2)) {
                            g1.addEdge(v2, sv1);
                        }
                    }

                    SimpleGraph g2 = new SimpleGraph<Object, PseudoDirectedEdge>(PseudoDirectedEdge.class);
                    Graphs.addAllVertices(g2, SetOperations.difference(g.vertexSet(), S));
                    SpecialVertex sv2 = new SpecialVertex("sv" + Integer.toString(specialvertexnumber++));
                    g2.addVertex(sv2);
                    gesi = g.edgeSet().iterator();
                    while (gesi.hasNext()) {
                        PseudoDirectedEdge g2e = gesi.next();
                        Object v1 = g.getEdgeSource(g2e);
                        Object v2 = g.getEdgeTarget(g2e);
                        if (g2.containsVertex(v1) && g2.containsVertex(v2)) {
                            g2.addEdge(v1, v2, g2e);
                        } else if (g2.containsVertex(v1) && !g2.containsVertex(v2)) {
                            g2.addEdge(v1, sv2);
                        } else if (!g2.containsVertex(v1) && g2.containsVertex(v2)) {
                            g2.addEdge(v2, sv2);
                        }
                    }

                    Object[] ret = new Object[3];
                    ret[0] = g1;
                    ret[1] = g2;
                    ret[2] = new Object[]{sv1, sv2};

                    return ret;

                }
            }
        }
        throw new TreeLikeReductionException("No split: fallthrow.");
    }

    private static Object[] splitGraph3(SimpleGraph<Object, PseudoDirectedEdge> g) throws TreeLikeReductionException {
        if (g.vertexSet().size() < 4) {
            throw new TreeLikeReductionException("Not treelike");
        }

        Object[] vsa = g.vertexSet().toArray(new Object[0]);
        Object[] vsa1 = Arrays.copyOfRange(vsa, 0, vsa.length / 2);
        Object[] vsa2 = Arrays.copyOfRange(vsa, vsa.length / 2, vsa.length);

        int sc1 = 0;
        int svp1 = 0;
        for (int c = 0; c < vsa1.length; c++) {
            if (isSpecialVertex(vsa1[c])) {
                sc1++;
                svp1 = c;
            }
        }

        int sc2 = 0;
        int svp2 = 0;
        for (int c = 0; c < vsa1.length; c++) {
            if (isSpecialVertex(vsa2[c])) {
                sc2++;
                svp2 = c;
            }
        }

//        if (sc1 + sc2 > 2) {
//            throw new TreeLikeReductionException("Failed: more then 2 specialverteces?");
//        }

        if (sc1 >= 2) {
            Object tmp = vsa1[svp1];
            vsa1[svp1] = vsa2[0];
            vsa2[0] = tmp;
        }

        if (sc2 >= 2) {
            Object tmp = vsa2[svp2];
            vsa2[svp2] = vsa1[0];
            vsa1[0] = tmp;
        }

        SimpleGraph g1 = new SimpleGraph<Object, PseudoDirectedEdge>(PseudoDirectedEdge.class);
        Graphs.addAllVertices(g1, Arrays.asList(vsa1));
        SpecialVertex sv1 = new SpecialVertex("sv" + Integer.toString(specialvertexnumber++));
        g1.addVertex(sv1);
        Iterator<PseudoDirectedEdge> gesi = g.edgeSet().iterator();
        while (gesi.hasNext()) {
            PseudoDirectedEdge g1e = gesi.next();
            Object v1 = g.getEdgeSource(g1e);
            Object v2 = g.getEdgeTarget(g1e);
            if (g1.containsVertex(v1) && g1.containsVertex(v2)) {
                g1.addEdge(v1, v2, g1e);
            } else if (g1.containsVertex(v1) && !g1.containsVertex(v2)) {
                g1.addEdge(v1, sv1);
            } else if (!g1.containsVertex(v1) && g1.containsVertex(v2)) {
                g1.addEdge(v2, sv1);
            }
        }

        SimpleGraph g2 = new SimpleGraph<Object, PseudoDirectedEdge>(PseudoDirectedEdge.class);
        Graphs.addAllVertices(g2, Arrays.asList(vsa2));
        SpecialVertex sv2 = new SpecialVertex("sv" + Integer.toString(specialvertexnumber++));
        g2.addVertex(sv2);
        gesi = g.edgeSet().iterator();
        while (gesi.hasNext()) {
            PseudoDirectedEdge g2e = gesi.next();
            Object v1 = g.getEdgeSource(g2e);
            Object v2 = g.getEdgeTarget(g2e);
            if (g2.containsVertex(v1) && g2.containsVertex(v2)) {
                g2.addEdge(v1, v2, g2e);
            } else if (g2.containsVertex(v1) && !g2.containsVertex(v2)) {
                g2.addEdge(v1, sv2);
            } else if (!g2.containsVertex(v1) && g2.containsVertex(v2)) {
                g2.addEdge(v2, sv2);
            }
        }

        Object[] ret = new Object[3];
        ret[0] = g1;
        ret[1] = g2;
        ret[2] = new Object[]{sv1, sv2};

        return ret;
    }

    private static Object[] splitGraph2(Graph<Object, PseudoDirectedEdge> g) throws TreeLikeReductionException {
        if (g.vertexSet().size() < 4) {
            throw new TreeLikeReductionException("Not treelike");
        }
        Iterator<Object> vi = g.vertexSet().iterator();
        Set<PseudoDirectedEdge> es = null;

        while (vi.hasNext()) {
            Object v = vi.next();
            if (isSpecialVertex(v)) { // test wheter this is special vertex
                List nbs = Graphs.neighborListOf(g, v);
                Iterator<Object> nbsi = nbs.iterator();
                while (nbsi.hasNext()) {
                    Object v2 = nbsi.next();
                    if (!isSpecialVertex(v2)) {
                        es = Collections.singleton(g.getEdge(v, v2));
                        break;
                    }
                }
            }
        }

        if (es == null) {
            vi = g.vertexSet().iterator();
            while (vi.hasNext()) {
                Object v = vi.next();
                Set<PseudoDirectedEdge> es2 = g.edgesOf(v);
                if (es == null || es2.size() < es.size()) {
                    es = es2;
                }
            }
        }
        PseudoDirectedEdge de = es.toArray(new PseudoDirectedEdge[es.size()])[0];
        Set<Object> v1 = new HashSet<Object>();
        v1.add(g.getEdgeSource(de));
        v1.add(g.getEdgeTarget(de));

        SimpleGraph<Object, PseudoDirectedEdge> g1 = graphFromSubgraph(new Subgraph(g, v1));
        SimpleGraph<Object, PseudoDirectedEdge> g2 = graphFromSubgraph(new Subgraph(g, SetOperations.difference(g.vertexSet(), g1.vertexSet())));

        // String specialVertex1 = "sv" + Integer.toString(specialvertexnumber++);
        SpecialVertex specialVertex1 = new SpecialVertex("sv" + Integer.toString(specialvertexnumber++));
        g1.addVertex(specialVertex1);
        Iterator<Object> v1i = SetOperations.difference(g1.vertexSet(), Collections.singleton((Object) specialVertex1)).iterator();
        while (v1i.hasNext()) {
            Object v = v1i.next();
            Set<PseudoDirectedEdge> ve = g.edgesOf(v);
            Iterator<PseudoDirectedEdge> vei = ve.iterator();
            while (vei.hasNext()) {
                PseudoDirectedEdge vde = vei.next();
                Object v2 = g.getEdgeSource(vde);
                if (v2.equals(v)) {
                    v2 = g.getEdgeTarget(vde);
                }
                if (g2.containsVertex(v2)) {
                    if (!g1.containsEdge(v, specialVertex1)) {
                        g1.addEdge(v, specialVertex1);
                    }

                }
            }
        }

        // String specialVertex2 = "sv" + Integer.toString(specialvertexnumber++);
        SpecialVertex specialVertex2 = new SpecialVertex("sv" + Integer.toString(specialvertexnumber++));
        g2.addVertex(specialVertex2);
        Iterator<Object> v2i = SetOperations.difference(g2.vertexSet(), Collections.singleton((Object) specialVertex2)).iterator();
        while (v2i.hasNext()) {
            Object v = v2i.next();
            Set<PseudoDirectedEdge> ve = g.edgesOf(v);
            Iterator<PseudoDirectedEdge> vei = ve.iterator();
            while (vei.hasNext()) {
                PseudoDirectedEdge vde = vei.next();
                Object v2 = g.getEdgeSource(vde);
                if (v2.equals(v)) {
                    v2 = g.getEdgeTarget(vde);
                }
                if (g1.containsVertex(v2)) {
                    if (!g2.containsEdge(v, specialVertex2)) {
                        g2.addEdge(v, specialVertex2);
                    }

                }
            }
        }

        Object[] ret = new Object[3];
        ret[0] = g1;
        ret[1] = g2;
        ret[2] = new Object[]{specialVertex1, specialVertex2};

        return ret;
    }

    private static boolean isStar(SimpleGraph<Object, PseudoDirectedEdge> g) {
        int vertexCount = g.vertexSet().size();
        int degMaxSeen = 0;
        int degOneSeen = 0;
        int maxDeg = vertexCount - 1;
        Iterator<Object> vi = g.vertexSet().iterator();
        while (vi.hasNext()) {
            Object v = vi.next();
            int vdeg = g.degreeOf(v);
            if (vdeg == maxDeg) {
                degMaxSeen++;
            } else if (vdeg == 1) {
                degOneSeen++;
            } else {
                return false;
            }
        }
        if (degOneSeen == maxDeg && degMaxSeen == 1) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean isClique(SimpleGraph<Object, PseudoDirectedEdge> g) {
        int vertexCount = g.vertexSet().size();
        Iterator<Object> vi = g.vertexSet().iterator();
        while (vi.hasNext()) {
            Object v = vi.next();
            int vdeg = g.degreeOf(v);
            if (vdeg != (vertexCount - 1)) {
                return false;
            }
        }
        return true;
    }

    private static SimpleGraph<Object, PseudoDirectedEdge> graphFromSubgraph(Subgraph<Object, PseudoDirectedEdge, SimpleGraph<Object, PseudoDirectedEdge>> g) {
        SimpleGraph<Object, PseudoDirectedEdge> ng = new SimpleGraph<Object, PseudoDirectedEdge>(PseudoDirectedEdge.class);
        Iterator<Object> vi = g.vertexSet().iterator();
        while (vi.hasNext()) {
            ng.addVertex(vi.next());
        }

        Iterator<PseudoDirectedEdge> ve = g.edgeSet().iterator();
        while (ve.hasNext()) {
            PseudoDirectedEdge de = ve.next();
            Object v1 = g.getEdgeSource(de);
            Object v2 = g.getEdgeTarget(de);
            ng.addEdge(v2, v1, de);
        }

        return ng;
    }

    private static Object getStarCenter(SimpleGraph<Object, PseudoDirectedEdge> gH) {
        Set<Object> vs = gH.vertexSet();
        for (Object v : vs) {
            if (gH.degreeOf(v) == vs.size() - 1) {
                return v;
            } else {
                List<Object> c = Graphs.neighborListOf(gH, v);
                return c.get(0);
            }
        }
        return null;
    }

    private static boolean isSpecialVertex(Object v) {
        if (v instanceof SpecialVertex) {
            return true;
        }
        return false;
    }

    private static Object getOtherSpecialEdgeNode(Object w, ArrayList<Object[]> specialEdges) throws TreeLikeReductionException {
        Iterator<Object[]> spei = specialEdges.iterator();
        while (spei.hasNext()) {
            Object[] spe = spei.next();
            if (spe[0].equals(w)) {
                return spe[1];
            } else if (spe[1].equals(w)) {
                return spe[0];
            }
        }
        throw new TreeLikeReductionException("Programming error? 2");
    }
}
