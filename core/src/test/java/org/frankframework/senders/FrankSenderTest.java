package org.frankframework.senders;

import static org.frankframework.testutil.mock.WaitUtils.waitForState;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.hasKey;
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

import jakarta.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import lombok.extern.log4j.Log4j2;

import nl.nn.adapterframework.dispatcher.DispatcherException;
import nl.nn.adapterframework.dispatcher.DispatcherManagerFactory;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.IbisManager;
import org.frankframework.core.Adapter;
import org.frankframework.core.IPipe;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineExit;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.jta.narayana.NarayanaJtaTransactionManager;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.pipes.AbstractPipe;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.pipes.ExceptionPipe;
import org.frankframework.pipes.GetFromSession;
import org.frankframework.processors.CorePipeLineProcessor;
import org.frankframework.processors.CorePipeProcessor;
import org.frankframework.processors.PipeProcessor;
import org.frankframework.receivers.FrankListener;
import org.frankframework.receivers.JavaListener;
import org.frankframework.receivers.Receiver;
import org.frankframework.receivers.ServiceClient;
import org.frankframework.receivers.ServiceDispatcher;
import org.frankframework.senders.FrankSender.Scope;
import org.frankframework.stream.Message;
import org.frankframework.testutil.ParameterBuilder;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.RunState;

@Log4j2
class FrankSenderTest {
	private static final String TARGET_SERVICE_NAME = "TEST_TARGET_SERVICE";

	private PipeLineSession session;
	private Message input;
	private SenderResult result;
	private TestConfiguration configuration;
	private FrankListener frankListener;

	@AfterEach
	void tearDown() {
		log.debug("FrankSenderTest: Teardown start, has configuration? [{}]", configuration != null);
		log.debug("FrankSenderTest: Closing Configuration and other resources");
		CloseUtils.closeSilently(input, result, session, configuration);

		// In case JavaListener didn't close after end of test, deregister the service.
		log.debug("FrankSenderTest: Unregistering services");
		ServiceDispatcher.getInstance().unregisterServiceClient(TARGET_SERVICE_NAME);
		try {
			DispatcherManagerFactory.getDispatcherManager().unregister(TARGET_SERVICE_NAME);
		} catch (DispatcherException e) {
			// Ignore
		}
		if (frankListener != null) {
			frankListener.stop();
		}
		log.debug("FrankSenderTest: Teardown done");
	}

	@Test
	void configureNoTargetConfigured() {
		// Arrange
		FrankSender sender = new FrankSender();
		Configuration mockConfiguration = mock();
		sender.setConfiguration(mockConfiguration);

		// Act / Assert
		assertThrows(ConfigurationException.class, sender::configure);
	}

	@Test
	void configureInvalidTargetConfigured() {
		// Arrange
		FrankSender sender = new FrankSender();
		Configuration mockConfiguration = mock();
		sender.setConfiguration(mockConfiguration);

		sender.setScope(FrankSender.Scope.ADAPTER);
		sender.setTarget("invalid");

		// Act / Assert
		assertThrows(ConfigurationException.class, sender::configure);
	}

	@Test
	void configureValidAdapterTargetConfigured() {
		// Arrange
		FrankSender sender = new FrankSender();
		sender.setScope(FrankSender.Scope.ADAPTER);
		sender.setTarget("adapterName");
		Configuration mockConfiguration = mock();
		sender.setConfiguration(mockConfiguration);
		Adapter adapter = mock();
		when(mockConfiguration.getRegisteredAdapter("adapterName")).thenReturn(adapter);

		// Act / Assert
		assertDoesNotThrow(sender::configure);
	}

	@Test
	void configureTargetViaParameter() {
		// Arrange
		FrankSender sender = new FrankSender();
		Configuration mockConfiguration = mock();
		sender.setConfiguration(mockConfiguration);

		sender.setScope(FrankSender.Scope.ADAPTER);
		sender.addParameter(ParameterBuilder.create("target", "adapterName"));

		// Act / Assert
		assertDoesNotThrow(sender::configure);
	}

