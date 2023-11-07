/*
   Copyright 2013 Nationale-Nederlanden, 2021, 2022 WeAreFrank!

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
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.extensions.sap.SapException;
import nl.nn.adapterframework.extensions.sap.jco3.tx.DestinationFactoryUtils;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;

/**
 * Base class for functions that call SAP.
 * 
 * @ff.parameter sapSystemName  points to {@link SapSystemImpl} to use; required when attribute <code>sapSystemName</code> is empty

 * @author  Gerrit van Brakel
 * @author  Jaco de Groot
 * @since   5.0
 */
public abstract class SapSenderBase extends SapFunctionFacade implements ISenderWithParameters {

	private @Getter String luwHandleSessionKey;
	private @Getter String sapSystemNameParam="sapSystemName";
	private @Getter boolean synchronous=false;

	protected ParameterList paramList = null;

	@Override
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

	@Override
	public void open() throws SenderException {
		try {
			openFacade();
		} catch (SapException e) {
			log.error(getLogPrefix() + "Exception on opening SapFunctionFacade", e);
			close();
			throw new SenderException(getLogPrefix()+"exception starting", e);
		}
	}

	@Override
	public void close() {
		closeFacade();
	}

	public SapSystemImpl getSystem(ParameterValueList pvl) throws SapException {
		if (StringUtils.isNotEmpty(getSapSystemName())) {
			return getSapSystem();
		}
		if (pvl==null) {
			throw new SapException("no parameters to determine sapSystemName from");
		}
		String SapSystemName=pvl.get(getSapSystemNameParam()).asStringValue(null);
		if (StringUtils.isEmpty(SapSystemName)) {
			throw new SapException("could not determine sapSystemName using parameter ["+getSapSystemNameParam()+"]");
		}
		SapSystemImpl result = getSapSystem(SapSystemName);
		if (log.isDebugEnabled()) log.debug(getLogPrefix()+"determined SapSystemName ["+SapSystemName+"]"); 
		if (result==null) {
			log.warn(getLogPrefix()+"could not find a SapSystem ["+SapSystemName+"] from Parameter ["+getSapSystemNameParam()+"]");
		}
		return getSapSystem(SapSystemName);
	}

	public JCoDestination getDestination(PipeLineSession session, SapSystemImpl sapSystem) throws SenderException, SapException, JCoException {
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

	public String getTid(JCoDestination destination, SapSystemImpl sapSystem) throws SapException, JCoException {
		if (isSynchronous()) {
			return null;
		}
		return DestinationFactoryUtils.getTransactionalTid(sapSystem,destination,true);
	}

	@Override
	public void addParameter(Parameter p) {
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}

	@Override
	public ParameterList getParameterList() {
		return paramList;
	}


	/** Session key in which LUW information is stored. If set, actions that share a LUW-handle will be executed using the same destination. Can only be used for synchronous functions */
	public void setLuwHandleSessionKey(String string) {
		luwHandleSessionKey = string;
	}

	/**
	 * Name of the parameter used to indicate the name of the {@link SapSystem} used by this object if the attribute <code>sapSystemName</code> is empty
	 * @ff.default sapSystemName
	 */
	public void setSapSystemNameParam(String string) {
		sapSystemNameParam = string;
	}

	/**
	 * If <code>false</code>, the sender operates in RR mode: the a reply is expected from SAP, and the sender does not participate in a transaction. When <code>false</code>, the sender operates in FF mode: no reply is expected from SAP, and the sender joins the transaction, that must be present. The SAP transaction is committed right after the XA transaction is completed.
	 * @ff.default false
	 */
	protected void setSynchronous(boolean b) {
		synchronous = b;
	}

}
