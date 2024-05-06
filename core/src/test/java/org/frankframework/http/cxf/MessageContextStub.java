package org.frankframework.http.cxf;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.xml.ws.handler.MessageContext;

public class MessageContextStub implements MessageContext{

	private final Map map = new HashMap();

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	@Override
	public Object get(Object key) {
		if(MessageContext.HTTP_REQUEST_METHOD.equals(key.toString())) {
			return "POST";
		}
		return map.get(key);
	}

	@Override
	public Object put(String key, Object value) {
		return map.put(key, value);
	}

	@Override
	public Object remove(Object key) {
		return map.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> m) {
		map.putAll(m);
	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public Set<String> keySet() {
		return map.keySet();
	}

	@Override
	public Collection<Object> values() {
		return map.values();
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {
		return map.entrySet();
	}

	@Override
	public void setScope(String s, Scope scope) {
		// TODO Auto-generated method stub

	}

	@Override
	public Scope getScope(String s) {
		// TODO Auto-generated method stub
		return null;
	}

}
