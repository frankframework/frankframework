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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.IParameterHandler;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Sender that just logs its message.
 * 
 * @author Gerrit van Brakel
 * @since  4.9
 */
public class LogSender extends SenderWithParametersBase implements IParameterHandler {
	private String logLevel="info";
	private String logCategory=null;

	protected Level level;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		log=LogUtil.getLogger(getLogCategory());
		level=Level.toLevel(getLogLevel());
	}

	@Override
	public boolean isSynchronous() {
		return true;
	}

	@Override
	public Message sendMessage(Message message, IPipeLineSession session) throws SenderException, TimeOutException {
		log.log(level,message);
		if (getParameterList() != null) {
			try {
				ParameterValueList pvl = getParameterList().getValues(message, session);
				if (pvl != null) {
					pvl.forAllParameters(this);
				}
			} catch (ParameterException e) {
				throw new SenderException("exception determining value of parameters", e);
			}
		}
		return message;
	}

	@Override
	public void handleParam(String paramName, Object value) {
		log.log(level,"parameter [" + paramName + "] value [" + value + "]");
	}

	public String getLogCategory() {
		if (StringUtils.isNotEmpty(logCategory)) {
			return logCategory;
		}
		if (StringUtils.isNotEmpty(getName())) {
			return getName();
		}
		return this.getClass().getName();
	}

	@IbisDoc({"category under which messages are logged", "name of the sender"})
	public void setLogCategory(String string) {
		logCategory = string;
	}

	public String getLogLevel() {
		return logLevel;
	}

	@IbisDoc({"level on which messages are logged", "info"})
	public void setLogLevel(String string) {
		logLevel = string;
	}

	@Override
	public String toString() {
		return "LogSender ["+getName()+"] logLevel ["+getLogLevel()+"] logCategory ["+logCategory+"]";
	}

}
