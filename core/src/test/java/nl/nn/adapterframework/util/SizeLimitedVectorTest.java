package nl.nn.adapterframework.util;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * SizeLimitedVector Tester.
 *
 * @author <Sina Sen>

 */
public class SizeLimitedVectorTest {


    /**
     *
     * Method: add(Object o)
     *
     */
    @Test
    public void testAdd() throws Exception {
        SizeLimitedVector slv = new SizeLimitedVector(10);
        slv.add("testString");
        assertEquals(slv.getMaxSize(), 10);
        assertEquals(slv.get(0), "testString");
    }

    /**
     *
     * Method: setMaxSize(int maxSize)
     *
     */
    @Test
    public void testSetMaxSize() throws Exception {
        SizeLimitedVector slv = new SizeLimitedVector(10);
        slv.setMaxSize(14);
        assertEquals(slv.getMaxSize(), 14);
    }


}