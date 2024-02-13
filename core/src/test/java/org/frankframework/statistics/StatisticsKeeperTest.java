package org.frankframework.statistics;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.stream.Stream;

import org.frankframework.statistics.HasStatistics.Action;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

public class StatisticsKeeperTest {

	private static Stream<Arguments> data() {
		return Stream.of(
			Arguments.of(Basics.class),
			Arguments.of(MicroMeterBasics.class)
		);
	}

	public StatisticsKeeper createStatisticsKeeper(Class<IBasics> clazz, boolean publishPercentiles, boolean publishHistograms, boolean calculatePercentiles) {
		try {
			StringTokenizer boundariesTokenizer = new StringTokenizer("100,1000,2000,10000",",");
			return new StatisticsKeeper("test", clazz.newInstance(), boundariesTokenizer, publishPercentiles, publishHistograms, calculatePercentiles, 2);
		} catch (InstantiationException | IllegalAccessException e) {
			fail(e.getMessage());
		}
		return null;
	}

	public StatisticsKeeper createStatisticsKeeper(Class<IBasics> clazz) {
		return createStatisticsKeeper(clazz, false, false, true);
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
		assertEquals((limit-1)/2.0, sk.getAvg(), 1.5);
		assertEquals((limit-1)*(count/2), sk.getTotal(), count*1.2);
		assertEquals(842.0, sk.getVariance(), 50.0);
		assertEquals(sumsq, sk.getTotalSquare(), 0.001);
		assertEquals(29.0, sk.getStdDev(), 1.0);
		if (withPercentiles) {
			assertEquals(49.5, getItemValueByName(sk, "p50"), 2.0);
			assertEquals(94.5, getItemValueByName(sk, "p95"), 2.0);
			assertEquals(97.5, getItemValueByName(sk, "p98"), 2.0);
		}
	}

