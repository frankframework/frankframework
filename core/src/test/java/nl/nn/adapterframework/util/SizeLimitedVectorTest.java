package nl.nn.adapterframework.util;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
    public void testAdd() throws ArrayIndexOutOfBoundsException {
        SizeLimitedVector slv = new SizeLimitedVector(10);
        slv.add("testString");
        assertEquals(slv.getMaxSize(), 10);
        assertEquals(slv.get(0), "testString");

    }


    @Test
    public void testMaxSizePassed() throws ArrayIndexOutOfBoundsException {
        try{SizeLimitedVector slv = new SizeLimitedVector(1);
        slv.add(13);
        slv.add(14);
        }
        catch (ArrayIndexOutOfBoundsException e) {
            fail("array capacity is passed, capacity was 1, but 2 elements got added");
        }

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