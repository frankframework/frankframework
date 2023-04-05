package nl.nn.adapterframework.statistics;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import nl.nn.adapterframework.statistics.HasStatistics.Action;

@RunWith(Parameterized.class)
public class StatisticsKeeperTest {

	@Parameter(0)
	public String  description=null;
	@Parameter(1)
	public Class<IBasics> basicsClass;

	@Parameters(name = "{index}: {0} - {1}")
	public static Collection estimators() {
		return Arrays.asList(new Object[][] {
			{ "classic", Basics.class },
			{ "micrometer", MicroMeterBasics.class }
		});
	}

	public StatisticsKeeper createStatisticsKeeper(boolean publishPercentiles, boolean publishHistograms, boolean calculatePercentiles) {
		try {
			StringTokenizer boundariesTokenizer = new StringTokenizer("100,1000,2000,10000",",");
			return new StatisticsKeeper("test", basicsClass.newInstance(), boundariesTokenizer, publishPercentiles, publishHistograms, calculatePercentiles, 2);
		} catch (InstantiationException | IllegalAccessException e) {
			fail(e.getMessage());
		}
		return null;
	}

	public StatisticsKeeper createStatisticsKeeper() {
		return createStatisticsKeeper(false, false, true);
	}

	public void testLineair(StatisticsKeeper sk) {
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

	public void testRandom(StatisticsKeeper sk, boolean withPercentiles) {
		int count = 10000;
		int limit = 100;

		long sumsq=0;

		Random rand = new Random();
		for (int i=0; i<count; i++) {
			int value = rand.nextInt(100);
			sumsq += value*value;
			sk.addValue(value);
		}

		assertEquals(count, sk.getCount());
		assertEquals(0, sk.getMin());
		assertEquals(99, sk.getMax());
		assertEquals(limit /2.0, sk.getAvg(), 1.0);
		assertEquals(limit *(count/2), sk.getTotal(), count*1.0);
		assertEquals(842.0, sk.getVariance(), 50.0);
		assertEquals(sumsq, sk.getTotalSquare(), 0.001);
		assertEquals(29.0, sk.getStdDev(), 1.0);
		if (withPercentiles) {
			assertEquals(49.5, getItemValueByName(sk, "p50"), 2.0);
			assertEquals(94.5, getItemValueByName(sk, "p95"), 2.0);
			assertEquals(97.5, getItemValueByName(sk, "p98"), 2.0);
		}
	}

	@Test
	public void testLineair() {
		StatisticsKeeper sk = createStatisticsKeeper();
		sk.initMetrics(new SimpleMeterRegistry(), "testLineair", null);
		testLineair(sk);
	}

	@Test
	public void testLineairPublishPercentiles() {
		StatisticsKeeper sk = createStatisticsKeeper(true, false, false);
		sk.initMetrics(new SimpleMeterRegistry(), "testLineair", null);
		testLineair(sk);
	}

	@Test
	public void testLineairPublishHistograms() {
		StatisticsKeeper sk = createStatisticsKeeper(false, true, false);
		sk.initMetrics(new SimpleMeterRegistry(), "testLineair", null);
		testLineair(sk);
	}

	@Test
	public void testRandom() {
		StatisticsKeeper sk = createStatisticsKeeper();
		sk.initMetrics(new SimpleMeterRegistry(), "testRandom", null);
		testRandom(sk, true);
	}

	@Test
	public void testRandomPublishPercentiles() {
		StatisticsKeeper sk = createStatisticsKeeper(true, false, false);
		sk.initMetrics(new SimpleMeterRegistry(), "testRandom", null);
		testRandom(sk, true);
	}

	@Test
	public void testRandomPublishHistograms() {
		StatisticsKeeper sk = createStatisticsKeeper(false, true, false);
		sk.initMetrics(new SimpleMeterRegistry(), "testRandom", null);
		assertEquals(16, sk.getItemCount());
		testRandom(sk, true);
	}

	@Test
	public void testRandomNoPercentiles() {
		StatisticsKeeper sk = createStatisticsKeeper(false, false, false);
		sk.initMetrics(new SimpleMeterRegistry(), "testRandom", null);
		assertEquals(12, sk.getItemCount());
		testRandom(sk, false);
	}


	double getItemValueByName(StatisticsKeeper sk, String name) {
		return (double)sk.getItemValue(sk.getItemIndex(name));
	}

	@Test
	public void testInterval() {
		StatisticsKeeper sk = createStatisticsKeeper();
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

	@Test
	public void testGetMap() {
		StatisticsKeeper sk = createStatisticsKeeper();
		sk.initMetrics(new SimpleMeterRegistry(), "group", new ArrayList<>());

		for (int i=0; i<100; i++) {
			sk.addValue(i*100);
		}

		Map<String,Object> map = sk.asMap();
		assertMapValue(map, "name", "test");
		assertMapValue(map, "count", "100");
		assertMapValue(map, "min", "0");
		assertMapValue(map, "max", "9900");
		assertMapValue(map, "avg", "4950.0");
		assertMapValue(map, "stdDev", "2901.1");
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

		assertEquals("name", labels.get(0));
		assertEquals("STRING", types.get(0));
		assertEquals("count", labels.get(1));
		assertEquals("INTEGER", types.get(1));
	}

}
