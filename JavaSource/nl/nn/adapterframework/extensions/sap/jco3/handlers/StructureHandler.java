/*
 * $Log: StructureHandler.java,v $
 * Revision 1.2  2012-05-23 16:10:20  m00f069
 * Fixed javadoc
 *
 * Revision 1.1  2012/05/15 22:13:44  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Allow nesting of (different) types in SAP XML
 *
 */
package nl.nn.adapterframework.extensions.sap.jco3.handlers;

import com.sap.conn.jco.JCoStructure;

/**
 * Handler for structure xml elements like:
 * <pre>
 * &lt;I_HCDAK&gt;
 *   &lt;TIMESTAMP&gt;20120419152332000003&lt;/TIMESTAMP&gt;
 *   &lt;SENDDATE&gt;2012-04-19&lt;/SENDDATE&gt;
 *   &lt;ACCOUNTID&gt;4403106&lt;/ACCOUNTID&gt;
 *   TABLE|STRUCTURE
 * &lt;/I_HCDAK&gt;
 * </pre>
 * @author  Jaco de Groot
 * @since   5.0
 * @version Id
 */
public class StructureHandler extends RecordHandler {

	public StructureHandler(JCoStructure structure) {
		super(structure);
	}

}
