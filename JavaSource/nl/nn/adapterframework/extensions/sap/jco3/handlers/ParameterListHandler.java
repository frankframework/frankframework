/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
/*
 * $Log: ParameterListHandler.java,v $
 * Revision 1.2  2012-05-23 16:10:20  m00f069
 * Fixed javadoc
 *
 * Revision 1.1  2012/05/15 22:13:44  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Allow nesting of (different) types in SAP XML
 *
 */
package nl.nn.adapterframework.extensions.sap.jco3.handlers;

import com.sap.conn.jco.JCoParameterList;

/**
 * Handler for parameter list xml elements like:
 * <pre>
 * &lt;INPUT&gt;
 *   &lt;CORRELATIONID123&lt;/CORRELATIONID&gt;
 *   &lt;EVENT&gt;&lt;/EVENT&gt;
 *   STRUCTURE|TABLE
 * &lt;/INPUT&gt;
 * </pre>
 * 
 * and:
 * 
 * <pre>
 * &lt;TABLES&gt;
 *   TABLE
 * &lt;/TABLES&gt;
 * </pre>
 * 
 * @author  Jaco de Groot
 * @since   5.0
 * @version $Id$
 */
public class ParameterListHandler extends RecordHandler {

	public ParameterListHandler(JCoParameterList parameterList) {
		super(parameterList);
	}

}
