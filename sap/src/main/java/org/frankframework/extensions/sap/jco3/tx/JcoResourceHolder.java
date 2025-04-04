/*
   Copyright 2013 Nationale-Nederlanden, 2022 WeAreFrank!

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
package org.frankframework.extensions.sap.jco3.tx;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sap.conn.jco.JCoDestination;

import org.frankframework.extensions.sap.SapException;
import org.frankframework.extensions.sap.jco3.SapSystemImpl;
import org.springframework.transaction.support.ResourceHolderSupport;
import org.springframework.util.Assert;

/**
 * Connection holder, wrapping a Jco destination.
 *
 * <p>based on {@code org.springframework.jms.connection.JmsResourceHolder}
 *
 * <p>Note: This is an SPI class, not intended to be used by applications.
 *
 * @author  Gerrit van Brakel
 * @author  Jaco de Groot
 * @since   5.0
 */
public class JcoResourceHolder extends ResourceHolderSupport {

	private boolean frozen = false;

	private final List<JCoDestination> destinations = new LinkedList<>();

	private final List<String> tids = new LinkedList<>();

	private final Map<JCoDestination,List<String>> tidsPerDestination = new HashMap<>();


	/**
	 * Create a new JcoResourceHolder that is open for resources to be added.
	 * @see #addDestination
	 * @see #addTid
	 */
	public JcoResourceHolder() {
	}

	/**
	 * Create a new JcoResourceHolder that is open for resources to be added.
	 * @param sapSystem the SapSystem that this
	 * resource holder is associated with (may be <code>null</code>)
	 */
	public JcoResourceHolder(SapSystemImpl sapSystem) {
	}

	/**
	 * Create a new JcoResourceHolder for the given JCoDestination.
	 * @param sapSystem the SapSystem that this
	 * resource holder is associated with (may be <code>null</code>)
	 * @param destination the JCoDestination
	 */
	public JcoResourceHolder(SapSystemImpl sapSystem, JCoDestination destination) {
		addDestination(destination);
	}

	/**
	 * Create a new JcoResourceHolder for the given JCO resources.
	 * @param destination the JCoDestination
	 * @param tid the TID
	 */
	public JcoResourceHolder(JCoDestination destination, String tid) {
		addDestination(destination);
		addTid(tid, destination);
		this.frozen = true;
	}

	/**
	 * Create a new JcoResourceHolder for the given JCO resources.
	 * @param sapSystem the SapSystem that this
	 * resource holder is associated with (may be <code>null</code>)
	 * @param destination the JCoDestination
	 * @param tid the TID
	 */
	public JcoResourceHolder(SapSystemImpl sapSystem, JCoDestination destination, String tid) {
		addDestination(destination);
		addTid(tid, destination);
		this.frozen = true;
	}


	public final boolean isFrozen() {
		return this.frozen;
	}

	public final void addDestination(JCoDestination destination) {
		Assert.isTrue(!this.frozen, "Cannot add Destination because JcoResourceHolder is frozen");
		Assert.notNull(destination, "Destination must not be null");
		if (!this.destinations.contains(destination)) {
			this.destinations.add(destination);
		}
	}

	public final void addTid(String tid, JCoDestination destination) {
		Assert.isTrue(!this.frozen, "Cannot add TID because JcoResourceHolder is frozen");
		Assert.notNull(destination, "Destination must not be null");
		Assert.notNull(tid, "TID must not be null");
		if (!this.tids.contains(tid)) {
			this.tids.add(tid);
			if (destination != null) {
				List<String> tids = this.tidsPerDestination.get(destination);
				if (tids == null) {
					tids = new LinkedList<>();
					this.tidsPerDestination.put(destination, tids);
				}
				tids.add(tid);
			}
		}
	}

	public boolean containsDestination(JCoDestination destination) {
		return this.destinations.contains(destination);
	}
	public boolean containsTid(String tid) {
		return this.tids.contains(tid);
	}


	public JCoDestination getDestination() {
		return !this.destinations.isEmpty() ? this.destinations.get(0) : null;
	}

	public String getTid(JCoDestination destination) {
		Assert.notNull(destination, "Destination must not be null");
		List<String> tids = this.tidsPerDestination.get(destination);
		if (tids==null) {
			return null;
		}
		return tids.get(tids.size()-1);
	}


	public void commitAll() throws SapException {
		for (Iterator<JCoDestination> itc = this.destinations.iterator(); itc.hasNext();) {
			JCoDestination destination = itc.next();
			List<String> tids = this.tidsPerDestination.get(destination);
			for (Iterator<String> itt = tids.iterator(); itt.hasNext();) {
				String tid = itt.next();
				try {
					destination.confirmTID(tid);
				} catch (Throwable t) {
					throw new SapException("Could not confirm TID ["+tid+"]");
				}
			}
		}
	}

}