	@Test
	void configureTargetIsScopeJvm() {
		// Arrange
		FrankSender sender = new FrankSender();
		Configuration mockConfiguration = mock();
		sender.setConfiguration(mockConfiguration);

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
	void findAdapterSuccess(String target) throws SenderException, ConfigurationException {
		// Arrange
		configuration = new TestConfiguration(false);
		FrankSender sender = configuration.createBean(FrankSender.class);
		sender.setTarget(target);
		IPipe pipe = new EchoPipe();
		pipe.setName("test-pipe");
		PipeLine pipeline = configuration.createBean(PipeLine.class);
		pipeline.addPipe(pipe);
		Adapter adapter = configuration.createBean(Adapter.class);
		adapter.setName("adapterName");
		configuration.addAdapter(adapter);

		IbisManager ibisManager = mock();
		sender.setIbisManager(ibisManager);
		when(ibisManager.getConfiguration("configurationName")).thenReturn(configuration);

		// Act
		sender.configure();
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
		sender.setTarget(target);
		sender.setScope(Scope.ADAPTER);

		Adapter adapter = mock();
		Configuration mockConfiguration = mock();
		IbisManager ibisManager = mock();

		sender.setIbisManager(ibisManager);
		sender.setConfiguration(mockConfiguration);

		when(ibisManager.getConfiguration("configurationName")).thenReturn(mockConfiguration);
		when(mockConfiguration.getRegisteredAdapter("adapterName")).thenReturn(adapter);

		// Act / Assert
		assertThrows(ConfigurationException.class, sender::configure);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"ListenerName",
			"TestConfiguration/ListenerName",
			"/ListenerName"
	})
	void getFrankListenerSuccess(String target) throws Exception {
		// Arrange
		configuration = new TestConfiguration(false);
		FrankSender sender = configuration.createBean(FrankSender.class);
		sender.setScope(Scope.LISTENER);
		sender.setTarget(target);

		frankListener = configuration.createBean(FrankListener.class);
		frankListener.setName("ListenerName");

		// Act
		sender.configure();
		frankListener.configure();
		frankListener.start();
		ServiceClient actual = sender.getFrankListener(target);

		// Assert
		assertNotNull(actual, "Expected to have found a FrankListener for target [" + target + "]");
		assertEquals(frankListener, actual);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"NoSuchListener",
			"NoSuchConfig/ListenerName",
			"/NoSuchListener"
	})
	void getFrankListenerNotFound(String target) throws Exception {
		// Arrange
		FrankSender sender = new FrankSender();
		sender.setScope(Scope.LISTENER);
		sender.setTarget(target);
		Configuration mockConfiguration = mock();
		when(mockConfiguration.getName()).thenReturn("ConfigName");
		sender.setConfiguration(mockConfiguration);
		sender.configure();

		frankListener = new FrankListener();
		frankListener.setName("ListenerName");
		frankListener.setConfiguration(mockConfiguration);
		frankListener.configure();
		frankListener.start();

		// Act
		assertThrows(SenderException.class, ()-> sender.getFrankListener(target));
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
		Configuration mockConfiguration = mock();
		sender.setConfiguration(mockConfiguration);

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
		Configuration mockConfiguration = mock();
		if (configuredTarget != null) {
			sender.setTarget(configuredTarget);
			Adapter adapter = mock();
			when(mockConfiguration.getRegisteredAdapter(anyString())).thenReturn(adapter);
		}
		sender.setConfiguration(mockConfiguration);
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
	@CsvSource({ // Cannot test with DLL Scope
			"ADAPTER, cid1",
			"LISTENER, cid1",
			"JVM, cid1",
			"ADAPTER,",
			"LISTENER,",
			"JVM,",
	})
	void sendSyncMessage(FrankSender.Scope scope, String correlationId) throws Exception {
		// Arrange
		log.debug("Creating Configuration");
		configuration = new TestConfiguration(false);
		GetFromSession pipe = new GetFromSession();
		pipe.setSessionKey("session-key");
		pipe.setName("test-pipe");

		FrankSender sender = createFrankSender(scope, true, pipe);

		session = new PipeLineSession();
		session.put("session-key", "reply");
		session.put(PipeLineSession.CORRELATION_ID_KEY, correlationId);

		input = new Message("request");

		// Act
		log.debug("starting actual test");
		result = sender.sendMessage(input, session);

		// Assert
		assertNotNull(result);
		assertTrue(result.isSuccess(), "Expected sender to succeed calling target adapter");
		assertNotNull(result.getResult());

		Message resultMessage = result.getResult();
		assertNotNull(resultMessage.asString());
		String resultString = resultMessage.asString();
		assertEquals("reply", resultString);
		if (correlationId != null) {
			assertThat(session, hasKey(PipeLineSession.CORRELATION_ID_KEY));
			assertEquals(correlationId, session.getCorrelationId());
		}
	}

	@ParameterizedTest
	@EnumSource(names = {"ADAPTER", "LISTENER", "JVM"}) // Cannot test with DLL Scope
	void sendAsyncMessage(FrankSender.Scope scope) throws Exception {
		// Arrange
		log.debug("Creating Configuration");
		configuration = new TestConfiguration(false);
		Semaphore semaphore = new Semaphore(0);
		IPipe pipe = new AbstractPipe() {
			@Override
			public PipeRunResult doPipe(Message message, PipeLineSession session) {
				// Assert this to make sure input message is not prematurely closed; normally this is asserted by a PipeLineProcessor but that is not created in the Test SpringContext
				message.assertNotClosed();
				semaphore.release();
				return new PipeRunResult();
			}
		};
		pipe.setName("test-pipe");

		FrankSender sender = createFrankSender(scope, false, pipe);

		session = new PipeLineSession();
		session.put(PipeLineSession.MESSAGE_ID_KEY, "mid1");
		session.put(PipeLineSession.CORRELATION_ID_KEY, "cid2");
		input = new Message("request");

		// Act
		log.debug("starting actual test");
		result = sender.sendMessage(input, session);

		// Assert
		assertNotNull(result);
		assertTrue(result.isSuccess(), "Expected sender to succeed calling target adapter");
		assertNotNull(result.getResult());

		Message resultMessage = result.getResult();
		assertTrue(Message.isNull(resultMessage), "Expected result from async call to be a NULL Message");
		boolean acquired = semaphore.tryAcquire(1, TimeUnit.SECONDS);
		assertTrue(acquired, "Failed to acquire semaphore, appears as if async adapter was not executed");
	}

	@ParameterizedTest
	@CsvSource({ // Cannot test with DLL Scope
			"ADAPTER, cid1",
			"LISTENER, cid1",
			"JVM, cid1",
			"ADAPTER,",
			"LISTENER,",
			"JVM,",
	})
	void sendMessageHandleException(FrankSender.Scope scope, String correlationId) throws Exception {
		// Arrange
		log.debug("Creating Configuration");
		configuration = new TestConfiguration(false);
		ExceptionPipe pipe = new ExceptionPipe();
		pipe.setName("test-pipe");

		FrankSender sender = createFrankSender(scope, true, pipe);

		session = new PipeLineSession();
		session.put(PipeLineSession.CORRELATION_ID_KEY, correlationId);

		input = new Message("request");

		// Act
		log.debug("starting actual test");
		SenderException exception = assertThrows(SenderException.class, () -> sender.sendMessage(input, session));

		// Assert
		assertThat(exception.getMessage(), containsStringIgnoringCase("exception"));
		if (correlationId != null) {
			assertThat(session, hasKey(PipeLineSession.CORRELATION_ID_KEY));
			assertEquals(correlationId, session.getCorrelationId());
		}
	}

	private @Nonnull FrankSender createFrankSender(FrankSender.Scope scope, boolean callSync, IPipe pipe) throws ConfigurationException, ListenerException {
		FrankSender sender = configuration.createBean(FrankSender.class);
		sender.setTarget(TARGET_SERVICE_NAME);
		sender.setScope(scope);
		sender.setSynchronous(callSync);
		if (callSync) {
			sender.setReturnedSessionKeys("session-key,cid");
			sender.addParameter(ParameterBuilder
					.create("session-key", null)
					.withSessionKey("session-key"));
		}

		Adapter targetAdapter = createAdapter(configuration, pipe);
		if (scope == FrankSender.Scope.JVM) {
			createJavaListener(configuration, targetAdapter);
		} else if (scope == FrankSender.Scope.LISTENER) {
			createFrankListener(configuration, targetAdapter);
		}
		if (!callSync) {
			IsolatedServiceCaller isc = configuration.createBean(IsolatedServiceCaller.class);
			sender.setIsolatedServiceCaller(isc);
		}

		configuration.configure();
		configuration.start();
		waitForState(targetAdapter, RunState.STARTED);

		sender.configure();
		return sender;
	}

	private void createFrankListener(TestConfiguration configuration, Adapter targetAdapter) throws ListenerException {
		@SuppressWarnings("unchecked")
		Receiver<Message> receiver = configuration.createBean(Receiver.class);
		receiver.setName("TargetAdapter-receiver");

		FrankListener listener = configuration.createBean(FrankListener.class);
		listener.setHandler(receiver);

		receiver.setListener(listener);
		receiver.setTxManager(configuration.createBean(NarayanaJtaTransactionManager.class));

		targetAdapter.addReceiver(receiver);
		receiver.setAdapter(targetAdapter);

		listener.configure();
		listener.start();
	}

	private void createJavaListener(TestConfiguration configuration, Adapter targetAdapter) throws ListenerException {
		@SuppressWarnings("unchecked")
		Receiver<String> receiver = configuration.createBean(Receiver.class);
		receiver.setName("TargetAdapter-receiver");

		@SuppressWarnings("unchecked")
		JavaListener<String> listener = configuration.createBean(JavaListener.class);
		listener.setName(receiver.getName());
		listener.setServiceName(TARGET_SERVICE_NAME);
		listener.setHandler(receiver);

		receiver.setListener(listener);
		receiver.setTxManager(configuration.createBean(NarayanaJtaTransactionManager.class));

		targetAdapter.addReceiver(receiver);
		receiver.setAdapter(targetAdapter);

		listener.start();
	}

	private Adapter createAdapter(TestConfiguration configuration, IPipe pipe) throws ConfigurationException {
		Adapter adapter = configuration.createBean(Adapter.class);
		adapter.setName(TARGET_SERVICE_NAME);

		CorePipeLineProcessor plp = configuration.createBean(CorePipeLineProcessor.class);
		PipeProcessor pp = configuration.createBean(CorePipeProcessor.class);
		plp.setPipeProcessor(pp);
		PipeLine pl = configuration.createBean(PipeLine.class);
		pl.setPipeLineProcessor(plp);
		pl.addPipe(pipe);
		pl.setFirstPipe(pipe.getName());

		PipeLine.ExitState exitState = PipeLine.ExitState.SUCCESS;
		PipeLineExit ple = new PipeLineExit();
		ple.setName(exitState.name());
		ple.setState(exitState);
		pl.addPipeLineExit(ple);
		adapter.setPipeLine(pl);

		configuration.addAdapter(adapter);
		return adapter;
	}

}
