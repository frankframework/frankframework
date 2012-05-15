/*
 * $Log: StructureHandler.java,v $
 * Revision 1.1  2012-05-15 22:13:44  m00f069
 * Allow nesting of (different) types in SAP XML
 *
 */
package nl.nn.adapterframework.extensions.sap.jco3.handlers;

import com.sap.conn.jco.JCoStructure;

/**
 * Handler for structure xml elements like:
 * 
 * <I_HCDAK>
 *   <TIMESTAMP>20120419152332000003</TIMESTAMP>
 *   <SENDDATE>2012-04-19</SENDDATE>
 *   <ACCOUNTID>4403106</ACCOUNTID>
 *   TABLE|STRUCTURE
 * </I_HCDAK>
 * 
 * @author  Jaco de Groot
 * @since   5.0
 * @version Id
 */
public class StructureHandler extends RecordHandler {

	public StructureHandler(JCoStructure structure) {
		super(structure);
	}

}
