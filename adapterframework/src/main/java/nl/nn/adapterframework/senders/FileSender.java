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
package nl.nn.adapterframework.senders;

import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.FileHandler;

/**
 * <p>See {@link FileHandler}</p>
 * 
 * @author Jaco de Groot
 */
public class FileSender extends FileHandler implements ISenderWithParameters {
	private String name;

	public String sendMessage(String correlationID, String message,
			ParameterResolutionContext prc) throws SenderException,
			TimeOutException {
		try {
			return handle(message, prc.getSession());
		} catch(Exception e) {
			throw new SenderException(e); 
		}
	}

	public String sendMessage(String correlationID, String message)
			throws SenderException, TimeOutException {
		throw new SenderException("FileSender cannot be used without a session"); 
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void open() throws SenderException {
	}

	public void close() throws SenderException {
	}

	public boolean isSynchronous() {
		return true;
	}

	public void addParameter(Parameter p) {
	}

}
