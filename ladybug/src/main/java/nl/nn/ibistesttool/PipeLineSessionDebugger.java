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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;

/**
 * Wrapper class for PipeLineSession to be able to debug storing values in
 * session keys.
 */
public class PipeLineSessionDebugger implements MethodHandler {

	private PipeLineSession pipeLineSession;
	private IbisDebugger ibisDebugger;

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
		if (method.getName().equals("put")) {
			return put((String)args[0], args[1]);
		}
		if (method.getName().equals("putAll")) {
			putAll((Map<String,Object>)args[0]);
			return null;
		}
		if(method.getName().equals("getMessage")) {
			return getMessage((String)args[0]);
		}
		return method.invoke(pipeLineSession, args);
	}

	private Object getMessage(String name) {
		Object value = pipeLineSession.getMessage(name);
		ibisDebugger.showValue(pipeLineSession.getCorrelationId(), "SessionKey "+name, value);
		return value;
	}

	private Object put(final String name, final Object originalValue) {
		Object newValue = ibisDebugger.storeInSessionKey(pipeLineSession.getCorrelationId(), name, originalValue);
		if (newValue != originalValue && newValue instanceof Message) {
			// If a session key is stubbed with a stream and this session key is not used (stream is not read) it will
			// keep the report in progress (waiting for the stream to be read, captured and closed).
			((Message)newValue).closeOnCloseOf(pipeLineSession, this.getClass().getTypeName());
		}
		return pipeLineSession.put(name, newValue);
	}

	private void putAll(Map<? extends String,? extends Object> entries) {
		for(Entry<? extends String,? extends Object> entry: entries.entrySet()) {
			put(entry.getKey(),entry.getValue());
		}
	}

}
