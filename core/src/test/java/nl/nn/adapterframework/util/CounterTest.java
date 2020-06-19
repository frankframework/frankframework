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
        assertEquals(14, c.decrease());
    }

    /**
     * Method: decrease(long amount)
     */
    @Test
    public void testDecreaseAmount() throws Exception {
        Counter c = new Counter(15);
        assertEquals(5, c.decrease(10));
    }

    /**
     * Method: increase()
     */
    @Test
    public void testIncrease() throws Exception {
        Counter c = new Counter(15);
        assertEquals(16, c.increase());
    }

    /**
     * Method: increase(long amount)
     */
    @Test
    public void testIncreaseAmount() throws Exception {
        Counter c = new Counter(15);
        assertEquals(25, c.increase(10));
    }

    /**
     * Method: clear()
     */
    @Test
    public void testClear() throws Exception {
        Counter c = new Counter(15);
        c.clear();
        assertEquals(0, c.getValue());
    }

    /**
     * Method: getValue()
     */
    @Test
    public void testGetValue() throws Exception {
        Counter c = new Counter(15);
        assertEquals(15, c.getValue());
    }

    /**
     * Method: setValue(long newValue)
     */
    @Test
    public void testSetValue() throws Exception {
        Counter c = new Counter(15);
        c.setValue(30);
        assertEquals(30, c.getValue());
    }


}