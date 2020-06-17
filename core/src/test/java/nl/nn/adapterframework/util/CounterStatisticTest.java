package nl.nn.adapterframework.util;

import org.junit.Test; 
import static org.junit.Assert.assertEquals;
/** 
* CounterStatistic Tester. 
* 
* @author <Sina Sen>

*/ 
public class CounterStatisticTest { 


/** 
* 
* Method: performAction(int action) 
* 
*/ 
@Test
public void testPerformAction01() throws Exception {
    CounterStatistic cs = new CounterStatistic(10);
    cs.performAction(0);
}

    /**
     *
     * Method: performAction(int action)
     *
     */
    @Test
    public void testPerformAction2() throws Exception {
        CounterStatistic cs = new CounterStatistic(10);
        cs.performAction(2);
        assertEquals(cs.getValue(), 0);    }

    /**
     *
     * Method: performAction(int action)
     *
     */
    @Test
    public void testPerformAction34() throws Exception {
        CounterStatistic cs = new CounterStatistic(10);
        cs.performAction(3);
        assertEquals(cs.getValue(), 10);    }


    /**
* 
* Method: getIntervalValue() 
* 
*/ 
@Test
public void testGetIntervalValue() throws Exception {
    CounterStatistic cs = new CounterStatistic(10);
    assertEquals(cs.getIntervalValue(), 0);}




} 
