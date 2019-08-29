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
package nl.nn.adapterframework.extensions.sap.jco2.tx;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import nl.nn.adapterframework.extensions.sap.SapException;
import nl.nn.adapterframework.extensions.sap.jco2.SapSystem;

import org.springframework.transaction.support.ResourceHolderSupport;
import org.springframework.util.Assert;

import com.sap.mw.jco.JCO;
import com.sap.mw.jco.JCO.Client;

/**
 * Connection holder, wrapping a Jco client.
 *
 * <p>based on {@link org.springframework.jms.connection.JmsResourceHolder}
 *
 * <p>Note: This is an SPI class, not intended to be used by applications.
 *
 * @author  Gerrit van Brakel
 * @since   4.8
 */
public class JcoResourceHolder extends ResourceHolderSupport {

	private SapSystem sapSystem;

	private boolean frozen = false;

	private final List<JCO.Client> clients = new LinkedList<JCO.Client>();

	private final List<String> tids = new LinkedList<String>();

	private final Map<Client, List<String>> tidsPerClient = new HashMap<Client, List<String>>();


	/**
	 * Create a new JcoResourceHolder that is open for resources to be added.
	 * @see #addClient
	 * @see #addTid
	 */
	public JcoResourceHolder() {
	}

	/**
	 * Create a new JcoResourceHolder that is open for resources to be added.
	 * @param sapSystem the SapSystem that this
	 * resource holder is associated with (may be <code>null</code>)
	 */
	public JcoResourceHolder(SapSystem sapSystem) {
		this.sapSystem = sapSystem;
	}

	/**
	 * Create a new JcoResourceHolder for the given JCO.Client.
	 * @param sapSystem the SapSystem that this
	 * resource holder is associated with (may be <code>null</code>)
	 * @param client the JCO.Client
	 */
	public JcoResourceHolder(SapSystem sapSystem, JCO.Client client) {
		this.sapSystem = sapSystem;
		addClient(client);
	}

	/**
	 * Create a new JcoResourceHolder for the given JCO resources.
	 * @param client the JCO.Client
	 * @param tid the TID
	 */
	public JcoResourceHolder(JCO.Client client, String tid) {
		addClient(client);
		addTid(tid, client);
		this.frozen = true;
	}

	/**
	 * Create a new JcoResourceHolder for the given JCO resources.
	 * @param sapSystem the SapSystem that this
	 * resource holder is associated with (may be <code>null</code>)
	 * @param client the JCO.Client
	 * @param tid the TID
	 */
	public JcoResourceHolder(SapSystem sapSystem, JCO.Client client, String tid) {
		this.sapSystem = sapSystem;
		addClient(client);
		addTid(tid, client);
		this.frozen = true;
	}

	public SapSystem getSapSystem() {
		return sapSystem;
	}


	public final boolean isFrozen() {
		return this.frozen;
	}

	public final void addClient(JCO.Client client) {
		Assert.isTrue(!this.frozen, "Cannot add Client because JcoResourceHolder is frozen");
		Assert.notNull(client, "Client must not be null");
		if (!this.clients.contains(client)) {
			this.clients.add(client);
		}
	}

	public final void addTid(String tid, JCO.Client client) {
		Assert.isTrue(!this.frozen, "Cannot add TID because JcoResourceHolder is frozen");
		Assert.notNull(client, "Client must not be null");
		Assert.notNull(tid, "TID must not be null");
		if (!this.tids.contains(tid)) {
			this.tids.add(tid);
			if (client != null) {
				List<String> tids = this.tidsPerClient.get(client);
				if (tids == null) {
					tids = new LinkedList<String>();
					this.tidsPerClient.put(client, tids);
				}
				tids.add(tid);
			}
		}
	}

	public boolean containsClient(JCO.Client client) {
		return this.clients.contains(client);
	}
	public boolean containsTid(String tid) {
		return this.tids.contains(tid);
	}


	public JCO.Client getClient() {
		return (!this.clients.isEmpty() ? (JCO.Client) this.clients.get(0) : null);
	}

	public String getTid(JCO.Client client) {
		Assert.notNull(client, "Client must not be null");
		List<String> tids = this.tidsPerClient.get(client);
		if (tids==null) {
			return null;
		}
		return tids.get(tids.size()-1);
	}


	public void commitAll() throws SapException {
		for (Iterator<Client> itc = this.clients.iterator(); itc.hasNext();) {
			JCO.Client client = itc.next();
			List<String> tids = this.tidsPerClient.get(client);
			for (Iterator<String> itt = tids.iterator(); itt.hasNext();) {
				String tid = itt.next();
				try {
					client.confirmTID(tid);
				} catch (Throwable t) {
					throw new SapException("Could not confirm TID ["+tid+"]");
				}
			}
		}
	}

	public void closeAll() {
		for (Iterator<Client> itc = this.clients.iterator(); itc.hasNext();) {
			JCO.Client client = itc.next();
			JCO.releaseClient(client);
		}
	}
}
