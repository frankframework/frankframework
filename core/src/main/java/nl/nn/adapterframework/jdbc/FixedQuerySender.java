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
package nl.nn.adapterframework.jdbc;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;

/**
 * QuerySender that assumes a fixed query, possibly with attributes.
 * 
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>&nbsp;</td><td>all parameters present are applied to the statement to be executed</td></tr>
 * </table>
 * </p>
 * 
 * <p><b>NOTE:</b> See {@link nl.nn.adapterframework.util.DB2XMLWriter DB2XMLWriter} for Resultset!</p>
 * 
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public class FixedQuerySender extends JdbcQuerySenderBase {

	private String query=null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getQuery())) {
			throw new ConfigurationException(getLogPrefix()+"query must be specified");
		}
	}
		
	
	@Override
	protected String getQuery(Message message) {
		return getQuery();
	}

	/**
	 * Sets the SQL-query text to be executed each time sendMessage() is called.
	 */
	@IbisDoc({"the sql query text to be excecuted each time sendmessage() is called", ""})
	public void setQuery(String query) {
		this.query = query;
	}
	public String getQuery() {
		return query;
	}

}
