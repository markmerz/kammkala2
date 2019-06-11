
package ee.ut.merzin.kammkala2.logic;

/**
 *
 * @author markko
 */
public class SpecialVertex {

    private String name;

    SpecialVertex(String name) {
        this.name = name;
    }

    String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return name;
    }
}
