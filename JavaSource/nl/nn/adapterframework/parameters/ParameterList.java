/*
 * $Log: ParameterList.java,v $
 * Revision 1.1  2004-10-05 09:52:25  L190409
 * moved parameter code  to package parameters
 *
 * Revision 1.1  2004/05/21 07:58:47  unknown <unknown@ibissource.org>
 * Moved PipeParameter to core
 *
 */
package nl.nn.adapterframework.parameters;

import java.util.ArrayList;

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
	
	public Parameter getParameter(int i) {
		return (Parameter)get(i);
	}
}
