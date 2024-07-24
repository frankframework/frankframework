package org.frankframework.senders;

import static org.frankframework.testutil.mock.WaitUtils.waitForState;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.frankframework.configuration.AdapterManager;
import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.IbisManager;
import org.frankframework.core.Adapter;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineExit;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.jta.narayana.NarayanaJtaTransactionManager;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.pipes.GetFromSession;
import org.frankframework.processors.CorePipeLineProcessor;
import org.frankframework.processors.CorePipeProcessor;
import org.frankframework.processors.PipeProcessor;
import org.frankframework.receivers.JavaListener;
import org.frankframework.receivers.Receiver;
import org.frankframework.receivers.ServiceDispatcher;
import org.frankframework.stream.Message;
import org.frankframework.testutil.ParameterBuilder;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.RunState;

import nl.nn.adapterframework.dispatcher.DispatcherException;
import nl.nn.adapterframework.dispatcher.DispatcherManagerFactory;

class FrankSenderTest {
	private static final String TARGET_SERVICE_NAME = "TEST_TARGET_SERVICE";

	private PipeLineSession session;
	private Message input;
	private SenderResult result;

	@BeforeEach
	void setUp() {
	}

	@AfterEach
	void tearDown() {
		CloseUtils.closeSilently(input, result, session);

		// In case JavaListener didn't close after end of test, deregister the service.
		ServiceDispatcher.getInstance().unregisterServiceClient(TARGET_SERVICE_NAME);
		try {
			DispatcherManagerFactory.getDispatcherManager().unregister(TARGET_SERVICE_NAME);
		} catch (DispatcherException e) {
			// Ignore
		}
	}

	@Test
	void configureNoTargetConfigured() {
		// Arrange
		FrankSender sender = new FrankSender();

		// Act / Assert
		assertThrows(ConfigurationException.class, sender::configure);
	}

	@Test
	void configureInvalidTargetConfigured() {
		// Arrange
		FrankSender sender = new FrankSender();
		sender.setScope(FrankSender.Scope.ADAPTER);
		sender.setTarget("invalid");
		AdapterManager adapterManager = mock();
		sender.setAdapterManager(adapterManager);

		// Act / Assert
		assertThrows(ConfigurationException.class, sender::configure);
	}

	@Test
	void configureValidAdapterTargetConfigured() {
		// Arrange
		FrankSender sender = new FrankSender();
		sender.setScope(FrankSender.Scope.ADAPTER);
		sender.setTarget("adapterName");
		AdapterManager adapterManager = mock();
		sender.setAdapterManager(adapterManager);
		Adapter adapter = mock();
		when(adapterManager.getAdapter("adapterName")).thenReturn(adapter);

		// Act / Assert
		assertDoesNotThrow(sender::configure);
	}

	@Test
	void configureTargetViaParameter() {
		// Arrange
		FrankSender sender = new FrankSender();
		sender.setScope(FrankSender.Scope.ADAPTER);
		sender.addParameter(ParameterBuilder.create("target", "adapterName"));

		// Act / Assert
		assertDoesNotThrow(sender::configure);
	}

	@Test
	void configureTargetIsScopeJvm() {
		// Arrange
		FrankSender sender = new FrankSender();
		sender.setScope(FrankSender.Scope.JVM);
		sender.setTarget("serviceName");

		// Act / Assert
		assertDoesNotThrow(sender::configure);
	}

	@ParameterizedTest
	@CsvSource({
			"JVM, RemoteService, false, false, JVM/RemoteService",
			"ADAPTER, Configuration/AdapterName, false, false, ADAPTER/Configuration/AdapterName",
			"DLL, RemoteService, true, false, param:scope/RemoteService",
			"JVM, RemoteService, false, true, JVM/param:target",
			"ADAPTER, AdapterName, false, true, ADAPTER/param:target",
			"ADAPTER, AdapterName, true, true, param:scope/param:target",
	})
	void getPhysicalDestinationName(FrankSender.Scope scope, String target, boolean scopeParam, boolean targetParam, String expected) {
		// Arrange
		FrankSender sender = new FrankSender();
		sender.setScope(scope);
		sender.setTarget(target);
		if (scopeParam) {
			sender.addParameter(ParameterBuilder.create("scope", ""));
		}
		if (targetParam) {
			sender.addParameter(ParameterBuilder.create("target", ""));
		}

		// Act
		String actual = sender.getPhysicalDestinationName();

		// Assert
		assertEquals(expected, actual);
	}

