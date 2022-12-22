/*
   Copyright 2013, 2019 Nationale-Nederlanden, 2021, 2022 WeAreFrank!

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
package nl.nn.adapterframework.extensions.sap.jco3;

import org.apache.commons.lang3.StringUtils;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoFunction;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.extensions.sap.ISapSender;
import nl.nn.adapterframework.extensions.sap.SapException;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;

/**
 * Implementation of {@link ISender sender} that calls a SAP RFC-function.
 *
 * N.B. If no requestFieldIndex or requestFieldName is specified, input is converted from xml;
 * If no replyFieldIndex or replyFieldName is specified, output is converted to xml.
 *
 * @ff.parameter functionName   defines functionName; required when attribute <code>functionName</code> is empty
 * @ff.parameter <i>inputfieldname</i> The value of the parameter is set to the (simple) input field
 * @ff.parameter <i>structurename</i>/<i>inputfieldname</i> The value of the parameter is set to the named field of the named structure
 *
 * @author  Gerrit van Brakel
 * @author  Jaco de Groot
 * @since   5.0
 */
public abstract class SapSenderImpl extends SapSenderBase implements ISapSender {

	private @Getter String functionName=null;
	private @Getter String functionNameParam="functionName";

	public SapSenderImpl() {
		super();
		setSynchronous(true);
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getFunctionName())) {
			if (StringUtils.isEmpty(getFunctionNameParam())) {
				throw new ConfigurationException(getLogPrefix()+"if attribute functionName is not specified, value of attribute functionNameParam must indicate parameter to obtain functionName from");
			}
			if (paramList==null || paramList.findParameter(getFunctionNameParam())==null) {
				throw new ConfigurationException(getLogPrefix()+"functionName must be specified, either in attribute functionName, or via parameter ["+getFunctionNameParam()+"]");
			}
		} else {
			if (StringUtils.isNotEmpty(getFunctionNameParam()) && paramList!=null && paramList.findParameter(getFunctionNameParam())!=null) {
				throw new ConfigurationException(getLogPrefix()+"functionName cannot be specified both in attribute functionName ["+getFunctionName()+"] and via parameter ["+getFunctionNameParam()+"]");
			}
		}
	}

	public JCoFunction getFunction(SapSystemImpl sapSystem, ParameterValueList pvl) throws SapException {
		if (StringUtils.isNotEmpty(getSapSystemName()) && StringUtils.isNotEmpty(getFunctionName())) {
			return getFunctionTemplate().getFunction();
		}
		String functionName=getFunctionName();
		if (StringUtils.isEmpty(functionName)) {
			if (pvl==null) {
				throw new SapException("no parameters to determine functionName from");
			}
			ParameterValue pv = pvl.get(getFunctionNameParam());
			if (pv==null) {
				throw new SapException("could not get ParameterValue for parameter ["+getFunctionNameParam()+"]");
			}
			functionName = pv.asStringValue(null);
		}
		if (StringUtils.isEmpty(functionName)) {
			throw new SapException("could not determine functionName using parameter ["+getFunctionNameParam()+"]");
		}
		return getFunctionTemplate(sapSystem, functionName).getFunction();
	}

	@Override
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		String tid=null;
		try {
			ParameterValueList pvl = null;
			if (paramList!=null) {
				pvl = paramList.getValues(message, session);
			}
			SapSystemImpl sapSystem = getSystem(pvl);

			JCoFunction function=getFunction(sapSystem, pvl);

			if (StringUtils.isEmpty(getSapSystemName())) {
				pvl.remove(getSapSystemNameParam());
			}
			if (StringUtils.isEmpty(getFunctionName())) {
				pvl.remove(getFunctionNameParam());
			}
			String correlationID = session==null ? null : session.getCorrelationId();
			message2FunctionCall(function, message.asString(), correlationID, pvl);
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+" function call ["+functionCall2message(function)+"]");

			JCoDestination destination = getDestination(session, sapSystem);
			tid = getTid(destination,sapSystem);
			if (StringUtils.isEmpty(tid)) {
				function.execute(destination);
			} else {
				function.execute(destination,tid);
			}

			if (isSynchronous()) {
				return new SenderResult(functionResult2message(function));
			}
			return new SenderResult(tid);
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	@Override
	public void setSynchronous(boolean b) {
		super.setSynchronous(b);
	}


	/** Name of the RFC-function to be called in the SAP system */
	@Override
	public void setFunctionName(String string) {
		functionName = string;
	}


	/**
	 * Name of the parameter used to obtain the functionName from if the attribute <code>functionName</code> is empty
	 * @ff.default functionName
	 */
	@Override
	public void setFunctionNameParam(String string) {
		functionNameParam = string;
	}

}
