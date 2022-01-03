package nl.nn.adapterframework.receivers;

import static org.mockito.Mockito.spy;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.configuration.IbisManager.IbisAction;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.lifecycle.ConfigurableLifecycle.BootState;
import nl.nn.adapterframework.pipes.EchoPipe;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.unmanaged.DefaultIbisManager;
import nl.nn.adapterframework.util.RunStateEnum;

public class ReceiverTest {

	private IbisContext ibisContext = spy(new IbisContext());
	private TestConfiguration configuration;
	private Receiver receiver;
	private TaskExecutor taskExecuter;
	private IbisManager ibisManager;
	
	@Before
	public void setUp() throws Exception {
		configuration = new TestConfiguration();
		receiver = configuration.createBean(Receiver.class);
		taskExecuter = new SimpleAsyncTaskExecutor();
		ibisManager = spy(new DefaultIbisManager());
		ibisManager.setIbisContext(ibisContext);
		configuration.setIbisManager(ibisManager);
	}
	
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
	public void startReceiver() throws Exception {
		/** listener */
		MockPullingListener listener = configuration.createBean(MockPullingListener.class);
		receiver.setListener(listener);

		/** adapter and pipeline */
		String adapterName="adapter";
		Adapter adapter = Mockito.spy(new Adapter());
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
		receiver.setStartTimeout(10);
		receiver.setStopTimeout(10);
		adapter.registerReceiver(receiver);

		adapter.configure();

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
		
		System.err.println(configuration.getState());
		
		// start adapter
		adapter.startRunning();
		
		while(adapter.getRunState()!=RunStateEnum.STARTED) {
			Thread.sleep(200);
		}
		System.err.println("adapter started "+adapter.getRunState());
		
		// wait for receiver to start
		while(receiver.getRunState()!=RunStateEnum.STARTED) {
			Thread.sleep(200);
		}
		System.err.println("receiver started "+receiver.getRunState());
		
		// stop receiver then start
		Runnable runnable = new Runnable() {
			
			@Override
			public void run() {
				receiver.stopRunning();
				while(receiver.getRunState()!=RunStateEnum.STOPPED) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
				receiver.startRunning();
			}
		};
		taskExecuter.execute(runnable);

		// when receiver is in starting state
		while(receiver.getRunState()!=RunStateEnum.STARTING) {
			Thread.sleep(100);
		}

		System.err.println("receiver is in state ["+receiver.getRunState()+"]");
		
		// try to stop the started adapter
		configuration.getIbisManager().handleAction(IbisAction.STOPADAPTER, configuration.getName(), adapterName, "receiver", null, true);

		System.err.println(adapter.getRunState());

		//see what happens 
		while(adapter.getRunState()!=RunStateEnum.STOPPED) {
			Thread.sleep(999);
			System.err.println("Trying to stop adapter in state ["+adapter.getRunState()+"]");
			System.err.println("receiver in state ["+receiver.getRunState()+"]");
		}

		configuration.close();
		configuration = null;
	}
	
}