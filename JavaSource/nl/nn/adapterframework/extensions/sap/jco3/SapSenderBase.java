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
package nl.nn.adapterframework.extensions.sap.jco3;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.extensions.sap.jco3.tx.DestinationFactoryUtils;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;

import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;

/**
 * Base class for functions that call SAP.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSapSystemName(String) sapSystemName}</td><td>name of the {@link SapSystem} used by this object</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSapSystemNameParam(String) sapSystemNameParam}</td><td>name of the parameter used to indicate the name of the {@link SapSystem} used by this object if the attribute <code>sapSystemName</code> is empty</td><td>sapSystemName</td></tr>
 * <tr><td>{@link #setLuwHandleSessionKey(String) luwHandleSessionKey}</td><td>session key in which LUW information is stored. When set, actions that share a LUW-handle will be executed using the same destination. Can only be used for synchronous functions</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSynchronous(boolean) synchronous}</td><td>when <code>false</code>, the sender operates in RR mode: the a reply is expected from SAP, and the sender does not participate in a transaction. When <code>false</code>, the sender operates in FF mode: no reply is expected from SAP, and the sender joins the transaction, that must be present. The SAP transaction is committed right after the XA transaction is completed.</td><td>false</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>sapSystemName</td><td>String</td><td>points to {@link SapSystem} to use; required when attribute <code>sapSystemName</code> is empty</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @author  Jaco de Groot
 * @since   5.0
 * @version $Id$
 */
public abstract class SapSenderBase extends SapFunctionFacade implements ISenderWithParameters {

	private String luwHandleSessionKey;
	private String sapSystemNameParam="sapSystemName";
	private boolean synchronous=false;

	protected ParameterList paramList = null;
	
	
	public void configure() throws ConfigurationException {
		super.configure();
		if (paramList!=null) {
			paramList.configure();
		}
		if (StringUtils.isEmpty(getSapSystemName())) {
			if (StringUtils.isEmpty(getSapSystemNameParam())) {
				throw new ConfigurationException(getLogPrefix()+"if attribute sapSystemName is not specified, value of attribute sapSystemNameParam must indicate parameter to obtain name of sapSystem from");
			}
			if (paramList==null || paramList.findParameter(getSapSystemNameParam())==null) {
				throw new ConfigurationException(getLogPrefix()+"sapSystem must be specified, either in attribute sapSystemName, or via parameter ["+getSapSystemNameParam()+"]");
			}
		}
		if (!isSynchronous() && StringUtils.isNotEmpty(getLuwHandleSessionKey())) {
			throw new ConfigurationException(getLogPrefix()+"luwHandleSessionKey can only be used for synchronous calls to SAP");
		}
	}

	public void open() throws SenderException {
		try {
			openFacade();
		} catch (SapException e) {
			close();
			throw new SenderException(getLogPrefix()+"exception starting", e);
		}
	}
	
	public void close() {
		closeFacade();
	}

	public String sendMessage(String correlationID, String message) throws SenderException, TimeOutException {
		return sendMessage(correlationID,message,null);
	}

	public SapSystem getSystem(ParameterValueList pvl) throws SapException {
		if (StringUtils.isNotEmpty(getSapSystemName())) {
			return getSapSystem();
		}
		if (pvl==null) {
			throw new SapException("no parameters to determine sapSystemName from");
		}
		String SapSystemName=pvl.getParameterValue(getSapSystemNameParam()).asStringValue(null);
		if (StringUtils.isEmpty(SapSystemName)) {
			throw new SapException("could not determine sapSystemName using parameter ["+getSapSystemNameParam()+"]");
		}
		SapSystem result = getSapSystem(SapSystemName);
		if (log.isDebugEnabled()) log.debug(getLogPrefix()+"determined SapSystemName ["+SapSystemName+"]"); 
		if (result==null) {
			log.warn(getLogPrefix()+"could not find a SapSystem ["+SapSystemName+"] from Parameter ["+getSapSystemNameParam()+"]");
		}
		return getSapSystem(SapSystemName);
	}

	public JCoDestination getDestination(IPipeLineSession session, SapSystem sapSystem) throws SenderException, SapException, JCoException {
		JCoDestination result;
		if (isSynchronous()) {
			if (StringUtils.isNotEmpty(getLuwHandleSessionKey())) {
				SapLUWHandle handle = SapLUWHandle.retrieveHandle(session, getLuwHandleSessionKey(), true, sapSystem, false);
				if (handle==null) {
					throw new SenderException("cannot find LUW handle from session key ["+getLuwHandleSessionKey()+"]");
				}
				result = handle.getDestination();
			} else {
				result = sapSystem.getDestination();
			}
		} else {
			result = DestinationFactoryUtils.getTransactionalDestination(sapSystem, true);
			if (result==null) {
				if (!TransactionSynchronizationManager.isSynchronizationActive()) {
					throw new SenderException("can only be called from within a transaction");
				}
				throw new SenderException(getLogPrefix()+"Could not obtain Jco Destination");
			}
		}
		return result;
	}

	public String getTid(JCoDestination destination, SapSystem sapSystem) throws SapException, JCoException {
		if (isSynchronous()) {
			return null;
		}
		return DestinationFactoryUtils.getTransactionalTid(sapSystem,destination,true);
	}

	public void addParameter(Parameter p) {
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}



	public void setLuwHandleSessionKey(String string) {
		luwHandleSessionKey = string;
	}
	public String getLuwHandleSessionKey() {
		return luwHandleSessionKey;
	}

	public void setSapSystemNameParam(String string) {
		sapSystemNameParam = string;
	}
	public String getSapSystemNameParam() {
		return sapSystemNameParam;
	}

	protected void setSynchronous(boolean b) {
		synchronous = b;
	}
	public boolean isSynchronous() {
		return synchronous;
	}

}
