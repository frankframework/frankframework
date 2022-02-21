package nl.nn.adapterframework.statistics;


import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

public class StatisticsKeeperTest {


	@Test
	public void testLineair() {
		StatisticsKeeper sk = new StatisticsKeeper("test");

		for (int i=0; i<100; i++) {
			sk.addValue(i);
		}

		assertEquals(100, sk.getCount());
		assertEquals(0, sk.getMin());
		assertEquals(99, sk.getMax());
		assertEquals(49.5, sk.getAvg(), 0.001);
		assertEquals(4950, sk.getTotal(), 0.001);
		assertEquals(841.0, sk.getVariance(), 0.001);
		assertEquals(49.5, getItemValueByName(sk, "p50"), 0.001);
		assertEquals(94.5, getItemValueByName(sk, "p95"), 0.001);
		assertEquals(97.5, getItemValueByName(sk, "p98"), 0.001);

	}

	double getItemValueByName(StatisticsKeeper sk, String name) {
		return (double)sk.getItemValue(sk.getItemIndex(name));
	}

	@Test
	public void testCompareDistributionSummary() {
		StatisticsKeeper sk = new StatisticsKeeper("test");
		sk.initMetrics(new SimpleMeterRegistry(), "group", new ArrayList<>());

		for (int i=0; i<100; i++) {
			sk.addValue(i);
		}

		assertEquals(sk.getCount(), sk.getDistributionSummary().count());
		assertEquals(sk.getMax(),   sk.getDistributionSummary().max(), 0.001);
		assertEquals(sk.getAvg(),   sk.getDistributionSummary().mean(), 0.001);
		assertEquals(sk.getTotal(), sk.getDistributionSummary().totalAmount(), 0.001);

	}

	@Test
	public void testGetMap() {
		StatisticsKeeper sk = new StatisticsKeeper("test");
		sk.initMetrics(new SimpleMeterRegistry(), "group", new ArrayList<>());

		for (int i=0; i<100; i++) {
			sk.addValue(i);
		}

		Map<String,Object> map = sk.asMap();
		assertMapValue(map, "Name", "test");
		assertMapValue(map, "Count", "100");
		assertMapValue(map, "Min", "0");
		assertMapValue(map, "Max", "99");
		assertMapValue(map, "Avg", "49.5");
		assertMapValue(map, "StdDev", "29.0");
		assertMapValue(map, "p50", "49.5");
		assertMapValue(map, "p90", "89.5");
		assertMapValue(map, "p95", "94.5");
		assertMapValue(map, "p98", "97.5");
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