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
 * $Log: SapSender.java,v $
 * Revision 1.17  2012-02-06 14:33:04  m00f069
 * Implemented JCo 3 based on the JCo 2 code. JCo2 code has been moved to another package, original package now contains classes to detect the JCo version available and use the corresponding implementation.
 *
 */
package nl.nn.adapterframework.extensions.sap;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

/**
 * Depending on the JCo version found (see {@link JCoVersion}) delegate to
 * {@link nl.nn.adapterframework.extensions.sap.jco3.SapSender jco3.SapSender} or
 * {@link nl.nn.adapterframework.extensions.sap.jco2.SapSender jco2.SapSender}
 * Don't use the jco3 or jco2 class in your Ibis configuration, use this one
 * instead.
 * 
 * @author  Jaco de Groot
 * @since   5.0
 * @version $Id$
 */
public class SapSender implements ISenderWithParameters {
	private int jcoVersion = -1;
	private nl.nn.adapterframework.extensions.sap.jco3.SapSender sapSender3;
	private nl.nn.adapterframework.extensions.sap.jco2.SapSender sapSender2;

	public SapSender() throws ConfigurationException {
		jcoVersion = JCoVersion.getInstance().getJCoVersion();
		if (jcoVersion == -1) {
			throw new ConfigurationException(JCoVersion.getInstance().getErrorMessage());
		} else if (jcoVersion == 3) {
			sapSender3 = new nl.nn.adapterframework.extensions.sap.jco3.SapSender();
		} else {
			sapSender2 = new nl.nn.adapterframework.extensions.sap.jco2.SapSender();
		}
	}

	public String getName() {
		if (jcoVersion == 3) {
			return sapSender3.getName();
		} else {
			return sapSender2.getName();
		}
	}

	public void setName(String name) {
		if (jcoVersion == 3) {
			sapSender3.setName(name);
		} else {
			sapSender2.setName(name);
		}
	}

	public void configure() throws ConfigurationException {
		if (jcoVersion == 3) {
			sapSender3.configure();
		} else {
			sapSender2.configure();
		}
	}

	public void open() throws SenderException {
		if (jcoVersion == 3) {
			sapSender3.open();
		} else {
			sapSender2.open();
		}
	}

	public void close() throws SenderException {
		if (jcoVersion == 3) {
			sapSender3.close();
		} else {
			sapSender2.close();
		}
	}

	public boolean isSynchronous() {
		if (jcoVersion == 3) {
			return sapSender3.isSynchronous();
		} else {
			return sapSender2.isSynchronous();
		}
	}

	public void addParameter(Parameter p) {
		if (jcoVersion == 3) {
			sapSender3.addParameter(p);
		} else {
			sapSender2.addParameter(p);
		}
	}

	public String sendMessage(String correlationID, String message)
			throws SenderException, TimeOutException {
		if (jcoVersion == 3) {
			return sapSender3.sendMessage(correlationID, message);
		} else {
			return sapSender2.sendMessage(correlationID, message);
		}
	}

	public String sendMessage(String correlationID, String message,
			ParameterResolutionContext prc) throws SenderException,
			TimeOutException {
		if (jcoVersion == 3) {
			return sapSender3.sendMessage(correlationID, message, prc);
		} else {
			return sapSender2.sendMessage(correlationID, message, prc);
		}
	}

	public void setSynchronous(boolean b) {
		if (jcoVersion == 3) {
			sapSender3.setSynchronous(b);
		} else {
			sapSender2.setSynchronous(b);
		}
	}

	public String getFunctionName() {
		if (jcoVersion == 3) {
			return sapSender3.getFunctionName();
		} else {
			return sapSender2.getFunctionName();
		}
	}

	public void setFunctionName(String string) {
		if (jcoVersion == 3) {
			sapSender3.setFunctionName(string);
		} else {
			sapSender2.setFunctionName(string);
		}
	}

	public void setFunctionNameParam(String string) {
		if (jcoVersion == 3) {
			sapSender3.setFunctionNameParam(string);
		} else {
			sapSender2.setFunctionNameParam(string);
		}
	}

	public void setLuwHandleSessionKey(String string) {
		if (jcoVersion == 3) {
			sapSender3.setLuwHandleSessionKey(string);
		} else {
			sapSender2.setLuwHandleSessionKey(string);
		}
	}

	public void setSapSystemName(String string) {
		if (jcoVersion == 3) {
			sapSender3.setSapSystemName(string);
		} else {
			sapSender2.setSapSystemName(string);
		}
	}

	public void setSapSystemNameParam(String string) {
		if (jcoVersion == 3) {
			sapSender3.setSapSystemNameParam(string);
		} else {
			sapSender2.setSapSystemNameParam(string);
		}
	}

	public String getRequestFieldName() {
		if (jcoVersion == 3) {
			return sapSender3.getRequestFieldName();
		} else {
			return sapSender2.getRequestFieldName();
		}
	}

	public void setCorrelationIdFieldIndex(int i) {
		if (jcoVersion == 3) {
			sapSender3.setCorrelationIdFieldIndex(i);
		} else {
			sapSender2.setCorrelationIdFieldIndex(i);
		}
	}

	public void setCorrelationIdFieldName(String string) {
		if (jcoVersion == 3) {
			sapSender3.setCorrelationIdFieldName(string);
		} else {
			sapSender2.setCorrelationIdFieldName(string);
		}
	}

	public void setReplyFieldIndex(int i) {
		if (jcoVersion == 3) {
			sapSender3.setReplyFieldIndex(i);
		} else {
			sapSender2.setReplyFieldIndex(i);
		}
	}

	public void setReplyFieldName(String string) {
		if (jcoVersion == 3) {
			sapSender3.setReplyFieldName(string);
		} else {
			sapSender2.setReplyFieldName(string);
		}
	}

	public void setRequestFieldIndex(int i) {
		if (jcoVersion == 3) {
			sapSender3.setRequestFieldIndex(i);
		} else {
			sapSender2.setRequestFieldIndex(i);
		}
	}

	public void setRequestFieldName(String string) {
		if (jcoVersion == 3) {
			sapSender3.setRequestFieldName(string);
		} else {
			sapSender2.setRequestFieldName(string);
		}
	}

}
