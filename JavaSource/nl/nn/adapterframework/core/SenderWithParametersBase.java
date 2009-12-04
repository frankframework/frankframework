/*
 * $Log: SenderWithParametersBase.java,v $
 * Revision 1.5  2009-12-04 18:23:34  m00f069
 * Added ibisDebugger.senderAbort and ibisDebugger.pipeRollback
 *
 * Revision 1.4  2007/02/26 16:53:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * add throws clause to open and close
 *
 * Revision 1.3  2007/02/12 13:44:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.2  2005/08/30 15:55:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added log and getLogPrefix()
 *
 * Revision 1.1  2005/06/20 08:58:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of SenderWithParametersBase
 *
 *
 */
package nl.nn.adapterframework.core;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.senders.SenderBase;

/**
 * Provides a base class for senders with parameters.
 * 
 * @author Gerrit van Brakel
 * @since  4.3
 * @version Id
 */
public abstract class SenderWithParametersBase extends SenderBase implements ISenderWithParameters {
	public static final String version="$RCSfile: SenderWithParametersBase.java,v $ $Revision: 1.5 $ $Date: 2009-12-04 18:23:34 $";
	
	private String name;
	protected ParameterList paramList = null;

	public void configure() throws ConfigurationException {
		if (paramList!=null) {
			paramList.configure();
		}
	}

	public String sendMessage(String correlationID, String message) throws SenderException, TimeOutException  {
		return sendMessage(correlationID,message,null);
	}

	public void addParameter(Parameter p) {
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}

}
