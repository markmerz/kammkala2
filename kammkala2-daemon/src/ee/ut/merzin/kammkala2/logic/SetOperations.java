package ee.ut.merzin.kammkala2.logic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** From http://www.java2s.com/Code/Java/Collections-Data-Structure/Setoperationsunionintersectiondifferencesymmetricdifferenceissubsetissuperset.htm. 
 *  Modified to support really large sets.
 *  All-subsets from http://blog.sarah-happy.ca/2011/02/all-sub-sets-of-set-in-java.html.
 */
public class SetOperations {

    public static <T> Set<T> union(Set<T> setA, Set<T> setB) {
        Set<T> tmp = new HashSet<T>(setA);
        tmp.addAll(setB);
        return tmp;
    }

    public static <T> Set<T> intersection(Set<T> setA, Set<T> setB) {
        Set<T> tmp = new HashSet<T>();
        Iterator<T> i = setA.iterator();
        while (i.hasNext()) {
            T x = i.next();
            if (setB.contains(x)) {
                tmp.add(x);
            }
        }
        return tmp;
    }
    
    public static <T> Set<T> intersection(Set<T> setA, Set<T> setB, Set<T> setC, Set<T> setD) {
        Set s1 = intersection(setA, setB);
        s1 = intersection(s1, setC);
        s1 = intersection(s1, setD);
        return s1;
    }

    public static <T> Set<T> difference(Set<T> setA, Set<T> setB) {
        Set<T> tmp = new HashSet<T>(setA);
        tmp.removeAll(setB);
        return tmp;
    }

    public static <T> Set<T> symdifference(Set<T> setA, Set<T> setB) {
        Set<T> tmpA;
        Set<T> tmpB;

        tmpA = union(setA, setB);
        tmpB = intersection(setA, setB);
        return difference(tmpA, tmpB);
    }

    public static <T> boolean isSubset(Set<T> setA, Set<T> setB) {
        return setB.containsAll(setA);
    }

    public static <T> boolean isSuperset(Set<T> setA, Set<T> setB) {
        return setA.containsAll(setB);
    }
    
    /**
     * Find all the subsets of in, non-recursively.
     * 
     * @param <T>
     *            the type of item items in the set.
     * @param in
     *            the set to get subsets off.
     * @return the list of subsets of in.
     */
    public static <T> List<List<T>> allSubSets(List<T> in) {
        List<List<T>> out = new ArrayList<List<T>>();
        out.add(new ArrayList<T>());

        for (T i : in) {
            List<List<T>> next = new ArrayList<List<T>>();
            for (List<T> j : out) {
                next.add(j);

                List<T> k = new ArrayList<T>(j);
                k.add(i);
                next.add(k);
            }
            out = next;
        }

        return out;
    }

}
