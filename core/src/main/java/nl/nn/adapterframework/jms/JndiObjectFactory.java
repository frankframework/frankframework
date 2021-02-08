/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.jms;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;

import org.springframework.jndi.JndiLocatorSupport;

import lombok.SneakyThrows;

public class JndiObjectFactory<O extends L,L> extends JndiLocatorSupport {
	
	private Class<L> lookupClass;
	
	protected Map<String,O> objects = new ConcurrentHashMap<>();
	
	public JndiObjectFactory(Class<L> lookupClass) {
		this.lookupClass = lookupClass;
		setResourceRef(true); //the prefix "java:comp/env/" will be added if the JNDI name doesn't already contain it. 
	}
	
	public O get(String objectName) throws NamingException {
		return objects.computeIfAbsent(objectName, k -> compute(k));
	}
	
	@SneakyThrows(NamingException.class)
	private O compute(String objectName) {
		return augment(lookup(objectName), objectName);
	}

	@Override
	protected L lookup(String jndiName) throws NamingException {
		return super.lookup(jndiName, lookupClass);
	}

	@SuppressWarnings("unchecked")
	protected O augment(L object, String objectName) {
		return (O)object;
	}

}
