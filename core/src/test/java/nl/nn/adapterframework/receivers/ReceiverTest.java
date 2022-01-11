package nl.nn.adapterframework.receivers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import nl.nn.adapterframework.configuration.AdapterManager;
import nl.nn.adapterframework.configuration.IbisManager.IbisAction;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IManagable;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.pipes.EchoPipe;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.RunState;

public class ReceiverTest {
	protected Logger log = LogUtil.getLogger(this);
	private TestConfiguration configuration = new TestConfiguration();

	@Before
	public void setUp() throws Exception {
		configuration.stop();
		configuration.getBean("adapterManager", AdapterManager.class).close();
	}

	public SlowStartingListenerBase setupPullingListener(int startupDelay) throws Exception {
		SlowStartingPullingListener listener = configuration.createBean(SlowStartingPullingListener.class);
		listener.setStartupDelay(startupDelay);
		return listener;
	}

	public SlowStartingListenerBase setupPushingListener(int startupDelay) throws Exception {
		SlowStartingPushingListener listener = configuration.createBean(SlowStartingPushingListener.class);
		listener.setStartupDelay(startupDelay);
		return listener;
	}

	public Receiver<String> setupReceiver(SlowStartingListenerBase listener) throws Exception {
		@SuppressWarnings("unchecked")
		Receiver<String> receiver = configuration.createBean(Receiver.class);
		configuration.autowireByName(listener);
		receiver.setListener(listener);
		receiver.setName("receiver");
		receiver.setStartTimeout(2);
		receiver.setStopTimeout(2);
		DummySender sender = configuration.createBean(DummySender.class);
		receiver.setSender(sender);
		return receiver;
	}

	public Adapter setupAdapter(Receiver<String> receiver) throws Exception {

		Adapter adapter = configuration.createBean(Adapter.class);
		adapter.setName("ReceiverTestAdapterName");

		PipeLine pl = new PipeLine();
		pl.setFirstPipe("dummy");

		EchoPipe pipe = new EchoPipe();
		pipe.setName("dummy");
		pl.addPipe(pipe);

		PipeLineExit ple = new PipeLineExit();
		ple.setPath("success");
		ple.setState("success");
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
		testStartNoTimeout(setupPullingListener(0));
	}

	@Test
	public void testPushingReceiverStartBasic() throws Exception {
		testStartNoTimeout(setupPushingListener(0));
	}

	public void testStartNoTimeout(SlowStartingListenerBase listener) throws Exception {
		Receiver<String> receiver = setupReceiver(listener);
		Adapter adapter = setupAdapter(receiver);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

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
		testStartTimeout(setupPullingListener(10000));
	}

	@Test
	public void testPushingReceiverStartWithTimeout() throws Exception {
		testStartTimeout(setupPushingListener(10000));
	}

	public void testStartTimeout(SlowStartingListenerBase listener) throws Exception {
		Receiver<String> receiver = setupReceiver(listener);
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
		assertTrue("Close has not been called on the Receiver's sender!", receiver.getSender().isSynchronous());

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
	public void startReceiver() throws Exception {
		Receiver<String> receiver = setupReceiver(setupPullingListener(10000));
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