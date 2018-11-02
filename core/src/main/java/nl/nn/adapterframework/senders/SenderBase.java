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

import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ClassLoaderManager;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Baseclass for senders.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Sender</td><td>&nbsp;</td></tr>
 * </table>
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public abstract class SenderBase implements ISender {
	protected Logger log = LogUtil.getLogger(this);
	private String name;
	private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();


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

	protected String getLogPrefix() {
		return "["+this.getClass().getName()+"] ["+getName()+"] ";
	}

	@Override
	public void setName(String name) {
		this.name=name;
	}
	@Override
	public String getName() {
		return name;
	}

	/**
	 * This ClassLoader is set upon creation of the sender, used to retrieve files from Ibis specific ClassLoaders.
	 * @return returns the ClassLoader created by the {@link ClassLoaderManager ClassLoaderManager}.
	 */
	public ClassLoader getClassLoader() {
		return classLoader;
	}
}
