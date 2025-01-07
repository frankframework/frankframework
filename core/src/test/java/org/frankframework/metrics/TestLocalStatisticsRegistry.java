package org.frankframework.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.json.Json;
import jakarta.json.JsonStructure;
import jakarta.json.JsonWriter;
import jakarta.json.stream.JsonGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.core.PipeLine;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.receivers.JavaListener;
import org.frankframework.receivers.Receiver;
import org.frankframework.statistics.FrankMeterType;
import org.frankframework.statistics.MetricsInitializer;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.SpringUtils;

public class TestLocalStatisticsRegistry {
	private TestConfiguration configuration;
	private LocalStatisticsRegistry registry;
	private final AtomicInteger inProcessInt = new AtomicInteger();

	private Adapter adapter;

	@BeforeEach
	void beforeEach() throws ConfigurationException {
		configuration = new TestConfiguration(false);
		registry = configuration.getBean("meterRegistry", LocalStatisticsRegistry.class);
		MetricsInitializer configurationMetrics = configuration.getBean("configurationMetrics", MetricsInitializer.class);

		createAndRegisterAdapter();

		// Create Dummy Gauge to test with
		configurationMetrics.createGauge(adapter, FrankMeterType.PIPELINE_IN_PROCESS, inProcessInt::doubleValue);

		// Configure the configuration (which initializes the meters)
		configuration.configure();

		// Ensure only 1 adapter exists
		assertEquals(1, configuration.getAdapterList().size());
	}

	@SuppressWarnings("unchecked")
	private <M> void createAndRegisterAdapter() throws ConfigurationException {
		adapter = configuration.createBean(Adapter.class);
		adapter.setName("myAdapter");
		PipeLine pipeline = SpringUtils.createBean(adapter, PipeLine.class);
		EchoPipe echoPipe = SpringUtils.createBean(adapter, EchoPipe.class);
		echoPipe.setName("echoPipe");
		pipeline.addPipe(echoPipe);
		adapter.setPipeLine(pipeline);

		Receiver<M> receiver = SpringUtils.createBean(adapter, Receiver.class);
		receiver.setName("myReceiver");
		JavaListener<M> listener = SpringUtils.createBean(adapter, JavaListener.class);
		receiver.setListener(listener);
		adapter.addReceiver(receiver);

		configuration.addAdapter(adapter);
	}

	private static String jsonStructureToString(JsonStructure payload) {
		Writer writer = new StringWriter();
		Map<String, Boolean> config = new HashMap<>();
		config.put(JsonGenerator.PRETTY_PRINTING, false);

		try (JsonWriter jsonWriter = Json.createWriterFactory(config).createWriter(writer)) {
			jsonWriter.write(payload);
		}

		return writer.toString();
	}

	@Test
	void testLocalStatisticsRegistry() throws Exception {
		JsonStructure json = registry.scrape(configuration.getName(), adapter);
		String expected = TestFileUtils.getTestFile("/metrics/myAdapter.json");
		MatchUtils.assertJsonEquals(expected, jsonStructureToString(json));

		inProcessInt.addAndGet(100);

		JsonStructure json2 = registry.scrape(configuration.getName(), adapter);
		String expected2 = TestFileUtils.getTestFile("/metrics/myAdapter2.json");
		MatchUtils.assertJsonEquals(expected2, jsonStructureToString(json2));
	}
}
