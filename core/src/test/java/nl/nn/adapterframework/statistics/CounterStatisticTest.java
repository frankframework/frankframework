package nl.nn.adapterframework.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import nl.nn.adapterframework.statistics.HasStatistics.Action;

/**
 * CounterStatistic Tester.
 *
 * @author <Sina Sen>
 * 
 */
public class CounterStatisticTest {

	/**
	 *
	 * Method: performAction(Action action)
	 *
	 */
	@Test
	public void testPerformActionSummaryOrFull() {
		CounterStatistic cs = new CounterStatistic(10);
		cs.initMetrics(new SimpleMeterRegistry(), "group", new ArrayList<>(), "counter under test");
		cs.performAction(Action.SUMMARY);
		assertEquals(10, cs.getValue());
	}

	/**
	 *
	 * Method: performAction(Action action)
	 *
	 */
	@Test
	public void testPerformActionMarkMain() {
		CounterStatistic cs = new CounterStatistic(10);
		cs.initMetrics(new SimpleMeterRegistry(), "group", new ArrayList<>(), "counter under test");
		cs.performAction(Action.MARK_MAIN);
		assertEquals(10, cs.getValue());
	}

	@Test
	public void testPerformActionMarkFull() {
		CounterStatistic cs = new CounterStatistic(10);
		cs.initMetrics(new SimpleMeterRegistry(), "group", new ArrayList<>(), "counter under test");
		cs.performAction(Action.MARK_FULL);
		assertEquals(10, cs.getValue());
	}

	/**
	 *
	 * Method: getIntervalValue()
	 *
	 */
	@Test
	public void testGetIntervalValue() {
		CounterStatistic cs = new CounterStatistic(10);
		cs.initMetrics(new SimpleMeterRegistry(), "group", new ArrayList<>(), "counter under test");
		assertEquals(0, cs.getIntervalValue());
	}

}
