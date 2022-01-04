package nl.nn.adapterframework.receivers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;

import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IManagable;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.lifecycle.ConfigurableLifecycle.BootState;
import nl.nn.adapterframework.pipes.EchoPipe;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.RunState;

public class ReceiverTest {
	protected Logger log = LogUtil.getLogger(this);

	private IbisContext ibisContext = spy(new IbisContext());
	private TaskExecutor taskExecuter;
	private IbisManager ibisManager;

	private TestConfiguration configuration;
	private Adapter adapter;
	private Receiver receiver;
	
	@Before
	public void setUp() throws Exception {
		configuration = new TestConfiguration();
		receiver = configuration.createBean(Receiver.class);

		taskExecuter = new SimpleAsyncTaskExecutor();
		ibisManager = configuration.getIbisManager();
	}
	
	public void setupReceiver(int startupDelay) throws ConfigurationException {
		/** listener */
		SlowStartingPullingListener listener = configuration.createBean(SlowStartingPullingListener.class);
		listener.setStartupDelay(startupDelay);
		receiver.setListener(listener);

		/** adapter and pipeline */
		String adapterName="adapter";
		adapter = Mockito.spy(new Adapter());
		adapter.setName(adapterName);
		
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

		configuration.autowireByName(adapter);
		configuration.autowireByName(receiver);

		receiver.setName("receiver");
		receiver.setAdapter(adapter);
		receiver.setStartTimeout(2);
		receiver.setStopTimeout(2);
		adapter.registerReceiver(receiver);
	}
	
	public void registerAdapter() throws InterruptedException {
		// close configuration to add the adapter
		configuration.close();
		while(!configuration.inState(BootState.STOPPED)) {
			Thread.sleep(100);
		}
		configuration.registerAdapter(adapter);
		configuration.refresh();
		configuration.getAdapterManager().configure();
		configuration.getScheduleManager().configure();
		configuration.start();
		ibisManager.addConfiguration(configuration);
	}
	
	public void waitWhileInState(IManagable object, RunState state) throws InterruptedException {
		while(object.getRunState()==state) {
			Thread.sleep(100);
		}
	}
	public void waitForState(IManagable object, RunState state) throws InterruptedException {
		while(object.getRunState()!=state) {
			Thread.sleep(100);
		}
	}
	
	@Test
	public void testReceiverStartBasic() throws Exception {

		setupReceiver(0);
		adapter.configure();
		registerAdapter();
		// start adapter
		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());
		adapter.startRunning();
		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);
		log.info("Adapter RunState "+adapter.getRunState());
		assertEquals(RunState.STARTED, adapter.getRunState());
		waitWhileInState(receiver, RunState.STOPPED);
		waitWhileInState(receiver, RunState.STARTING);
		log.info("Receiver RunState "+receiver.getRunState());
		assertEquals(RunState.STARTED, receiver.getRunState());
	}	

	@Test
	public void testReceiverStartTimeout() throws Exception {

		setupReceiver(10000);
		adapter.configure();
		registerAdapter();
		// start adapter
		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());
		adapter.startRunning();
		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);
		log.info("Adapter RunState "+adapter.getRunState());
		assertEquals(RunState.STARTED, adapter.getRunState());
		waitWhileInState(receiver, RunState.STOPPED);
		waitWhileInState(receiver, RunState.STARTING);
		log.info("Receiver RunState "+receiver.getRunState());
		assertEquals(RunState.EXCEPTION_STARTING, receiver.getRunState());
	}	


		// close configuration to add the adapter

	
	/**
	 * Sets up an adapter with a receiver and a pipeline.
	 * When everything is up and running stops and starts the receiver
	 * While receiver is in STARTING state tries to stop the adapter 
	 * 
	 * Adapter stucks in STOPPING state and the receiver gets STARTED state
	 * 
	 * 
	 */
//	@Test
//	public void startReceiver() throws Exception {
//
//		setupReceiver(10000);
//		adapter.configure();
//
//		// close configuration to add the adapter
//		configuration.close();
//		while(!configuration.inState(BootState.STOPPED)) {
//			Thread.sleep(100);
//		}
//		configuration.registerAdapter(adapter);
//		configuration.refresh();
//		configuration.getAdapterManager().configure();
//		configuration.getScheduleManager().configure();
//		configuration.start();
//		ibisManager.addConfiguration(configuration);
//		
//		System.err.println("configuration State "+configuration.getState());
//		
//		// start adapter
//		adapter.startRunning();
//		
//		while(adapter.getRunState()!=RunState.STARTED) {
//			Thread.sleep(200);
//		}
//		System.err.println("Adapter RunState "+adapter.getRunState());
//		
//		// wait for receiver to start
//		while(receiver.getRunState()==RunState.STARTING) {
//			System.err.println("wait for receiver to start, RunState "+receiver.getRunState());
//			Thread.sleep(200);
//		}
//		System.err.println("receiver RunState "+receiver.getRunState());
//		
//		// stop receiver then start
//		taskExecuter.execute(()->{
//				receiver.stopRunning();
//				System.err.println("wait for receiver to stop");
//				while(receiver.getRunState()!=RunState.STOPPED) {
//					try {
//						Thread.sleep(100);
//					} catch (InterruptedException e) {
//						Thread.currentThread().interrupt();
//					}
//				}
//				System.err.println("start receiver");
//				receiver.startRunning();
//			});
//
//		// when receiver is in starting state
//		while(receiver.getRunState()!=RunState.STARTING) {
//			System.err.println("wait for receiver to start, RunState "+receiver.getRunState());
//			Thread.sleep(100);
//		}
//
//		System.err.println("receiver is in state ["+receiver.getRunState()+"]");
//		
//		// try to stop the started adapter
//		configuration.getIbisManager().handleAction(IbisAction.STOPADAPTER, configuration.getName(), adapterName, "receiver", null, true);
//
//		System.err.println(adapter.getRunState());
//
//		//see what happens 
//		while(adapter.getRunState()!=RunState.STOPPED) {
//			Thread.sleep(999);
//			System.err.println("Trying to stop adapter in state ["+adapter.getRunState()+"]");
//			System.err.println("receiver in state ["+receiver.getRunState()+"]");
//		}
//
//		configuration.close();
//		configuration = null;
//	}
//	
}