	@ParameterizedTest
	@CsvSource({
			"ADAPTER, false, ADAPTER",
			"DLL, false, DLL",
			"JVM, false, JVM",
			"JVM, true, Dynamic",
			"DLL, true, Dynamic",
			"ADAPTER, true, Dynamic",
	})
	void getDomain(FrankSender.Scope scope, boolean scopeParam, String expected) {
		// Arrange
		FrankSender sender = new FrankSender();
		sender.setScope(scope);
		if (scopeParam) {
			sender.addParameter(ParameterBuilder.create("scope", ""));
		}

		// Act
		String actual = sender.getDomain();

		// Assert
		assertEquals(expected, actual);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"adapterName",
			"configurationName/adapterName",
			"/adapterName"
	})
	void findAdapterSuccess(String target) throws SenderException {
		// Arrange
		FrankSender sender = new FrankSender();

		Adapter adapter = mock();
		AdapterManager adapterManager = mock();
		IbisManager ibisManager = mock();
		Configuration configuration = mock();

		sender.setIbisManager(ibisManager);
		sender.setAdapterManager(adapterManager);

		when(ibisManager.getConfiguration("configurationName")).thenReturn(configuration);
		when(configuration.getAdapterManager()).thenReturn(adapterManager);
		when(adapterManager.getAdapter("adapterName")).thenReturn(adapter);

		// Act
		Adapter actual = sender.findAdapter(target);

		// Assert
		assertNotNull(actual);
		assertSame(adapter, actual);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"noSuchAdapter",
			"configurationName/noSuchAdapter",
			"noSuchConfig/adapterName",
			"/noSuchAdapter"
	})
	void findAdapterFailure(String target) {
		// Arrange
		FrankSender sender = new FrankSender();

		Adapter adapter = mock();
		AdapterManager adapterManager = mock();
		IbisManager ibisManager = mock();
		Configuration configuration = mock();

		sender.setIbisManager(ibisManager);
		sender.setAdapterManager(adapterManager);

		when(ibisManager.getConfiguration("configurationName")).thenReturn(configuration);
		when(configuration.getAdapterManager()).thenReturn(adapterManager);
		when(adapterManager.getAdapter("adapterName")).thenReturn(adapter);

		// Act / Assert
		assertThrows(SenderException.class, () -> sender.findAdapter(target));
	}

	@ParameterizedTest
	@CsvSource({
			"JVM, , JVM",
			", DLL, DLL",
			"JVM, ADAPTER, ADAPTER",
	})
	void determineActualScope(FrankSender.Scope configuredScope, String scopeParamValue, FrankSender.Scope expected) throws Exception {
		// Arrange
		FrankSender sender = new FrankSender();
		if (configuredScope != null) {
			sender.setScope(configuredScope);
		}
		if (scopeParamValue != null) {
			sender.addParameter(ParameterBuilder.create("scope", scopeParamValue));
		}
		sender.addParameter(ParameterBuilder.create("target", "dummy"));
		sender.configure();
		session = new PipeLineSession();
		ParameterValueList pvl = sender.getParameterValueList(Message.nullMessage(), session);

		// Act
		FrankSender.Scope actual = sender.determineActualScope(pvl);

		// Assert
		assertEquals(expected, actual);
	}

	@ParameterizedTest
	@CsvSource({
			"t1, t2, t2",
			"t1, , t1",
			", t2, t2"
	})
	void determineActualTarget(String configuredTarget, String targetParamValue, String expected) throws Exception {
		// Arrange
		FrankSender sender = new FrankSender();
		if (configuredTarget != null) {
			sender.setTarget(configuredTarget);
			AdapterManager adapterManager = mock();
			Adapter adapter = mock();
			when(adapterManager.getAdapter(anyString())).thenReturn(adapter);
			sender.setAdapterManager(adapterManager);
		}
		if (targetParamValue != null) {
			sender.addParameter(ParameterBuilder.create("target", targetParamValue));
		}
		sender.configure();
		session = new PipeLineSession();
		ParameterValueList pvl = sender.getParameterValueList(Message.nullMessage(), session);

		// Act
		String actual = sender.determineActualTarget(pvl);

		// Assert
		assertEquals(expected, actual);
	}

	@ParameterizedTest
	@CsvSource({
			"ADAPTER, true, cid1, reply",
			"JVM, true, cid1, reply",
			"ADAPTER, true, , reply",
			"JVM, true, , reply",
			"ADAPTER, false, , reply",
			"JVM, false, , reply",
	})
	void sendMessage(FrankSender.Scope scope, boolean callSync, String correlationId, String expected) throws Exception {
		// Arrange
		TestConfiguration configuration = new TestConfiguration(false);
		FrankSender sender = configuration.createBean(FrankSender.class);
		sender.setTarget(TARGET_SERVICE_NAME);
		sender.setScope(scope);
		sender.setSynchronous(callSync);
		sender.setReturnedSessionKeys("session-key,cid");
		sender.addParameter(ParameterBuilder
				.create("session-key", null)
				.withSessionKey("session-key"));

		Semaphore semaphore = new Semaphore(0);
		Adapter targetAdapter = createAdapter(configuration, semaphore);
		if (scope != FrankSender.Scope.ADAPTER) {
			createJavaListener(configuration, targetAdapter);
		}
		if (!callSync) {
			IsolatedServiceCaller isc = configuration.createBean(IsolatedServiceCaller.class);
			sender.setIsolatedServiceCaller(isc);
		}

		configuration.configure();
		configuration.start();
		sender.configure();

		session = new PipeLineSession();
		session.put("session-key", expected);
		session.put(PipeLineSession.CORRELATION_ID_KEY, correlationId);

		input = new Message("request");

		waitForState(targetAdapter, RunState.STARTED);

		// Act
		result = sender.sendMessage(input, session);

		// Assert
		assertNotNull(result);
		assertNotNull(result.getResult());

		Message resultMessage = result.getResult();
		if (callSync) {
			assertNotNull(resultMessage.asString());
			String resultString = resultMessage.asString();
			assertEquals(expected, resultString);
			if (correlationId != null) {
				assertThat(session, Matchers.hasKey(PipeLineSession.CORRELATION_ID_KEY));
				assertEquals(correlationId, session.getCorrelationId());
			}
		} else {
			assertTrue(Message.isNull(resultMessage), "Expected result from async call to be a NULL Message");
			// TODO: Check async adapter finished
			boolean acquired = semaphore.tryAcquire(1, TimeUnit.SECONDS);
			assertTrue(acquired, "Failed to acquire semaphore, appears as if async adapter was not executed");
		}
	}

	private void createJavaListener(TestConfiguration configuration, Adapter targetAdapter) throws ListenerException {
		@SuppressWarnings("unchecked")
		Receiver<String> receiver = configuration.createBean(Receiver.class);
		configuration.autowireByName(receiver);
		receiver.setName("TargetAdapter-receiver");

		@SuppressWarnings("unchecked")
		JavaListener<String> listener = configuration.createBean(JavaListener.class);
		listener.setName(receiver.getName());
		listener.setServiceName(TARGET_SERVICE_NAME);
		listener.setHandler(receiver);

		receiver.setListener(listener);
		receiver.setTxManager(configuration.createBean(NarayanaJtaTransactionManager.class));

		targetAdapter.registerReceiver(receiver);

		listener.open();
	}

	private Adapter createAdapter(TestConfiguration configuration, Semaphore semaphore) throws ConfigurationException {
		Adapter adapter = configuration.createBean(Adapter.class);
		configuration.autowireByName(adapter);
		adapter.setName(TARGET_SERVICE_NAME);

		CorePipeLineProcessor plp = configuration.createBean(CorePipeLineProcessor.class);
		PipeProcessor pp = configuration.createBean(CorePipeProcessor.class);
		plp.setPipeProcessor(pp);
		PipeLine pl = configuration.createBean(PipeLine.class);
		configuration.autowireByName(pl);
		pl.setPipeLineProcessor(plp);
		GetFromSession pipe = new GetFromSession() {
			@Override
			public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
				semaphore.release();
				return super.doPipe(message, session);
			}
		};
		pipe.setSessionKey("session-key");
		pipe.setName("test-pipe");
		pl.addPipe(pipe);
		pl.setFirstPipe(pipe.getName());

		PipeLine.ExitState exitState = PipeLine.ExitState.SUCCESS;
		PipeLineExit ple = new PipeLineExit();
		ple.setName(exitState.name());
		ple.setState(exitState);
		pl.registerPipeLineExit(ple);
		adapter.setPipeLine(pl);

		configuration.registerAdapter(adapter);
		return adapter;
	}

}
