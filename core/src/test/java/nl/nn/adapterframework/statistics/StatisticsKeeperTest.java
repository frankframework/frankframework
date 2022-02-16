package nl.nn.adapterframework.statistics;


import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import liquibase.repackaged.org.apache.commons.lang3.builder.ToStringBuilder;
import liquibase.repackaged.org.apache.commons.lang3.builder.ToStringStyle;

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
		sk.initMetrics(new SimpleMeterRegistry(), "test", new ArrayList<>());
		
		for (int i=0; i<100; i++) {
			sk.addValue(i);
		}
		
		System.out.println(ToStringBuilder.reflectionToString(sk.getDistributionSummary(),ToStringStyle.MULTI_LINE_STYLE));

		assertEquals(sk.getCount(), sk.getDistributionSummary().count());
		assertEquals(sk.getMax(),   sk.getDistributionSummary().max(), 0.001);
		assertEquals(sk.getAvg(),   sk.getDistributionSummary().mean(), 0.001);
		assertEquals(sk.getTotal(), sk.getDistributionSummary().totalAmount(), 0.001);
		
	}

}
