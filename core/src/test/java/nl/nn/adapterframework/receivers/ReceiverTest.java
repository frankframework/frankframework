package nl.nn.adapterframework.receivers;

import static org.junit.Assert.assertEquals;
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

	public Receiver<String> setupReceiver(int startupDelay) throws Exception {
		SlowStartingPullingListener listener = configuration.createBean(SlowStartingPullingListener.class);
		listener.setStartupDelay(startupDelay);
		@SuppressWarnings("unchecked")
		Receiver<String> receiver = configuration.createBean(Receiver.class);
		receiver.setListener(listener);
		receiver.setName("receiver");
		receiver.setStartTimeout(2);
		receiver.setStopTimeout(2);
		return receiver;
	}

	public Adapter setupAdapter(Receiver<String> receiver) throws Exception {

		Adapter adapter = configuration.createBean(Adapter.class);
		adapter.setName("adapterName");

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
	public void waitForState(IManagable object, RunState state) throws InterruptedException {
		while(object.getRunState()!=state) {
			Thread.sleep(100);
		}
	}

	@Test
	public void testReceiverStartBasic() throws Exception {

		Receiver<String> receiver = setupReceiver(0);
		Adapter adapter = setupAdapter(receiver);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STARTING);

		log.info("Adapter RunState "+adapter.getRunState());
		assertEquals(RunState.STARTED, adapter.getRunState());

		waitWhileInState(receiver, RunState.STOPPED); // ?
		waitWhileInState(receiver, RunState.STARTING);
		log.info("Receiver RunState "+receiver.getRunState());
		assertEquals(RunState.STARTED, receiver.getRunState());

		configuration.stop();
	}

	@Test
	public void testReceiverStartTimeout() throws Exception {

		Receiver<String> receiver = setupReceiver(10000);
		Adapter adapter = setupAdapter(receiver);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STARTING);

		log.info("Adapter RunState "+adapter.getRunState());
		assertEquals(RunState.STARTED, adapter.getRunState());

		waitWhileInState(receiver, RunState.STOPPED);
		waitWhileInState(receiver, RunState.STARTING);

		log.info("Receiver RunState "+receiver.getRunState());
		assertEquals(RunState.EXCEPTION_STARTING, receiver.getRunState());
	}

	@Test
	public void startReceiver() throws Exception {
		Receiver<String> receiver = setupReceiver(10000);
		Adapter adapter = setupAdapter(receiver);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STARTING);
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
		waitWhileInState(receiver, RunState.STARTING);
		assertEquals(RunState.EXCEPTION_STARTING, receiver.getRunState());

		// try to stop the started adapter
		configuration.getIbisManager().handleAction(IbisAction.STOPADAPTER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);
		waitWhileInState(adapter, RunState.STOPPING);

		assertEquals(RunState.STOPPING, receiver.getRunState());
		waitWhileInState(receiver, RunState.STOPPING);

		assertEquals(RunState.EXCEPTION_STOPPING, receiver.getRunState());
		assertEquals(RunState.STARTED, adapter.getRunState());
	}
}