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

import java.util.Iterator;
import java.util.List;

import com.sap.conn.jco.JCoParameterList;

/**
 * Handler for xml root element containing INPUT, OUTPUT and TABLES (parameter
 * lists).
 *
 * @author  Jaco de Groot
 * @since   5.0
 */
public class RootHandler extends Handler {

	private final List<JCoParameterList> parameterLists;
	private boolean parsedRequestRoot = false;

	public RootHandler(List<JCoParameterList> parameterLists) {
		super();
		this.parameterLists = parameterLists;
	}

	@Override
	protected void startElement(String localName) {
		if (!parsedRequestRoot) {
			parsedRequestRoot = true;
		} else  {
			Iterator<JCoParameterList> iterator = parameterLists.iterator();
			while (iterator.hasNext()) {
				JCoParameterList jcoParameterList = iterator.next();
				if (jcoParameterList.getMetaData().getName().equals(localName)) {
					childHandler = new ParameterListHandler(jcoParameterList);
				}
			}
			if (childHandler == null) {
				log.warn("parameter list '{}' does not exist", localName);
				unknownElementDepth = 1;
			}
		}
	}

	@Override
	protected void endElement(String localName) {
		finished(localName);
	}

}
