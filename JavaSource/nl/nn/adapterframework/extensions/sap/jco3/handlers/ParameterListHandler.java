/*
 * $Log: ParameterListHandler.java,v $
 * Revision 1.1  2012-05-15 22:13:44  m00f069
 * Allow nesting of (different) types in SAP XML
 *
 */
package nl.nn.adapterframework.extensions.sap.jco3.handlers;

import com.sap.conn.jco.JCoParameterList;

/**
 * Handler for parameter list xml elements like:
 * 
 * <INPUT>
 *   <CORRELATIONID123</CORRELATIONID>
 *   <EVENT></EVENT>
 *   STRUCTURE|TABLE
 * </INPUT>
 * 
 * and:
 * 
 * <TABLES>
 *   TABLE
 * </TABLES>
 * 
 * @author  Jaco de Groot
 * @since   5.0
 * @version Id
 */
public class ParameterListHandler extends RecordHandler {

	public ParameterListHandler(JCoParameterList parameterList) {
		super(parameterList);
	}

}