	@ParameterizedTest
	@MethodSource("data")
	public void testLineair(Class<IBasics> clazz) {
		StatisticsKeeper sk = createStatisticsKeeper(clazz);
		sk.initMetrics(new SimpleMeterRegistry(), "testLineair", null);
		testLineair(sk);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void testLineairPublishPercentiles(Class<IBasics> clazz) {
		StatisticsKeeper sk = createStatisticsKeeper(clazz, true, false, false);
		sk.initMetrics(new SimpleMeterRegistry(), "testLineair", null);
		testLineair(sk);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void testLineairPublishHistograms(Class<IBasics> clazz) {
		StatisticsKeeper sk = createStatisticsKeeper(clazz, false, true, false);
		sk.initMetrics(new SimpleMeterRegistry(), "testLineair", null);
		testLineair(sk);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void testRandom(Class<IBasics> clazz) {
		StatisticsKeeper sk = createStatisticsKeeper(clazz);
		sk.initMetrics(new SimpleMeterRegistry(), "testRandom", null);
		testRandom(sk, true);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void testRandomPublishPercentiles(Class<IBasics> clazz) {
		StatisticsKeeper sk = createStatisticsKeeper(clazz, true, false, false);
		sk.initMetrics(new SimpleMeterRegistry(), "testRandom", null);
		testRandom(sk, true);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void testRandomPublishHistograms(Class<IBasics> clazz) {
		StatisticsKeeper sk = createStatisticsKeeper(clazz, false, true, false);
		sk.initMetrics(new SimpleMeterRegistry(), "testRandom", null);
		assertEquals(16, sk.getItemCount());
		testRandom(sk, true);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void testRandomNoPercentiles(Class<IBasics> clazz) {
		StatisticsKeeper sk = createStatisticsKeeper(clazz, false, false, false);
		sk.initMetrics(new SimpleMeterRegistry(), "testRandom", null);
		assertEquals(12, sk.getItemCount());
		testRandom(sk, false);
	}


	double getItemValueByName(StatisticsKeeper sk, String name) {
		return (double)sk.getItemValue(sk.getItemIndex(name));
	}

	@ParameterizedTest
	@MethodSource("data")
	public void testInterval(Class<IBasics> clazz) {
		StatisticsKeeper sk = createStatisticsKeeper(clazz);
		sk.initMetrics(new SimpleMeterRegistry(), "testInterval", null);

		assertEquals(0L, sk.getIntervalItemValue(0), sk.getIntervalItemName(0)); // count
		assertEquals(null, sk.getIntervalItemValue(1), sk.getIntervalItemName(1)); // min
		assertEquals(null, sk.getIntervalItemValue(2), sk.getIntervalItemName(2)); // max
		assertEquals(null, sk.getIntervalItemValue(3), sk.getIntervalItemName(3)); // avg
		assertEquals(0L, sk.getIntervalItemValue(4), sk.getIntervalItemName(4)); // sum
		assertEquals(0L, sk.getIntervalItemValue(5), sk.getIntervalItemName(5)); // sumSq

		for (int i=0; i<100; i++) {
			sk.addValue(i);
		}

		assertEquals(100, sk.getCount());
		assertEquals(0, sk.getMin());
		assertEquals(99, sk.getMax());
		assertEquals(49.5, sk.getAvg(), 0.001);
		assertEquals(4950, sk.getTotal(), 0.001);
		assertEquals(841.0, sk.getVariance(), 0.001);

		assertEquals(100L, sk.getIntervalItemValue(0), sk.getIntervalItemName(0)); // count
		assertEquals(0L, sk.getIntervalItemValue(1), sk.getIntervalItemName(1)); // min
		assertEquals(99L, sk.getIntervalItemValue(2), sk.getIntervalItemName(2)); // max
		assertEquals(49.5, sk.getIntervalItemValue(3), sk.getIntervalItemName(3)); // avg
		assertEquals(4950L, sk.getIntervalItemValue(4), sk.getIntervalItemName(4)); // sum
		assertEquals(328350L, sk.getIntervalItemValue(5), sk.getIntervalItemName(5)); // sumSq

		sk.performAction(Action.MARK_FULL);

		assertEquals(0L, sk.getIntervalItemValue(0), sk.getIntervalItemName(0)); // count
		assertEquals(null, sk.getIntervalItemValue(1), sk.getIntervalItemName(1)); // min
		assertEquals(null, sk.getIntervalItemValue(2), sk.getIntervalItemName(2)); // max
		assertEquals(null, sk.getIntervalItemValue(3), sk.getIntervalItemName(3)); // avg
		assertEquals(0L, sk.getIntervalItemValue(4), sk.getIntervalItemName(4)); // sum
		assertEquals(0L, sk.getIntervalItemValue(5), sk.getIntervalItemName(5)); // sumSq


		for (int i=200; i<300; i++) {
			sk.addValue(i);
		}

		assertEquals(200, sk.getCount());
		assertEquals(0, sk.getMin());
		assertEquals(299, sk.getMax());
		assertEquals(149.5, sk.getAvg(), 0.001);
		assertEquals(29900, sk.getTotal(), 0.001);
		assertEquals(10887.0, sk.getVariance(), 0.001);

		assertEquals(100L, sk.getIntervalItemValue(0), sk.getIntervalItemName(0)); // count
		assertEquals(200L, sk.getIntervalItemValue(1), sk.getIntervalItemName(1)); // min
		assertEquals(299L, sk.getIntervalItemValue(2), sk.getIntervalItemName(2)); // max
		assertEquals(249.5, sk.getIntervalItemValue(3), sk.getIntervalItemName(3)); // avg
		assertEquals(24950L, sk.getIntervalItemValue(4), sk.getIntervalItemName(4)); // sum
		assertEquals(6308350L, sk.getIntervalItemValue(5), sk.getIntervalItemName(5)); // sumSq

	}

	@ParameterizedTest
	@MethodSource("data")
	public void testGetMap(Class<IBasics> clazz) {
		StatisticsKeeper sk = createStatisticsKeeper(clazz);
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
