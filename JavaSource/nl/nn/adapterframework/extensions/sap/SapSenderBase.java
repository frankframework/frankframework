/*
 * $Log: SapSenderBase.java,v $
 * Revision 1.2  2008-01-30 14:41:58  europe\L190409
 * modified javadoc
 *
 * Revision 1.1  2008/01/29 15:37:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * base class extracted from SapSender
 *
 */
package nl.nn.adapterframework.extensions.sap;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.extensions.sap.tx.ClientFactoryUtils;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;

import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.sap.mw.jco.JCO;

/**
 * Base class for functions that call SAP.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSapSystemName(String) sapSystemName}</td><td>name of the {@link SapSystem} used by this object</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSapSystemNameParam(String) sapSystemNameParam}</td><td>name of the parameter used to indicate the name of the {@link SapSystem} used by this object if the attribute <code>sapSystemName</code> is empty</td><td>sapSystemName</td></tr>
 * <tr><td>{@link #setLuwHandleSessionKey(String) luwHandleSessionKey}</td><td>session key in which LUW information is stored. When set, actions that share a LUW-handle will be executed using the same client. Can only be used for synchronous functions</td><td>&nbsp;</td></tr>
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
 * @since   4.8
 * @version Id
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

	public JCO.Client getClient(PipeLineSession session, SapSystem sapSystem) throws SenderException, SapException {
		JCO.Client result;
		if (isSynchronous()) {
			if (StringUtils.isNotEmpty(getLuwHandleSessionKey())) {
				SapLUWHandle handle = SapLUWHandle.retrieveHandle(session, getLuwHandleSessionKey(), true, sapSystem, false);
				if (handle==null) {
					throw new SenderException("cannot find LUW handle from session key ["+getLuwHandleSessionKey()+"]");
				}
				result = handle.getClient();
			} else {
				result = sapSystem.getClient();
			}
		} else {
			result = ClientFactoryUtils.getTransactionalClient(sapSystem, true);
			if (result==null) {
				if (!TransactionSynchronizationManager.isSynchronizationActive()) {
					throw new SenderException("can only be called from within a transaction");
				}
				throw new SenderException(getLogPrefix()+"Could not obtain Jco Client");
			}
		}
		return result;
	}

	public void releaseClient(JCO.Client client, SapSystem sapSystem) {
		if (isSynchronous() && client!=null) {
			sapSystem.releaseClient(client);
		}
	}
	
	public String getTid(JCO.Client client, SapSystem sapSystem) throws SapException {
		if (isSynchronous()) {
			return null;
		}
		return ClientFactoryUtils.getTransactionalTid(sapSystem,client,true);
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
