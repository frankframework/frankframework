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
    public void testAdd() {
        SizeLimitedVector slv = new SizeLimitedVector(10);
        slv.add("testString");
        assertEquals(slv.getMaxSize(), 10);
        assertEquals(slv.get(0), "testString");

    }


    @Test
    public void testMaxSizePassed() throws ArrayIndexOutOfBoundsException {
        SizeLimitedVector slv = new SizeLimitedVector(1);
        slv.add(13);
        slv.add(14);
        assertEquals(slv.get(0), 14);

    }
    /**
     *
     * Method: setMaxSize(int maxSize)
     *
     */
    @Test
    public void testSetMaxSize() throws Exception {
        SizeLimitedVector slv = new SizeLimitedVector(5);
        slv.setMaxSize(6);
        slv.add(1); slv.add(2); slv.add(3); slv.add(4); slv.add(5); slv.add(6); slv.add(7);
        assertEquals(slv.getMaxSize(), 6);
        assertEquals(slv.capacity(), 10);
        assertEquals(slv.size(), 6);
        assertEquals(slv.get(0), 2);
    }


}