/*
 * $Log: ParameterValueList.java,v $
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
 * List of parametervalues.
 * 
 * @author Gerrit van Brakel
 * @version Id
 */
public class ParameterValueList extends ArrayList {
	
	public ParameterValueList() {
		super();
	}

	public ParameterValueList(int i) {
		super(i);
	}
	
	public ParameterValue getParameterValue(int i) {
		return (ParameterValue)get(i);
	}
}
