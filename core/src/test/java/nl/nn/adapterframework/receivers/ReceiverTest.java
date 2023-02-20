package nl.nn.adapterframework.receivers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import nl.nn.adapterframework.configuration.AdapterManager;
import nl.nn.adapterframework.configuration.IbisManager.IbisAction;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IManagable;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLine.ExitState;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.pipes.EchoPipe;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageKeeperMessage;
import nl.nn.adapterframework.util.RunState;

public class ReceiverTest {
	protected Logger log = LogUtil.getLogger(this);
	private TestConfiguration configuration = new TestConfiguration();

	@Before
	public void setUp() throws Exception {
		configuration.stop();
		configuration.getBean("adapterManager", AdapterManager.class).close();
	}

	public SlowListenerBase setupSlowStartPullingListener(int startupDelay) {
		return createSlowListener(SlowPullingListener.class, startupDelay, 0);
	}

	public SlowListenerBase setupSlowStartPushingListener(int startupDelay) {
		return createSlowListener(SlowPushingListener.class, startupDelay, 0);
	}

	public SlowListenerBase setupSlowStopPullingListener(int shutdownDelay) {
		return createSlowListener(SlowPullingListener.class, 0, shutdownDelay);
	}

	public SlowListenerBase setupSlowStopPushingListener(int shutdownDelay) {
		return createSlowListener(SlowPushingListener.class, 0, shutdownDelay);
	}

	protected <T extends SlowListenerBase> T createSlowListener(Class<T> cls, int startupDelay, int shutdownDelay) {
		T listener = configuration.createBean(cls);
		listener.setStartupDelay(startupDelay);
		listener.setShutdownDelay(shutdownDelay);
		return listener;
	}

	public Receiver<javax.jms.Message> setupReceiver(SlowListenerBase listener) throws Exception {
		@SuppressWarnings("unchecked")
		Receiver<javax.jms.Message> receiver = configuration.createBean(Receiver.class);
		configuration.autowireByName(listener);
		receiver.setListener(listener);
		receiver.setName("receiver");
		receiver.setStartTimeout(2);
		receiver.setStopTimeout(2);
		DummySender sender = configuration.createBean(DummySender.class);
		receiver.setSender(sender);
		return receiver;
	}

	public Adapter setupAdapter(Receiver<javax.jms.Message> receiver) throws Exception {

		Adapter adapter = configuration.createBean(Adapter.class);
		adapter.setName("ReceiverTestAdapterName");

		PipeLine pl = new PipeLine();
		pl.setFirstPipe("dummy");

		EchoPipe pipe = new EchoPipe();
		pipe.setName("dummy");
		pl.addPipe(pipe);

		PipeLineExit ple = new PipeLineExit();
		ple.setPath("success");
		ple.setState(ExitState.SUCCESS);
		pl.registerPipeLineExit(ple);
		adapter.setPipeLine(pl);

		adapter.registerReceiver(receiver);
		configuration.registerAdapter(adapter);
		return adapter;
	}

