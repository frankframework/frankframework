/*
   Copyright 2018 Nationale-Nederlanden, 2021 WeAreFrank!

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
package nl.nn.ibistesttool;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.security.Principal;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISecurityHandler;
import nl.nn.adapterframework.stream.Message;

/**
 * Wrapper class for PipeLineSession to be able to debug storing values in
 * session keys.
 * 
 * @author  Jaco de Groot (jaco@dynasol.nl)
 */

public class PipeLineSessionDebugger implements IPipeLineSession {
	private IPipeLineSession pipeLineSession;
	private IbisDebugger ibisDebugger;

	PipeLineSessionDebugger(IPipeLineSession pipeLineSession) {
		this.pipeLineSession = pipeLineSession;
	}

	public void setIbisDebugger(IbisDebugger ibisDebugger) {
		this.ibisDebugger = ibisDebugger;
	}

	// Methods implementing IPipeLineSession

	@Override
	public String getMessageId() {
		return pipeLineSession.getMessageId();
	}

	@Override
	public void setSecurityHandler(ISecurityHandler handler) {
		pipeLineSession.setSecurityHandler(handler);
	}

	@Override
	public ISecurityHandler getSecurityHandler() throws NotImplementedException {
		return pipeLineSession.getSecurityHandler();
	}

	@Override
	public boolean isUserInRole(String role) throws NotImplementedException {
		return pipeLineSession.isUserInRole(role);
	}

	@Override
	public Principal getPrincipal() throws NotImplementedException {
		return pipeLineSession.getPrincipal();
	}

	// Methods implementing Map

	@Override
	public void clear() {
		pipeLineSession.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return pipeLineSession.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return pipeLineSession.containsValue(value);
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		return pipeLineSession.entrySet();
	}

	@Override
	public boolean equals(Object other) {
		return pipeLineSession.equals(other);
	}

	@Override
	public Object get(Object key) {
		return pipeLineSession.get(key);
	}

	@Override
	public int hashCode() {
		return pipeLineSession.hashCode();
	}

	@Override
	public boolean isEmpty() {
		return pipeLineSession.isEmpty();
	}

	@Override
	public Set<String> keySet() {
		return pipeLineSession.keySet();
	}

	@Override
	public Object put(String name, Object value) {
		Object oldValue = value;
		value = ibisDebugger.storeInSessionKey(getMessageId(), name, value);
		if (value != oldValue && value instanceof Message) {
			// If a session key is stubbed with a stream and this session key is not used (stream is not read) it will
			// keep the report in progress (waiting for the stream to be read, captured and closed).
			((Message)value).closeOnCloseOf(this);
		}
		return pipeLineSession.put(name, value);
	}

	@Override
	public void putAll(Map<? extends String,? extends Object> entries) {
		pipeLineSession.putAll(entries);
	}

	@Override
	public Object remove(Object key) {
		return pipeLineSession.remove(key);
	}

	@Override
	public int size() {
		return pipeLineSession.size();
	}

	@Override
	public Collection<Object> values() {
		return pipeLineSession.values();
	}

	@Override
	public InputStream scheduleCloseOnSessionExit(InputStream stream) {
		return pipeLineSession.scheduleCloseOnSessionExit(stream);
	}

	@Override
	public OutputStream scheduleCloseOnSessionExit(OutputStream stream) {
		return pipeLineSession.scheduleCloseOnSessionExit(stream);
	}

	@Override
	public Reader scheduleCloseOnSessionExit(Reader reader) {
		return pipeLineSession.scheduleCloseOnSessionExit(reader);
	}

	@Override
	public Writer scheduleCloseOnSessionExit(Writer writer) {
		return pipeLineSession.scheduleCloseOnSessionExit(writer);
	}

	@Override
	public void unscheduleCloseOnSessionExit(AutoCloseable resource) {
		pipeLineSession.unscheduleCloseOnSessionExit(resource);
	}

	@Override
	public void close() {
		pipeLineSession.close();
	}

	// Remaining methods

	@Override
	public String toString() {
		return pipeLineSession.toString();
	}

}
