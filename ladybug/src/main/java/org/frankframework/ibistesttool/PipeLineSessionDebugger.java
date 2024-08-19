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
package org.frankframework.ibistesttool;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;

/**
 * Wrapper class for PipeLineSession to be able to debug storing values in
 * session keys.
 */
public class PipeLineSessionDebugger implements MethodHandler {

	private final PipeLineSession pipeLineSession;
	private final IbisDebugger ibisDebugger;

	private PipeLineSessionDebugger(PipeLineSession pipeLineSession, IbisDebugger ibisDebugger) {
		this.pipeLineSession = pipeLineSession;
		this.ibisDebugger = ibisDebugger;
	}

	public static PipeLineSession newInstance(PipeLineSession pipeLineSession, IbisDebugger ibisDebugger) throws NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		ProxyFactory factory = new ProxyFactory();
		factory.setSuperclass(PipeLineSession.class);
		PipeLineSessionDebugger handler = new PipeLineSessionDebugger(pipeLineSession, ibisDebugger);
		return (PipeLineSession)factory.create(new Class[0], new Object[0], handler);
	}

	@Override
	public Object invoke(Object self, Method method, Method proceed, Object[] args) throws Throwable {
		if ("put".equals(method.getName())) {
			return put((String)args[0], args[1]);
		}
		if ("putAll".equals(method.getName())) {
			putAll((Map<String,Object>)args[0]);
			return null;
		}
		if("getMessage".equals(method.getName())) {
			return getMessage((String)args[0]);
		}
		return method.invoke(pipeLineSession, args);
	}

	private Object getMessage(String name) {
		Object value = pipeLineSession.getMessage(name);
		ibisDebugger.showInputValue(pipeLineSession.getCorrelationId(), "SessionKey "+name, value);
		return value;
	}

	private Object put(final String name, final Object originalValue) {
		Object newValue = ibisDebugger.storeInSessionKey(pipeLineSession.getCorrelationId(), name, originalValue);
		if (newValue != originalValue && newValue instanceof Message message) {
			// If a session key is stubbed with a stream and this session key is not used (stream is not read) it will
			// keep the report in progress (waiting for the stream to be read, captured and closed).
			message.closeOnCloseOf(pipeLineSession, this.getClass().getTypeName());
		}
		return pipeLineSession.put(name, newValue);
	}

	private void putAll(Map<? extends String,? extends Object> entries) {
		for(Entry<? extends String,? extends Object> entry: entries.entrySet()) {
			put(entry.getKey(),entry.getValue());
		}
	}

}
