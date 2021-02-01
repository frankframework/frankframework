/*
   Copyright 2013, 2016, 2018 Nationale-Nederlanden

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
package nl.nn.adapterframework.senders;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Baseclass for senders.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public abstract class SenderBase implements ISender {
	protected Logger log = LogUtil.getLogger(this);
	private String name;
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();


	@Override
	public void configure() throws ConfigurationException {
	}

	@Override
	public void open() throws SenderException {
	}

	@Override
	public void close() throws SenderException {
	}


	@Override
	public boolean isSynchronous() {
		return true;
	}

	/**
	 * Returns the true name of the class and not <code>XsltPipe$$EnhancerBySpringCGLIB$$563e6b5d</code>.
	 * {@link ClassUtils#nameOf(Object)} makes sure the original class will be used.
	 * 
	 * @return className + name of the ISender
	 */
	protected String getLogPrefix() {
		return ClassUtils.nameOf(this) +" ["+getName()+"] ";
	}

	@IbisDoc({"name of the sender", ""})
	@Override
	public void setName(String name) {
		this.name=name;
	}
	@Override
	public String getName() {
		return name;
	}
}