	public void waitWhileInState(IManagable object, RunState state) {
		while(object.getRunState()==state) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				fail("test interrupted");
			}
		}
	}
	public void waitForState(IManagable object, RunState state) {
		while(object.getRunState()!=state) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				fail("test interrupted");
			}
		}
	}

	@Test
	public void testPullingReceiverStartBasic() throws Exception {
		testStartNoTimeout(setupSlowStartPullingListener(0));
	}

	@Test
	public void testPushingReceiverStartBasic() throws Exception {
		testStartNoTimeout(setupSlowStartPushingListener(0));
	}

	public void testStartNoTimeout(SlowListenerBase listener) throws Exception {
		Receiver<javax.jms.Message> receiver = setupReceiver(listener);
		Adapter adapter = setupAdapter(receiver);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);

		log.info("Adapter RunState "+adapter.getRunState());
		assertEquals(RunState.STARTED, adapter.getRunState());

		waitWhileInState(receiver, RunState.STOPPED); //Ensure the next waitWhileInState doesn't skip when STATE is still STOPPED
		waitWhileInState(receiver, RunState.STARTING); //Don't continue until the receiver has been started.
		log.info("Receiver RunState "+receiver.getRunState());

		assertFalse(listener.isClosed()); // Not closed, thus open
		assertFalse(receiver.getSender().isSynchronous()); // Not closed, thus open
		assertEquals(RunState.STARTED, receiver.getRunState());
	}

	@Test
	public void testPullingReceiverStartWithTimeout() throws Exception {
		testStartTimeout(setupSlowStartPullingListener(10000));
	}

	@Test
	public void testPushingReceiverStartWithTimeout() throws Exception {
		testStartTimeout(setupSlowStartPushingListener(10000));
	}

	public void testStartTimeout(SlowListenerBase listener) throws Exception {
		Receiver<javax.jms.Message> receiver = setupReceiver(listener);
		Adapter adapter = setupAdapter(receiver);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);

		log.info("Adapter RunState "+adapter.getRunState());
		assertEquals(RunState.STARTED, adapter.getRunState());

		waitWhileInState(receiver, RunState.STOPPED); //Ensure the next waitWhileInState doesn't skip when STATE is still STOPPED
		waitWhileInState(receiver, RunState.STARTING); //Don't continue until the receiver has been started.

		log.info("Receiver RunState "+receiver.getRunState());
		assertEquals("Receiver should be in state [EXCEPTION_STARTING]", RunState.EXCEPTION_STARTING, receiver.getRunState());
		Thread.sleep(500); //Extra timeout to give the receiver some time to close all resources
		assertTrue("Close has not been called on the Receiver's sender!", receiver.getSender().isSynchronous()); //isSynchronous ==> isClosed

		configuration.getIbisManager().handleAction(IbisAction.STOPRECEIVER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);
		while(receiver.getRunState()!=RunState.STOPPED) {
			System.out.println(receiver.getRunState());
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				fail("test interrupted");
			}
		}
		assertEquals(RunState.STOPPED, receiver.getRunState());
		assertEquals(RunState.STARTED, adapter.getRunState());
		assertTrue(listener.isClosed());
	}

	@Test
	public void testPullingReceiverStopWithTimeout() throws Exception {
		testStopTimeout(setupSlowStopPullingListener(100_000));
	}

	@Test
	public void testPushingReceiverStopWithTimeout() throws Exception {
		testStopTimeout(setupSlowStopPushingListener(100_000));
	}

	public void testStopTimeout(SlowListenerBase listener) throws Exception {
		Receiver<javax.jms.Message> receiver = setupReceiver(listener);
		Adapter adapter = setupAdapter(receiver);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);

		log.info("Adapter RunState "+adapter.getRunState());
		log.info("Receiver RunState "+receiver.getRunState());
		waitForState(receiver, RunState.STARTED); //Don't continue until the receiver has been started.

		configuration.getIbisManager().handleAction(IbisAction.STOPRECEIVER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);

		waitWhileInState(receiver, RunState.STARTED);
		waitWhileInState(receiver, RunState.STOPPING);
		log.info("Receiver RunState "+receiver.getRunState());

		assertEquals(RunState.EXCEPTION_STOPPING, receiver.getRunState());
	}

	@Test
	public void testPollGuardStartTimeout() throws Exception {
		// Create listener without any delays in starting or stopping, they will be set later
		SlowListenerWithPollGuard listener = createSlowListener(SlowListenerWithPollGuard.class, 0, 0);
		listener.setPollGuardInterval(1_000);
		listener.setMockLastPollDelayMs(10_000); // Last Poll always before PollGuard triggered

		Receiver<javax.jms.Message> receiver = setupReceiver(listener);
		Adapter adapter = setupAdapter(receiver);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);

		log.info("Adapter RunState "+adapter.getRunState());
		log.info("Receiver RunState "+receiver.getRunState());
		assertEquals(RunState.STARTED, adapter.getRunState());

		waitForState(receiver, RunState.STARTED); //Don't continue until the receiver has been started.

		// From here the PollGuard should be triggering startup-delay timeout-guard
		listener.setStartupDelay(100_000);

		log.warn("Test sleeping to let poll guard timer run and do its work for a while");
		Thread.sleep(5_000);
		log.warn("Test resuming");

		assertEquals(RunState.EXCEPTION_STARTING, receiver.getRunState());

		List<String> errors = (List<String>) adapter.getMessageKeeper()
				.stream()
				.filter((msg) -> msg instanceof MessageKeeperMessage && "ERROR".equals(((MessageKeeperMessage)msg).getMessageLevel()))
				.map(Object::toString)
				.collect(Collectors.toList());

		assertThat(errors, hasItem(containsString("Failed to restart receiver")));

		configuration.getIbisManager().handleAction(IbisAction.STOPRECEIVER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);

		waitWhileInState(receiver, RunState.STARTED);
		waitWhileInState(receiver, RunState.STOPPING);
		log.info("Receiver RunState "+receiver.getRunState());

		assertEquals(RunState.STOPPED, receiver.getRunState());
	}

	@Test
	public void testPollGuardStopTimeout() throws Exception {
		// Create listener without any delays in starting or stopping, they will be set later
		SlowListenerWithPollGuard listener = createSlowListener(SlowListenerWithPollGuard.class, 0, 0);
		listener.setPollGuardInterval(1_000);
		listener.setMockLastPollDelayMs(10_000); // Last Poll always before PollGuard triggered

		Receiver<javax.jms.Message> receiver = setupReceiver(listener);
		Adapter adapter = setupAdapter(receiver);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);

		log.info("Adapter RunState "+adapter.getRunState());
		log.info("Receiver RunState "+receiver.getRunState());
		assertEquals(RunState.STARTED, adapter.getRunState());

		waitForState(receiver, RunState.STARTED); //Don't continue until the receiver has been started.

		// From here the PollGuard should be triggering stop-delay timeout-guard
		listener.setShutdownDelay(100_000);

		log.warn("Test sleeping to let poll guard timer run and do its work for a while");
		Thread.sleep(5_000);
		log.warn("Test resuming");

		// Receiver may be in state "stopping" (by PollGuard) or in state "starting" while we come out of sleep, so wait until it's started
		waitForState(receiver, RunState.STARTED);

		configuration.getIbisManager().handleAction(IbisAction.STOPRECEIVER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);

		waitWhileInState(receiver, RunState.STARTED);
		waitWhileInState(receiver, RunState.STOPPING);
		log.info("Receiver RunState "+receiver.getRunState());

		assertEquals(RunState.EXCEPTION_STOPPING, receiver.getRunState());

		List<String> warnings = (List<String>) adapter.getMessageKeeper()
				.stream()
				.filter((msg) -> msg instanceof MessageKeeperMessage && "WARN".equals(((MessageKeeperMessage)msg).getMessageLevel()))
				.map(Object::toString)
				.collect(Collectors.toList());
		assertThat(warnings, everyItem(containsString("JMS poll timeout")));
	}

	@Test
	public void startReceiver() throws Exception {
		Receiver<javax.jms.Message> receiver = setupReceiver(setupSlowStartPullingListener(10000));
		Adapter adapter = setupAdapter(receiver);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);
		waitWhileInState(receiver, RunState.STOPPED);
		waitWhileInState(receiver, RunState.STARTING);

		log.info("Adapter RunState "+adapter.getRunState());

		// stop receiver then start
		SimpleAsyncTaskExecutor taskExecuter = new SimpleAsyncTaskExecutor();
		taskExecuter.execute(()-> {
			configuration.getIbisManager().handleAction(IbisAction.STOPRECEIVER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);
			waitWhileInState(receiver, RunState.STOPPING);

			configuration.getIbisManager().handleAction(IbisAction.STOPRECEIVER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);
			waitWhileInState(receiver, RunState.STARTING);
		});

		// when receiver is in starting state
		waitWhileInState(receiver, RunState.STOPPED);
		waitWhileInState(receiver, RunState.STARTING);
		assertEquals(RunState.EXCEPTION_STARTING, receiver.getRunState());

		// try to stop the started adapter
		configuration.getIbisManager().handleAction(IbisAction.STOPADAPTER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);
		waitForState(adapter, RunState.STOPPED);

		assertEquals(RunState.STOPPED, receiver.getRunState());
		assertEquals(RunState.STOPPED, adapter.getRunState());
	}
}
