package nl.nn.adapterframework.statistics;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.statistics.HasStatistics.Action;

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
    public void testPerformActionSummaryOrFull() throws Exception {
        CounterStatistic cs = new CounterStatistic(10);
        cs.performAction(Action.SUMMARY);
        assertEquals(10, cs.getValue());
    }

    /**
     *
     * Method: performAction(int action)
     *
     */
    @Test
    public void testPerformActionReset() throws Exception {
        CounterStatistic cs = new CounterStatistic(10);
        cs.performAction(Action.RESET);
        assertEquals(0, cs.getValue());
    }
    

    /**
     *
     * Method: performAction(int action)
     *
     */
    @Test
    public void testPerformActionMarkMain() throws Exception {
        CounterStatistic cs = new CounterStatistic(10);
        cs.performAction(Action.MARK_MAIN);
        assertEquals( 10, cs.getValue());
    }

    @Test
    public void testPerformActionMarkFull() throws Exception {
        CounterStatistic cs = new CounterStatistic(10);
        cs.performAction(Action.MARK_FULL);
        assertEquals( 10, cs.getValue());
    }


    /**
     *
     * Method: getIntervalValue()
     *
     */
    @Test
    public void testGetIntervalValue() throws Exception {
        CounterStatistic cs = new CounterStatistic(10);
        assertEquals( 0, cs.getIntervalValue());
    }




}
