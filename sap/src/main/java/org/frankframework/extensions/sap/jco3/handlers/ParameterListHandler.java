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
package org.frankframework.extensions.sap.jco3.handlers;

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
 */
public class ParameterListHandler extends RecordHandler {

	public ParameterListHandler(JCoParameterList parameterList) {
		super(parameterList);
	}

}
