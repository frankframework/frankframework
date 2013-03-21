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
/*
 * $Log: SapListener.java,v $
 * Revision 1.15  2012-02-06 14:33:04  m00f069
 * Implemented JCo 3 based on the JCo 2 code. JCo2 code has been moved to another package, original package now contains classes to detect the JCo version available and use the corresponding implementation.
 *
 */
package nl.nn.adapterframework.extensions.sap;

import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPushingListener;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;

/**
 * Depending on the JCo version found (see {@link JCoVersion}) delegate to
 * {@link nl.nn.adapterframework.extensions.sap.jco3.SapListener jco3.SapListener} or
 * {@link nl.nn.adapterframework.extensions.sap.jco2.SapListener jco2.SapListener}
 * Don't use the jco3 or jco2 class in your Ibis configuration, use this one
 * instead.
 * 
 * @author  Jaco de Groot
 * @since   5.0
 * @version $Id$
 */
public class SapListener implements IPushingListener {
	private int jcoVersion = 3;
	private nl.nn.adapterframework.extensions.sap.jco3.SapListener sapListener3;
	private nl.nn.adapterframework.extensions.sap.jco2.SapListener sapListener2;

	public SapListener() throws ConfigurationException {
		jcoVersion = JCoVersion.getInstance().getJCoVersion();
		if (jcoVersion == -1) {
			throw new ConfigurationException(JCoVersion.getInstance().getErrorMessage());
		} else if (jcoVersion == 3) {
			sapListener3 = new nl.nn.adapterframework.extensions.sap.jco3.SapListener();
		} else {
			sapListener2 = new nl.nn.adapterframework.extensions.sap.jco2.SapListener();
		}
	}

	public void configure() throws ConfigurationException {
		if (jcoVersion == 3) {
			sapListener3.configure();
		} else {
			sapListener2.configure();
		}
	}

	public void open() throws ListenerException {
		if (jcoVersion == 3) {
			sapListener3.open();
		} else {
			sapListener2.open();
		}
	}

	public void close() throws ListenerException {
		if (jcoVersion == 3) {
			sapListener3.close();
		} else {
			sapListener2.close();
		}
	}

	public String getIdFromRawMessage(Object rawMessage, Map context)
			throws ListenerException {
		if (jcoVersion == 3) {
			return sapListener3.getIdFromRawMessage(rawMessage, context);
		} else {
			return sapListener2.getIdFromRawMessage(rawMessage, context);
		}
	}

	public String getStringFromRawMessage(Object rawMessage, Map context)
			throws ListenerException {
		if (jcoVersion == 3) {
			return sapListener3.getStringFromRawMessage(rawMessage, context);
		} else {
			return sapListener2.getStringFromRawMessage(rawMessage, context);
		}
	}

	public void afterMessageProcessed(PipeLineResult processResult,
			Object rawMessage, Map context) throws ListenerException {
		if (jcoVersion == 3) {
			sapListener3.afterMessageProcessed(processResult, rawMessage, context);
		} else {
			sapListener2.afterMessageProcessed(processResult, rawMessage, context);
		}
	}

	public String getName() {
		if (jcoVersion == 3) {
			return sapListener3.getName();
		} else {
			return sapListener2.getName();
		}
	}

	public void setName(String name) {
		if (jcoVersion == 3) {
			sapListener3.setName(name);
		} else {
			sapListener2.setName(name);
		}
	}

	public void setHandler(IMessageHandler handler) {
		if (jcoVersion == 3) {
			sapListener3.setHandler(handler);
		} else {
			sapListener2.setHandler(handler);
		}
	}

	public void setExceptionListener(IbisExceptionListener listener) {
		if (jcoVersion == 3) {
			sapListener3.setExceptionListener(listener);
		} else {
			sapListener2.setExceptionListener(listener);
		}
	}

	public void setSapSystemName(String string) {
		if (jcoVersion == 3) {
			sapListener3.setSapSystemName(string);
		} else {
			sapListener2.setSapSystemName(string);
		}
	}

	public void setProgid(String string) {
		if (jcoVersion == 3) {
			sapListener3.setProgid(string);
		} else {
			sapListener2.setProgid(string);
		}
	}

	public void setConnectionCount(String connectionCount) {
		if (jcoVersion == 3) {
			sapListener3.setConnectionCount(connectionCount);
		}
	}

	public void setCorrelationIdFieldIndex(int i) {
		if (jcoVersion == 3) {
			sapListener3.setCorrelationIdFieldIndex(i);
		} else {
			sapListener2.setCorrelationIdFieldIndex(i);
		}
	}

	public void setCorrelationIdFieldName(String string) {
		if (jcoVersion == 3) {
			sapListener3.setCorrelationIdFieldName(string);
		} else {
			sapListener2.setCorrelationIdFieldName(string);
		}
	}

	public void setReplyFieldIndex(int i) {
		if (jcoVersion == 3) {
			sapListener3.setReplyFieldIndex(i);
		} else {
			sapListener2.setReplyFieldIndex(i);
		}
	}

	public void setReplyFieldName(String string) {
		if (jcoVersion == 3) {
			sapListener3.setReplyFieldName(string);
		} else {
			sapListener2.setReplyFieldName(string);
		}
	}

	public void setRequestFieldIndex(int i) {
		if (jcoVersion == 3) {
			sapListener3.setRequestFieldIndex(i);
		} else {
			sapListener2.setRequestFieldIndex(i);
		}
	}

	public void setRequestFieldName(String string) {
		if (jcoVersion == 3) {
			sapListener3.setRequestFieldName(string);
		} else {
			sapListener2.setRequestFieldName(string);
		}
	}

}
