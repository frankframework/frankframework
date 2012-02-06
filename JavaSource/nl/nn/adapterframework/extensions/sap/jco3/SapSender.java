/*
 * $Log: SapSender.java,v $
 * Revision 1.1  2012-02-06 14:33:04  m00f069
 * Implemented JCo 3 based on the JCo 2 code. JCo2 code has been moved to another package, original package now contains classes to detect the JCo version available and use the corresponding implementation.
 *
 * Revision 1.16  2011/11/30 13:51:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:52  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.14  2010/03/10 14:30:05  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * rolled back testtool adjustments (IbisDebuggerDummy)
 *
 * Revision 1.12  2009/08/26 15:34:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.11  2008/04/24 12:19:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fix NPE in configure() when no parameters present
 *
 * Revision 1.10  2008/01/30 14:42:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * return TID for asynchronous functions
 *
 * Revision 1.9  2008/01/29 15:39:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved some code to baseclass
 * added support for dynamic selection of sapsystem
 *
 * Revision 1.8  2007/05/02 11:33:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for handling parameters
 *
 * Revision 1.7  2007/05/01 14:22:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of SAP LUW management
 *
 * Revision 1.6  2006/01/05 13:59:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.5  2005/08/10 12:45:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected version-string
 *
 * Revision 1.4  2005/08/10 12:44:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * do close() if open() fails
 *
 * Revision 1.3  2004/07/19 09:45:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added getLogPrefix()
 *
 * Revision 1.2  2004/07/15 07:36:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.1  2004/07/06 07:09:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved SAP functionality to extensions
 *
 * Revision 1.2  2004/07/05 10:50:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected version setting
 *
 * Revision 1.1  2004/06/22 06:56:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * First version of SAP package
 *
 */
package nl.nn.adapterframework.extensions.sap.jco3;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;

import org.apache.commons.lang.StringUtils;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoFunction;

/**
 * Implementation of {@link ISender} that calls a SAP RFC-function.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.extensions.sap.SapSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSapSystemName(String) sapSystemName}</td><td>name of the {@link SapSystem} used by this object</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSapSystemNameParam(String) sapSystemNameParam}</td><td>name of the parameter used to indicate the name of the {@link SapSystem} used by this object if the attribute <code>sapSystemName</code> is empty</td><td>sapSystemName</td></tr>
 * <tr><td>{@link #setFunctionName(String) functionName}</td><td>Name of the RFC-function to be called in the SAP system</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFunctionNameParam(String) functionNameParam}</td><td>name of the parameter used to obtain the functionName from if the attribute <code>functionName</code> is empty</td><td>functionName</td></tr>
 * <tr><td>{@link #setSynchronous(boolean) synchronous}</td><td>when <code>false</code>, the sender operates in RR mode: the a reply is expected from SAP, and the sender does not participate in a transaction. When <code>false</code>, the sender operates in FF mode: no reply is expected from SAP, and the sender joins the transaction, that must be present. The SAP transaction is committed right after the XA transaction is completed.</td><td>true</td></tr>
 * <tr><td>{@link #setLuwHandleSessionKey(String) luwHandleSessionKey}</td><td>session key in which LUW information is stored. When set, actions that share a {@link SapLUWHandle LUW-handle} will be executed using the same destination. Can only be used for synchronous functions</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCorrelationIdFieldIndex(int) correlationIdFieldIndex}</td><td>Index of the field in the ImportParameterList of the RFC function that contains the correlationId</td><td>0</td></tr>
 * <tr><td>{@link #setCorrelationIdFieldName(String) correlationIdFieldName}</td><td>Name of the field in the ImportParameterList of the RFC function that contains the correlationId</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRequestFieldIndex(int) requestFieldIndex}</td><td>Index of the field in the ImportParameterList of the RFC function that contains the whole request message contents</td><td>0</td></tr>
 * <tr><td>{@link #setRequestFieldName(String) requestFieldName}</td><td>Name of the field in the ImportParameterList of the RFC function that contains the whole request message contents</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setReplyFieldIndex(int) replyFieldIndex}</td><td>Index of the field in the ExportParameterList of the RFC function that contains the whole reply message contents</td><td>0</td></tr>
 * <tr><td>{@link #setReplyFieldName(String) replyFieldName}</td><td>Name of the field in the ExportParameterList of the RFC function that contains the whole reply message contents</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
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
 * @author  Jaco de Groot
 * @since   5.0
 * @version Id
 */
public class SapSender extends SapSenderBase {
	public static final String version="$RCSfile: SapSender.java,v $  $Revision: 1.1 $ $Date: 2012-02-06 14:33:04 $";
	
	private String functionName=null;
	private String functionNameParam="functionName";

	public SapSender() {
		super();
		setSynchronous(true);
	}
	
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

	public void open() throws SenderException {
		super.open();
	}
	
	public void close() {
		super.close();
	}

	public JCoFunction getFunction(SapSystem sapSystem, ParameterValueList pvl) throws SapException {
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

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		String tid=null;
		try {
			ParameterValueList pvl = null;
			if (prc!=null) {
				pvl=prc.getValues(paramList);
			}
			SapSystem sapSystem = getSystem(pvl);
			
			JCoFunction function=getFunction(sapSystem, pvl);

			if (StringUtils.isEmpty(getSapSystemName())) {
				pvl.removeParameterValue(getSapSystemNameParam());
			}
			if (StringUtils.isEmpty(getFunctionName())) {
				pvl.removeParameterValue(getFunctionNameParam());
			}
			message2FunctionCall(function, message, correlationID, pvl);
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+" function call ["+functionCall2message(function)+"]");

			JCoDestination destination = getDestination(prc.getSession(), sapSystem);
			tid = getTid(destination,sapSystem);
			if (StringUtils.isEmpty(tid)) {
				function.execute(destination);
			} else {
				function.execute(destination,tid);
			}

			if (isSynchronous()) {
				return functionResult2message(function);
			} else {
				return tid;
			}
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	public void setSynchronous(boolean b) {
		super.setSynchronous(b);
	}


	public String getFunctionName() {
		return functionName;
	}
	public void setFunctionName(String string) {
		functionName = string;
	}


	public void setFunctionNameParam(String string) {
		functionNameParam = string;
	}
	public String getFunctionNameParam() {
		return functionNameParam;
	}

}
