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
        assertEquals("testString", slv.get(0));

    }


    @Test
    public void testMaxSizePassed() throws ArrayIndexOutOfBoundsException {
        SizeLimitedVector slv = new SizeLimitedVector(1);
        slv.add(13);
        slv.add(14);
        assertEquals(14, slv.get(0));

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
        assertEquals(6, slv.getMaxSize());
        assertEquals( 10, slv.capacity());
        assertEquals(6, slv.size());
        assertEquals(2, slv.get(0));
    }


}