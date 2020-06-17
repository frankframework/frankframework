package nl.nn.adapterframework.util;

import nl.nn.adapterframework.statistics.HasStatistics;
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
    public void testPerformActionSummaryOrFull() throws Exception {
        CounterStatistic cs = new CounterStatistic(10);
        cs.performAction(HasStatistics.STATISTICS_ACTION_SUMMARY);
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
        cs.performAction(HasStatistics.STATISTICS_ACTION_RESET);
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
        cs.performAction(HasStatistics.STATISTICS_ACTION_MARK_MAIN);
        assertEquals( 10, cs.getValue());
    }

    @Test
    public void testPerformActionMarkFull() throws Exception {
        CounterStatistic cs = new CounterStatistic(10);
        cs.performAction(HasStatistics.STATISTICS_ACTION_MARK_FULL);
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
