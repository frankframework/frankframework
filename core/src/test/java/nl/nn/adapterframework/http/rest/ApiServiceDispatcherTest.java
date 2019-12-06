package nl.nn.adapterframework.http.rest;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import nl.nn.adapterframework.core.ListenerException;

import org.junit.Before;
import org.junit.Test;

public class ApiServiceDispatcherTest {

	private ApiServiceDispatcher dispatcher = null;
	private int amount = 100;

	@Before
	public void setUp() {
		dispatcher = new ApiServiceDispatcher();
	}

	@Test
	public void testAddManyConcurrentSimultaneousListeners() throws ListenerException, InterruptedException {
		List<Thread> list = new ArrayList<Thread>();

		// Spin up many simultaneous threads to 'bomb' the dispatcher with many concurrent requests
		for (int i = 0; i < amount; i++) {
			String name = "thread"+i;
			CreateListener target = new CreateListener(name);
			Thread t = new Thread(target);
			t.setName(name);
			t.start();
			list.add(t);
		}

		// Join all threads together
		for (Thread thread : list) {
			thread.join();
		}

		// Make sure the expected amount matches the clients registered in the dispatcher!
		assertEquals(amount, dispatcher.getPatternClients().size());
	}

	private class CreateListener implements Runnable {
		private String name = null;

		public CreateListener(String name) {
			this.name = name;
		}

		@Override
		public void run() {
			Double timeout = Math.ceil(Math.random()*3);
			try {
				Thread.sleep(timeout.longValue());
			} catch (InterruptedException e1) {
				//Failed to sleep? unsure why, but doesn't matter much
			}
			ApiListener listener = new ApiListener();
			listener.setName(name);
			listener.setMethod("GET");
			listener.setUriPattern(name);
			
			try {
				dispatcher.registerServiceClient(listener);
			} catch (ListenerException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
