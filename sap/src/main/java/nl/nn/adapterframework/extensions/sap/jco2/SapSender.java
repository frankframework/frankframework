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
package nl.nn.adapterframework.extensions.sap.jco2;

import org.apache.commons.lang3.StringUtils;

import com.sap.mw.jco.JCO;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.extensions.sap.ISapSender;
import nl.nn.adapterframework.extensions.sap.SapException;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;

/**
 * Implementation of {@link nl.nn.adapterframework.core.ISender sender} that calls a SAP RFC-function.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>sapSystemName</td><td>String</td><td>points to {@link SapSystem} to use; required when attribute <code>sapSystemName</code> is empty</td></tr>
 * <tr><td>functionName</td><td>String</td><td>defines functionName; required when attribute <code>functionName</code> is empty</td></tr>
 * <tr><td><i>inputfieldname</i></td><td><i>any</i></td><td>The value of the parameter is set to the (simple) input field</td></tr>
 * <tr><td><i>structurename</i>/<i>inputfieldname</i></td><td><i>any</i></td><td>The value of the parameter is set to the named field of the named structure</td></tr>
 * </table>
 * </p>
 * N.B. If no requestFieldIndex or requestFieldName is specified, input is converted from xml;
 * If no replyFieldIndex or replyFieldName is specified, output is converted to xml. 
 * </p>
 * @author  Gerrit van Brakel
 * @since   4.2
 */
public class SapSender extends SapSenderBase implements ISapSender {
	
	private @Getter String functionName=null;
	private @Getter String functionNameParam="functionName";

	public SapSender() {
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

	public JCO.Function getFunction(SapSystem sapSystem, ParameterValueList pvl) throws SapException {
		if (StringUtils.isNotEmpty(getSapSystemName()) && StringUtils.isNotEmpty(getFunctionName())) {
			return getFunctionTemplate().getFunction();
		}
		String functionName=getFunctionName();
		if (StringUtils.isEmpty(functionName)) {
			if (pvl==null) {
				throw new SapException("no parameters to determine functionName from");
			}
			ParameterValue pv = pvl.getParameterValue(getFunctionNameParam());
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
	public Message sendMessage(Message message, PipeLineSession session) throws SenderException, TimeOutException {
		String tid=null;
		try {
			ParameterValueList pvl = null;
			if (paramList!=null) {
				pvl = paramList.getValues(message, session);
			}
			SapSystem sapSystem = getSystem(pvl);
			
			JCO.Function function=getFunction(sapSystem, pvl);

			if (StringUtils.isEmpty(getSapSystemName())) {
				pvl.remove(getSapSystemNameParam());
			}
			if (StringUtils.isEmpty(getFunctionName())) {
				pvl.remove(getFunctionNameParam());
			}
			String correlationID = session==null ? null : session.getMessageId();
		    message2FunctionCall(function, message.asString(), correlationID, pvl);
		    if (log.isDebugEnabled()) log.debug(getLogPrefix()+" function call ["+functionCall2message(function)+"]");
			JCO.Client client = getClient(session, sapSystem);
			try {
				tid = getTid(client,sapSystem);
				if (StringUtils.isEmpty(tid)) {
					client.execute(function);
				} else {
					client.execute(function,tid);
				}
			} finally {
				releaseClient(client,sapSystem);
			}
			if (isSynchronous()) {
				return functionResult2message(function);
			}
			return new Message(tid);
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	@Override
	public void setSynchronous(boolean b) {
		super.setSynchronous(b);
	}


	@IbisDoc({"1", "Name of the RFC-function to be called in the SAP system", ""})
	@Override
	public void setFunctionName(String string) {
		functionName = string;
	}


	@IbisDoc({"2", "name of the parameter used to obtain the functionName from if the attribute <code>functionName</code> is empty", "functionName"})
	@Override
	public void setFunctionNameParam(String string) {
		functionNameParam = string;
	}

}
