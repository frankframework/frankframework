package nl.nn.adapterframework.util;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Counter Tester.
 *
 * @author <Sina Sen>
 */
public class CounterTest {




    /**
     * Method: decrease()
     */
    @Test
    public void testDecrease() throws Exception {
        Counter c = new Counter(15);
        assertEquals(c.decrease(), 14);
    }

    /**
     * Method: decrease(long amount)
     */
    @Test
    public void testDecreaseAmount() throws Exception {
        Counter c = new Counter(15);
        assertEquals(c.decrease(10), 5);
    }

    /**
     * Method: increase()
     */
    @Test
    public void testIncrease() throws Exception {
        Counter c = new Counter(15);
        assertEquals(c.increase(), 16);
    }

    /**
     * Method: increase(long amount)
     */
    @Test
    public void testIncreaseAmount() throws Exception {
        Counter c = new Counter(15);
        assertEquals(c.increase(10), 25);
    }

    /**
     * Method: clear()
     */
    @Test
    public void testClear() throws Exception {
        Counter c = new Counter(15);
        c.clear();
        assertEquals(c.getValue(), 0);
    }

    /**
     * Method: getValue()
     */
    @Test
    public void testGetValue() throws Exception {
        Counter c = new Counter(15);
        assertEquals(c.getValue(), 15);
    }

    /**
     * Method: setValue(long newValue)
     */
    @Test
    public void testSetValue() throws Exception {
        Counter c = new Counter(15);
        c.setValue(30);
        assertEquals(c.getValue(), 30);
    }


}