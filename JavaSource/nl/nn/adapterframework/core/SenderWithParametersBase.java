/*
 * $Log: SenderWithParametersBase.java,v $
 * Revision 1.2  2005-08-30 15:55:43  europe\L190409
 * added log and getLogPrefix()
 *
 * Revision 1.1  2005/06/20 08:58:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of SenderWithParametersBase
 *
 *
 */
package nl.nn.adapterframework.core;

import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;

/**
 * Provides a base class for senders with parameters.
 * 
 * @author Gerrit van Brakel
 * @since  4.3
 * @version Id
 */
public abstract class SenderWithParametersBase implements ISenderWithParameters {
	public static final String version="$RCSfile: SenderWithParametersBase.java,v $ $Revision: 1.2 $ $Date: 2005-08-30 15:55:43 $";
	protected Logger log = Logger.getLogger(this.getClass());
	
	private String name;
	protected ParameterList paramList = null;


	public void configure() throws ConfigurationException {
		if (paramList!=null) {
			paramList.configure();
		}
	}

	public void open() {
	}

	public void close() {
	}

	public String sendMessage(String correlationID, String message) throws SenderException, TimeOutException  {
		return sendMessage(correlationID,message,null);
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void addParameter(Parameter p) {
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}

	protected String getLogPrefix() {
		return "["+this.getClass().getName()+"] ["+getName()+"] ";
	}

}
