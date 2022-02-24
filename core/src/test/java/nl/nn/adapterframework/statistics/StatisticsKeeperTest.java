package nl.nn.adapterframework.statistics;


import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import nl.nn.adapterframework.statistics.HasStatistics.Action;

public class StatisticsKeeperTest {


	@Test
	public void testLineair() {
		StatisticsKeeper sk = new StatisticsKeeper("test");
		sk.initMetrics(new SimpleMeterRegistry(), "testLineair", null);

		for (int i=0; i<100; i++) {
			sk.addValue(i);
		}

		assertEquals(100, sk.getCount());
		assertEquals(0, sk.getMin());
		assertEquals(99, sk.getMax());
		assertEquals(49.5, sk.getAvg(), 0.001);
		assertEquals(4950, sk.getTotal(), 0.001);
		assertEquals(841.0, sk.getVariance(), 0.001);
		assertEquals(328350, sk.getTotalSquare(), 0.001);
		assertEquals(29.0, sk.getStdDev(), 0.001);
		assertEquals(49.5, getItemValueByName(sk, "p50"), 0.5);
		assertEquals(94.5, getItemValueByName(sk, "p95"), 1.5);
		assertEquals(97.5, getItemValueByName(sk, "p98"), 2.5);

	}

	double getItemValueByName(StatisticsKeeper sk, String name) {
		return (double)sk.getItemValue(sk.getItemIndex(name));
	}
	
	@Test
	public void testInterval() {
		StatisticsKeeper sk = new StatisticsKeeper("test");
		sk.initMetrics(new SimpleMeterRegistry(), "testInterval", null);

		assertEquals(sk.getIntervalItemName(0), 0L,   sk.getIntervalItemValue(0)); // count
		assertEquals(sk.getIntervalItemName(1), null, sk.getIntervalItemValue(1)); // min
		assertEquals(sk.getIntervalItemName(2), null, sk.getIntervalItemValue(2)); // max
		assertEquals(sk.getIntervalItemName(3), null, sk.getIntervalItemValue(3)); // avg
		assertEquals(sk.getIntervalItemName(4), 0L,   sk.getIntervalItemValue(4)); // sum
		assertEquals(sk.getIntervalItemName(5), 0L,   sk.getIntervalItemValue(5)); // sumSq

		for (int i=0; i<100; i++) {
			sk.addValue(i);
		}

		assertEquals(100, sk.getCount());
		assertEquals(0, sk.getMin());
		assertEquals(99, sk.getMax());
		assertEquals(49.5, sk.getAvg(), 0.001);
		assertEquals(4950, sk.getTotal(), 0.001);
		assertEquals(841.0, sk.getVariance(), 0.001);

		assertEquals(sk.getIntervalItemName(0), 100L,    sk.getIntervalItemValue(0)); // count
		assertEquals(sk.getIntervalItemName(1), 0L,      sk.getIntervalItemValue(1)); // min
		assertEquals(sk.getIntervalItemName(2), 99L,     sk.getIntervalItemValue(2)); // max
		assertEquals(sk.getIntervalItemName(3), 49.5,    sk.getIntervalItemValue(3)); // avg
		assertEquals(sk.getIntervalItemName(4), 4950L,   sk.getIntervalItemValue(4)); // sum
		assertEquals(sk.getIntervalItemName(5), 328350L, sk.getIntervalItemValue(5)); // sumSq

		sk.performAction(Action.MARK_FULL);

		assertEquals(sk.getIntervalItemName(0), 0L,   sk.getIntervalItemValue(0)); // count
		assertEquals(sk.getIntervalItemName(1), null, sk.getIntervalItemValue(1)); // min
		assertEquals(sk.getIntervalItemName(2), null, sk.getIntervalItemValue(2)); // max
		assertEquals(sk.getIntervalItemName(3), null, sk.getIntervalItemValue(3)); // avg
		assertEquals(sk.getIntervalItemName(4), 0L,   sk.getIntervalItemValue(4)); // sum
		assertEquals(sk.getIntervalItemName(5), 0L,   sk.getIntervalItemValue(5)); // sumSq

		
		for (int i=200; i<300; i++) {
			sk.addValue(i);
		}

		assertEquals(200, sk.getCount());
		assertEquals(0, sk.getMin());
		assertEquals(299, sk.getMax());
		assertEquals(149.5, sk.getAvg(), 0.001);
		assertEquals(29900, sk.getTotal(), 0.001);
		assertEquals(10887.0, sk.getVariance(), 0.001);

		assertEquals(sk.getIntervalItemName(0), 100L,     sk.getIntervalItemValue(0)); // count
		assertEquals(sk.getIntervalItemName(1), 200L,     sk.getIntervalItemValue(1)); // min
		assertEquals(sk.getIntervalItemName(2), 299L,     sk.getIntervalItemValue(2)); // max
		assertEquals(sk.getIntervalItemName(3), 249.5,    sk.getIntervalItemValue(3)); // avg
		assertEquals(sk.getIntervalItemName(4), 24950L,   sk.getIntervalItemValue(4)); // sum
		assertEquals(sk.getIntervalItemName(5), 6308350L, sk.getIntervalItemValue(5)); // sumSq
		
	}


//	@Test
//	public void testCompareDistributionSummary() {
//		StatisticsKeeper sk = new StatisticsKeeper("test");
//		sk.initMetrics(new SimpleMeterRegistry(), "group", new ArrayList<>());
//
//		for (int i=0; i<100; i++) {
//			sk.addValue(i);
//		}
//
//		assertEquals(sk.getCount(),  sk.getDistributionSummary().count());
//		assertEquals(sk.getMax(),    sk.getDistributionSummary().max(), 0.001);
//		assertEquals(sk.getAvg(),    sk.getDistributionSummary().mean(), 0.001);
//		assertEquals(sk.getTotal(),  sk.getDistributionSummary().totalAmount(), 0.001);
//
//		
//		HistogramSnapshot snapshot = sk.getDistributionSummary().takeSnapshot();
//		snapshot.outputSummary(System.out, 0.01);
////		snapshot.
////		for (Measurement measurement:sk.getDistributionSummary().measure()) {
////			System.out.println("measurement: "+ToStringBuilder.reflectionToString(measurement));
////		}
////		
//	}

