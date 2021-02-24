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

	public String getMessageId() {
		return pipeLineSession.getMessageId();
	}

	public void setSecurityHandler(ISecurityHandler handler) {
		pipeLineSession.setSecurityHandler(handler);
	}

	public ISecurityHandler getSecurityHandler() throws NotImplementedException {
		return pipeLineSession.getSecurityHandler();
	}

	public boolean isUserInRole(String role) throws NotImplementedException {
		return pipeLineSession.isUserInRole(role);
	}

	public Principal getPrincipal() throws NotImplementedException {
		return pipeLineSession.getPrincipal();
	}

	// Methods implementing Map

	public void clear() {
		pipeLineSession.clear();
	}

	public boolean containsKey(Object arg0) {
		return pipeLineSession.containsKey(arg0);
	}

	public boolean containsValue(Object arg0) {
		return pipeLineSession.containsValue(arg0);
	}

	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		return pipeLineSession.entrySet();
	}

	public boolean equals(Object arg0) {
		return pipeLineSession.equals(arg0);
	}

	public Object get(Object arg0) {
		return pipeLineSession.get(arg0);
	}

	public int hashCode() {
		return pipeLineSession.hashCode();
	}

	public boolean isEmpty() {
		return pipeLineSession.isEmpty();
	}

	public Set keySet() {
		return pipeLineSession.keySet();
	}

	public Object put(String arg0, Object arg1) {
		arg1 = ibisDebugger.storeInSessionKey(getMessageId(), arg0, arg1);
		return pipeLineSession.put(arg0, arg1);
	}

	public void putAll(Map arg0) {
		pipeLineSession.putAll(arg0);
	}

	public Object remove(Object arg0) {
		return pipeLineSession.remove(arg0);
	}

	public int size() {
		return pipeLineSession.size();
	}

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

	public String toString() {
		return pipeLineSession.toString();
	}

}
