/*
 * $Log: SapSender.java,v $
 * Revision 1.7  2007-05-01 14:22:43  europe\L190409
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
package nl.nn.adapterframework.extensions.sap;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

import org.apache.commons.lang.StringUtils;

import com.sap.mw.jco.JCO;

/**
 * Implementation of {@link ISender} that calls a SAP RFC-function.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.extensions.sap.SapListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>Name of the Sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFunctionName(String) functionName}</td><td>Name of the RFC-function to be called in the SAP system</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSapSystemName(String) sapSystemName}</td><td>name of the SapSystem used by this object</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCorrelationIdFieldIndex(int) correlationIdFieldIndex}</td><td>Index of the field in the ImportParameterList of the RFC function that contains the correlationId</td><td>0</td></tr>
 * <tr><td>{@link #setCorrelationIdFieldName(String) correlationIdFieldName}</td><td>Name of the field in the ImportParameterList of the RFC function that contains the correlationId</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRequestFieldIndex(int) requestFieldIndex}</td><td>Index of the field in the ImportParameterList of the RFC function that contains the whole request message contents</td><td>0</td></tr>
 * <tr><td>{@link #setRequestFieldName(String) requestFieldName}</td><td>Name of the field in the ImportParameterList of the RFC function that contains the whole request message contents</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setReplyFieldIndex(int) replyFieldIndex}</td><td>Index of the field in the ExportParameterList of the RFC function that contains the whole reply message contents</td><td>0</td></tr>
 * <tr><td>{@link #setReplyFieldName(String) replyFieldName}</td><td>Name of the field in the ExportParameterList of the RFC function that contains the whole reply message contents</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setLuwHandleSessionKey(String) luwHandleSessionKey}</td><td>session key in which LUW information is stored. When set, the action will be executed transacted.</td><td>&nbsp;</td></tr>
 * </table>
 * N.B. If no requestFieldIndex or requestFieldName is specified, input is converted from xml;
 * If no replyFieldIndex or replyFieldName is specified, output is converted to xml. 
 * </p>
 * @author Gerrit van Brakel
 * @since 4.2
 */
public class SapSender extends SapFunctionFacade implements ISenderWithParameters {
	public static final String version="$RCSfile: SapSender.java,v $  $Revision: 1.7 $ $Date: 2007-05-01 14:22:43 $";
	
	//TODO: allow functionName to be set dynamically from a parameter or from the message
	private String functionName=null;
	private String luwHandleSessionKey;

	protected ParameterList paramList = null;
	
	
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getFunctionName())) {
			throw new ConfigurationException(getLogPrefix()+"Function name is mandatory");
		}
	}

	public void open() throws SenderException {
		try {
			openFacade();
		} catch (SapException e) {
			close();
			throw new SenderException(getLogPrefix()+"exception starting SapSender", e);
		}
	}
	
	public void close() {
		closeFacade();
	}

	public String sendMessage(String correlationID, String message) throws SenderException, TimeOutException {
		return sendMessage(correlationID,message,null);
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		try {
			JCO.Function function = getFunctionTemplate().getFunction();

		    message2FunctionCall(function, message, correlationID);
		    if (StringUtils.isNotEmpty(getLuwHandleSessionKey())) {
				SapLUWHandle handle = SapLUWHandle.retrieveHandle(prc.getSession(),getLuwHandleSessionKey(),true,getSapSystem(),false);
				if (handle==null) {
					throw new SenderException("cannot find LUW handle from session key ["+getLuwHandleSessionKey()+"]");
				}
				JCO.Client client = handle.getClient();
				if (handle.isUseTid()) {
					client.execute(function,handle.getTid());
				} else {
					client.execute(function);
				}
 		    } else {
				JCO.Client client = null;
				try {
					client = getSapSystem().getClient();
					client.execute(function);
				} finally {
					
					if (client!=null) {
						getSapSystem().releaseClient(client);
					}
				}
 		    }
			return functionResult2message(function);
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	/**
	 * allways returns true.
	 */
	public boolean isSynchronous() {
		return true;
	}


	public String getFunctionName() {
		return functionName;
	}
	public void setFunctionName(String string) {
		functionName = string;
	}

	public void setLuwHandleSessionKey(String string) {
		luwHandleSessionKey = string;
	}
	public String getLuwHandleSessionKey() {
		return luwHandleSessionKey;
	}

	public void addParameter(Parameter p) {
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}



}