	@Test
	public void testGetMap() {
		StatisticsKeeper sk = new StatisticsKeeper("test");
		sk.initMetrics(new SimpleMeterRegistry(), "group", new ArrayList<>());

		for (int i=0; i<100; i++) {
			sk.addValue(i*100);
		}

		Map<String,Object> map = sk.asMap();
		assertMapValue(map, "Name", "test");
		assertMapValue(map, "Count", "100");
		assertMapValue(map, "Min", "0");
		assertMapValue(map, "Max", "9900");
		assertMapValue(map, "Avg", "4950.0");
		assertMapValue(map, "StdDev", "2901.1");
		assertMapValue(map, "100ms", "1.0");
		assertMapValue(map, "1000ms", "10.0");
		assertMapValue(map, "2000ms", "20.0");
		assertMapValue(map, "10000ms", "100.0");
//		assertMapValue(map, "p50", "4950.0");
//		assertMapValue(map, "p90", "8950.0");
//		assertMapValue(map, "p95", "9450.0");
//		assertMapValue(map, "p98", "9750.0");
	}

	public void assertMapValue(Map<String,Object> map, String key, String value) {
		assertEquals(value, map.get(key).toString());
	}

	@Test
	public void testLabelsAndTypes() {
		List<String> labels = StatisticsKeeper.getLabels();
		List<String> types  = StatisticsKeeper.getTypes();

		assertEquals("Name", labels.get(0));
		assertEquals("STRING", types.get(0));
		assertEquals("Count", labels.get(1));
		assertEquals("INTEGER", types.get(1));
	}

}