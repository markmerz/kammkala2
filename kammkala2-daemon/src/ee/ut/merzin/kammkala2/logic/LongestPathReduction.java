/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ee.ut.merzin.kammkala2.logic;

import ee.ut.merzin.kammkala2.driver.Port;
import ee.ut.merzin.kammkala2.driver.Switch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

/**
 *
 * @author markko
 */
public class LongestPathReduction {

    public static SimpleGraph<Object, DefaultEdge> reduce(SimpleGraph<Object, DefaultEdge> sg) {
        List<List> tomographyPaths = new ArrayList<List>();
        Object[] vsa = sg.vertexSet().toArray(new Object[0]);
        for (int c = 0; c < vsa.length - 1; c++) {
            Object v1 = vsa[c];
            if (v1 instanceof Switch && sg.degreeOf(v1) == 1) {
                for (int d = c + 1; d < vsa.length; d++) {
                    Object v2 = vsa[d];
                    if (v2 instanceof Switch && !v1.equals(v2) && sg.degreeOf(v2) == 1) {
                        /*
                        KShortestPaths ksps = new KShortestPaths(sg, v1, (int) Math.pow(sg.vertexSet().size(), 2));
                        List<GraphPath> gpl = ksps.getPaths(v2);
                        ArrayList<GraphPath> paths = new ArrayList<GraphPath>();
                        for (GraphPath gp : gpl) {
                        if (matchesPattern(gp)) {
                        paths.add(gp);
                        }
                        }
                        
                        tomographyPaths.add(getBestPath(paths));
                        // tomographyPaths.addAll(paths);
                        
                         */
                        List path = findPath(v1, v2, sg);
                        if (path != null) {
                            tomographyPaths.add(path);
                        }
                    }
                }
            }
        }

        // Collections.sort(tomographyPaths, new PathsComparator());

        // System.out.println(tomographyPaths);

        SimpleGraph<Object, DefaultEdge> rg = new SimpleGraph<Object, DefaultEdge>(DefaultEdge.class);
        Iterator<List> tpi = tomographyPaths.iterator();
        while (tpi.hasNext()) {
            List path = tpi.next();
            for (int c = 0; c < path.size() - 1; c++) {
                if (!rg.containsVertex(path.get(c))) {
                    rg.addVertex(path.get(c));
                }
                if (!rg.containsVertex(path.get(c + 1))) {
                    rg.addVertex(path.get(c + 1));
                }
                if (!rg.containsEdge(path.get(c), path.get(c + 1))) {
                    rg.addEdge(path.get(c), path.get(c + 1));
                }
            }
        }

        Set<Object> toremove = new HashSet();
        Iterator vi = rg.vertexSet().iterator();
        while (vi.hasNext()) {
            Object v = vi.next();
            if (v instanceof Port) {
                if (rg.degreeOf(v) != 2) {
                    toremove.add(v);
                }
            }
        }

        rg.removeAllVertices(toremove);
        
        toremove.clear();
        vi = rg.vertexSet().iterator();
        while (vi.hasNext()) {
            Object v = vi.next();
            if (rg.degreeOf(v) == 0) {
                toremove.add(v);
            }
        }

        rg.removeAllVertices(toremove);

        return rg;
    }

    private static boolean matchesPattern(GraphPath gp) {
        List vlist = Graphs.getPathVertexList(gp);
        Object[] va = vlist.toArray(new Object[0]);
        for (int c = 0; c < va.length; c++) {
            int expectedType = c % 3;
            switch (expectedType) {
                case 0:
                    if (!(va[c] instanceof Switch)) {
                        return false;
                    }
                    break;
                case 1:
                case 2:
                    if (!(va[c] instanceof Port)) {
                        return false;
                    }
                    break;
            }
        }

        return true;
    }

    private static GraphPath getBestPath(ArrayList<GraphPath> paths) {
        int index = -1;
        int len = Integer.MIN_VALUE;
        for (int c = 0; c < paths.size(); c++) {
            List vs = Graphs.getPathVertexList(paths.get(c));
            if (vs.size() > len) {
                len = vs.size();
                index = c;
            }
        }
        return paths.get(index);
    }

    private static List findPath(Object v1, Object v2, SimpleGraph<Object, DefaultEdge> sg) {
        Stack<List> stack = new Stack<List>();
        List<List> goodpaths = new ArrayList<List>();
        List path = new ArrayList();
        path.add(v1);
        stack.push(path);
        do {
            List currentPath = stack.pop();
            Object lastV = currentPath.get(currentPath.size() - 1);
            List nbs = Graphs.neighborListOf(sg, lastV);
            nbs.removeAll(currentPath);
            Iterator nbsi = nbs.iterator();
            while (nbsi.hasNext()) {
                Object nb = nbsi.next();
                if (isExpected(nb, currentPath.size())) {
                    ArrayList newPath = new ArrayList(currentPath);
                    newPath.add(nb);
                    if (nb.equals(v2)) {
                        goodpaths.add(newPath);
                    } else {
                        stack.push(newPath);
                    }
                }
            }
        } while (!stack.empty());


        if (goodpaths.isEmpty()) {
            return null;
        }

        Collections.sort(goodpaths, new ArrayLenComparator());


        return goodpaths.get(0);
    }


    private static boolean isExpected(Object nv, int depth) {
        switch (depth % 3) {
            case 0:
                if (nv instanceof Switch) {
                    return true;
                }
                break;
            case 1:
            case 2:
                if (nv instanceof Port) {
                    return true;
                }
                break;
        }
        return false;
    }
}

class PathsComparator implements Comparator {

    @Override
    public int compare(Object o1, Object o2) {
        GraphPath gp1 = (GraphPath) o1;
        GraphPath gp2 = (GraphPath) o2;
        int gp1len = gp1.getEdgeList().size();
        int gp2len = gp2.getEdgeList().size();
        if (gp1len > gp2len) {
            return -1;
        } else if (gp1len < gp2len) {
            return 1;
        }
        return 0;
    }
}

class ArrayLenComparator implements Comparator {

    @Override
    public int compare(Object o1, Object o2) {
        List l1 = (List) o1;
        List l2 = (List) o2;
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