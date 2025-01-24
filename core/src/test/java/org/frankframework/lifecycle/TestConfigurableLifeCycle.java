package org.frankframework.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ScheduleManager;
import org.frankframework.core.Adapter;
import org.frankframework.core.IPipe;
import org.frankframework.core.PipeLine;
import org.frankframework.monitoring.AdapterFilter;
import org.frankframework.monitoring.EventType;
import org.frankframework.monitoring.IMonitorDestination;
import org.frankframework.monitoring.ITrigger.TriggerType;
import org.frankframework.monitoring.Monitor;
import org.frankframework.monitoring.MonitorManager;
import org.frankframework.monitoring.Severity;
import org.frankframework.monitoring.SourceFiltering;
import org.frankframework.monitoring.Trigger;
import org.frankframework.monitoring.events.MonitorEvent;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.testutil.JunitTestClassLoaderWrapper;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.XmlBuilder;

public class TestConfigurableLifeCycle {
	private static final String TEST_MONITOR_NAME = "TestMonitor";
	private static final String TEST_TRIGGER_EVENT_NAME = "testEvent1";

	private Configuration configuration;
	private MonitorManager monitorManager;
	private ScheduleManager scheduleManager;
	private Adapter adapter;
	private Monitor monitor;

	@BeforeEach
	public void setup() throws Exception {
		configuration = new Configuration();
		ClassLoader classLoader = new JunitTestClassLoaderWrapper(); // Add ability to retrieve classes from src/test/resources
		configuration.setClassLoader(classLoader); // Add the test classpath
		configuration.setConfigLocation("testMonitoringContext.xml");
		configuration.refresh();

		DefaultListableBeanFactory cbf = (DefaultListableBeanFactory) configuration.getAutowireCapableBeanFactory();
		cbf.destroySingleton("scheduleManager"); // Remove the default scheduleManager so we can use Mockito.spy()

		monitorManager = (configuration.getBean("monitorManager", MonitorManager.class));
		scheduleManager = spy(SpringUtils.createBean(configuration, ScheduleManager.class));
		SpringUtils.registerSingleton(configuration, "scheduleManager", scheduleManager);

		adapter = createAdapter();
		monitor = createMonitor();
	}

	private static final class InvocationQueue {
		private Queue<Object> configurables = new ConcurrentLinkedQueue<>();
		@SneakyThrows

		public Void answer(InvocationOnMock invocation) {
			configurables.add(invocation.getMock());
			invocation.callRealMethod();
			return null;
		}

		public Object peek() {
			return configurables.peek();
		}

		public Object poll() {
			return configurables.poll();
		}
	}

	private Adapter createAdapter() throws Exception {
		Adapter adapter = spy(SpringUtils.createBean(configuration, Adapter.class));
		adapter.setName("adapterName");
		IPipe pipe = SpringUtils.createBean(adapter, EchoPipe.class);
		pipe.setName("test-pipe");
		PipeLine pipeline = SpringUtils.createBean(adapter, PipeLine.class);
		pipeline.addPipe(pipe);
		adapter.setPipeLine(pipeline);
		configuration.addAdapter(adapter);

		List<Adapter> adapters = configuration.getRegisteredAdapters();
		assertEquals(1, adapters.size());
		return adapters.get(0);
	}

	private Monitor createMonitor() throws Exception {
		Monitor monitor = spy(SpringUtils.createBean(monitorManager, Monitor.class));
		monitor.setName(TEST_MONITOR_NAME);
		monitor.setType(EventType.FUNCTIONAL);
		Trigger trigger = SpringUtils.createBean(monitorManager, Trigger.class);
		AdapterFilter filter = SpringUtils.createBean(monitorManager, AdapterFilter.class);
		filter.setAdapter("dummyAdapterName");
		filter.addSubObjectText("dummySubObjectName");
		trigger.setPeriod(42);
		trigger.setThreshold(1337);
		trigger.setSeverity(Severity.HARMLESS);
		trigger.setTriggerType(TriggerType.ALARM);
		trigger.addAdapterFilter(filter);
		trigger.setSourceFiltering(SourceFiltering.ADAPTER);
		trigger.addEventCodeText(TEST_TRIGGER_EVENT_NAME);
		monitor.addTrigger(trigger);
		monitorManager.addMonitor(monitor);
		IMonitorDestination ima = new IMonitorDestination() {
			private @Getter @Setter String name = "mockDestination";

			@Override
			public void configure() throws ConfigurationException {
				// Nothing to configure, dummy class
			}

			@Override
			public void fireEvent(String monitor, EventType eventType, Severity severity, String eventCode, MonitorEvent message) {
				// Nothing to do, dummy class
			}

			@Override
			public XmlBuilder toXml() {
				return new XmlBuilder("destination");
			}

			@Override
			public void start() {
				// Nothing to do, dummy class
			}

			@Override
			public void stop() {
				// Nothing to do, dummy class
			}

			@Override
			public boolean isRunning() {
				return false;
			}
		};
		monitorManager.addDestination(ima);
		return monitor;
	}

	/**
	 * Test if calling configure on {@link Configuration} propagates to {@link Monitor monitors}, Schedules and {@link Adapter adapters}.
	 * Verify the order in which they are started, configured and stopped.
	 */
	@Test
	public void testConfigureStartAndStop() throws Exception {
		InvocationQueue configureQueue = new InvocationQueue();
		doAnswer(configureQueue::answer).when(adapter).configure(); // Phase 100
		doAnswer(configureQueue::answer).when(scheduleManager).configure(); // Phase 200
		doAnswer(configureQueue::answer).when(monitor).configure(); // Phase 300

		InvocationQueue startQueue = new InvocationQueue();
		doAnswer(startQueue::answer).when(adapter).start(); // Phase 100
		doAnswer(startQueue::answer).when(scheduleManager).start(); // Phase 200
		doAnswer(startQueue::answer).when(monitor).start(); // Phase 300

		InvocationQueue stopQueue = new InvocationQueue();
		doAnswer(stopQueue::answer).when(adapter).stop(any(Runnable.class)); // Phase 100
		doAnswer(stopQueue::answer).when(scheduleManager).stop(); // Phase 200
		doAnswer(stopQueue::answer).when(monitor).stop(); // Phase 300

		// Act
		configuration.configure();

		// Assert
		verify(adapter, times(1)).configure();
		verify(scheduleManager, times(1)).configure();
		verify(monitor, times(1)).configure();

		// Order matters here!
		assertInstanceOf(Adapter.class, configureQueue.poll());
		assertInstanceOf(ScheduleManager.class, configureQueue.poll());
		assertInstanceOf(Monitor.class, configureQueue.poll());

		verify(adapter, times(1)).start();
		verify(scheduleManager, times(1)).start();
		verify(monitor, times(1)).start();

		// Order matters here!
		assertInstanceOf(Adapter.class, startQueue.poll());
		assertInstanceOf(ScheduleManager.class, startQueue.poll());
		assertInstanceOf(Monitor.class, startQueue.poll());

		// Act
		configuration.stop();

		// Assert
		verify(adapter, times(1)).stop(any(Runnable.class));
		verify(scheduleManager, times(1)).stop();
		verify(monitor, times(1)).stop();

		// (Reversed) order matters here!
		assertInstanceOf(Monitor.class, stopQueue.poll());
		assertInstanceOf(ScheduleManager.class, stopQueue.poll());
		Awaitility.await().atMost(2500, TimeUnit.MILLISECONDS).until(stopQueue::peek, Objects::nonNull); // Give the adapter some time to stop
		assertInstanceOf(Adapter.class, stopQueue.poll());
	}
}
