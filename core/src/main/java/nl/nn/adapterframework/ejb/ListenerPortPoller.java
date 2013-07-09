/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.ejb;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IListenerConnector;
import nl.nn.adapterframework.core.IPortConnectedListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.receivers.GenericReceiver;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.RunStateEnum;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The ListenerPortPoller checks for all registered EjbListenerPortConnector
 * instances if the associated listener-port is still open while the
 * listener is also open, and opens / closes the listeners / receivers
 * accordingly.
 *
 * Instances to be polled are kept are weak references so that registration
 * here does not prevent objects from being garbage-collected. When a weak
 * reference goes <code>null</code> it is automatically unregistered.
 *
 * @author Tim van der Leeuw
 */
public class ListenerPortPoller implements DisposableBean {
	private Logger log = LogUtil.getLogger(this);

	private List portConnectorList = new ArrayList();

	public ListenerPortPoller() {
		log.debug("Created new ListenerPortPoller: " + this.toString());
	}


	/**
	 * Add an EjbListenerPortConnector instance to be polled.
	 *
	 * Only add instances if they are not already registered.
	 */
	public void registerEjbListenerPortConnector(IListenerConnector ejc) {
		if (!isRegistered(ejc)) {
			portConnectorList.add(new WeakReference(ejc));
		}
	}

	/**
	 * Remove an EjbListenerPortConnector instance from the list to be polled.
	 */
	public void unregisterEjbListenerPortConnector(IListenerConnector ejc) {
		for (Iterator iter = portConnectorList.iterator(); iter.hasNext();) {
			WeakReference wr = (WeakReference)iter.next();
			Object referent = wr.get();
			if (referent == null || referent == ejc) {
				iter.remove();
			}
		}
	}

	public boolean isRegistered(IListenerConnector ejc) {
		for (Iterator iter = portConnectorList.iterator(); iter.hasNext();) {
			WeakReference wr = (WeakReference)iter.next();
			Object referent = wr.get();
			if (referent == null) {
				iter.remove();
			} else if (referent == ejc) {
				return true;
			}
		}
		return false;
	}
	/**
	 * Unregister all registered EjbListenerPortConnector instances in one go.
	 */
	public void clear() {
		portConnectorList.clear();
	}
	/**
	 * Poll all registered EjbListenerPortConnector instances to see if they
	 * are in the same state as their associated listener-ports, and
	 * toggle their state if not.
	 */
	public void poll() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
//		  if (log.isDebugEnabled()) {
//			  log.debug("Enter polling " + this.toString() + ", thread: " + Thread.currentThread().getName());
//		  }
		for (Iterator iter = portConnectorList.iterator(); iter.hasNext();) {
			WeakReference wr = (WeakReference)iter.next();
			IListenerConnector elpc = (IListenerConnector) wr.get();
			if (elpc == null) {
				iter.remove();
				continue;
			}
			// Check for each ListenerPort if it's state matches with the
			// state that IBIS thinks it should be in.
			IPortConnectedListener listener = getListener(elpc);
			try {
				if (isClosed(elpc) != isListenerPortClosed(elpc)) {
					log.info("State of listener [" + listener.getName()
							+ "] does not match state of WebSphere ListenerPort ["
							//+ elpc.getListenerPortName(listener)
							+ "] to which it is attached; will try to change state of Receiver ["
							+ listener.getReceiver().getName() +"]");
					toggleConfiguratorState(elpc);
				}
			} catch (Exception ex) {
				log.error("Cannot change, or enquire on, state of Listener ["
						+ listener.getName() + "]", ex);
			}
		}

//		  if (log.isDebugEnabled()) {
//			  log.debug("Exit polling " + this.toString() + ", thread: " + Thread.currentThread().getName());
//		  }
	}

	private IPortConnectedListener getListener(IListenerConnector elpc) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		return (IPortConnectedListener) elpc.getClass().getMethod("getListener").invoke(elpc);
	}

	private boolean isClosed(IListenerConnector elpc) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		return (Boolean) elpc.getClass().getMethod("isClosed").invoke(elpc);
	}

	private boolean isListenerPortClosed(IListenerConnector elpc) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		return (Boolean) elpc.getClass().getMethod("isListenerPortClosed").invoke(elpc);
	}

	/**
	 * Toggle the state of the EjbListenerPortConnector instance by starting/stopping
	 * the receiver it is attached to (via the JmsListener).
	 * This method changes the state of the Receiver to match the state of the
	 * WebSphere ListenerPort.
	 *
	 * @param elpc ListenerPortConnector for which state is to be changed.
	 *
	 * @throws nl.nn.adapterframework.configuration.ConfigurationException
	 */
	public void toggleConfiguratorState(IListenerConnector elpc) throws ConfigurationException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
		GenericReceiver receiver = (GenericReceiver) getListener(elpc).getReceiver();
		if (isListenerPortClosed(elpc)) {
			if (receiver.isInRunState(RunStateEnum.STARTED)) {
				log.info("Stopping Receiver [" + receiver.getName() + "] because the WebSphere Listener-Port is in state 'stopped' but the JmsConnector in state 'open' and the receiver is in state 'started'");
				receiver.stopRunning();
			} else {
				log.info("ListenerPort [" + //elpc.getListenerPortName(getListener(elpc))
						"] is in closed state, Listener is not in closed state, but Receiver is in state ["
						+ receiver.getRunState().getName() + "] so the state of Receiver will not be changed.");

			}
		} else {
			// Only start the receiver for adapters which are running.
			if (receiver.getAdapter().getRunState().equals(RunStateEnum.STARTED)) {
				if (!receiver.isInRunState(RunStateEnum.STARTING)) {
					log.info("Starting Receiver [" + receiver.getName() + "] because the WebSphere Listener-Port is in state 'started' but the JmsConnector in state 'closed'");
					receiver.startRunning();
				} else {
					log.info("Receiver [" + receiver.getName() + "] still starting, so not changing anything now");
				}
			} else {
				try {
					log.warn("JmsConnector is closed, Adapter is not in state '" + RunStateEnum.STARTED + "', but WebSphere Jms Listener Port is in state 'started'. Stopping the listener port.");
					elpc.stop();
				} catch (ListenerException ex) {
					log.error(ex,ex);
				}
			}
		}
	}

	/**
	 * Callback method from the Spring Bean Factory to allow destruction on shutdown.
	 *
	 * This method ensures that all registered listener are cleared.
	 *
	 * @throws java.lang.Exception
	 */
	public void destroy() throws Exception {
		clear();
	}
}
