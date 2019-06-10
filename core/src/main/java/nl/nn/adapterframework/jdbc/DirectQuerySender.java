/*
   Copyright 2013, 2016 Nationale-Nederlanden

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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDescription; 
import nl.nn.adapterframework.util.ClassUtils;


/** 
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
@IbisDescription(
	"QuerySender that interprets the input message as a query, possibly with attributes. \n" + 
	"Messages are expected to contain sql-text. \n" + 
	"<table border=\"1\"> \n" + 
	"<p><b>Parameters:</b> \n" + 
	"<tr><th>name</th><th>type</th><th>remarks</th></tr> \n" + 
	"<tr><td>&nbsp;</td><td>all parameters present are applied to the statement to be executed</td></tr> \n" + 
	"</table> \n" + 
	"</p> \n" 
)
public class DirectQuerySender extends JdbcQuerySenderBase {

	private boolean lockRows=false;
	private int lockWait=-1;
	
	public void configure() throws ConfigurationException {
		configure(false);
	}

	public void configure(boolean trust) throws ConfigurationException {
		super.configure();
		if (!trust) {
			ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
			String msg = "The class ["+getClass().getName()+"] is used one or more times. Please change to ["+FixedQuerySender.class.getName()+"] for better security";
			configWarnings.add(log, msg, true);
		}
	}

	protected PreparedStatement getStatement(Connection con, String correlationID, String message, boolean updateable) throws SQLException, JdbcException {
		String qry = message;
		if (lockRows) {
			qry = getDbmsSupport().prepareQueryTextForWorkQueueReading(-1, qry, lockWait);
		}
		return prepareQuery(con, qry, updateable);
	}

	@IbisDoc({"when set <code>true</code>, exclusive row-level locks are obtained on all the rows identified by the select statement (by appending ' for update nowait skip locked' to the end of the query)", "false"})
	public void setLockRows(boolean b) {
		lockRows = b;
	}

	public boolean isLockRows() {
		return lockRows;
	}

	@IbisDoc({"when set and >=0, ' for update wait #' is used instead of ' for update nowait skip locked'", "-1"})
	public void setLockWait(int i) {
		lockWait = i;
	}

	public int getLockWait() {
		return lockWait;
	}
}
