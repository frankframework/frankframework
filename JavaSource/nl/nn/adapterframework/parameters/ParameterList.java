/*
 * $Log: ParameterList.java,v $
 * Revision 1.2  2004-10-12 15:07:26  L190409
 * added configure()-method
 *
 * Revision 1.1  2004/10/05 09:52:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved parameter code  to package parameters
 *
 * Revision 1.1  2004/05/21 07:58:47  unknown <unknown@ibissource.org>
 * Moved PipeParameter to core
 *
 */
package nl.nn.adapterframework.parameters;

import java.util.ArrayList;

import nl.nn.adapterframework.configuration.ConfigurationException;


/**
 * List of parameters.
 * 
 * @author Gerrit van Brakel
 * @version Id
 */
public class ParameterList extends ArrayList {
	
	public ParameterList() {
		super();
	}

	public ParameterList(int i) {
		super(i);
	}
	
	public void configure() throws ConfigurationException {
		for (int i=0; i<size(); i++) {
			getParameter(i).configure();
		}
	}
	
	public Parameter getParameter(int i) {
		return (Parameter)get(i);
	}
}
