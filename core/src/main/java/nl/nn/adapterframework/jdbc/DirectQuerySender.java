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

import java.io.IOException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.stream.Message;

/**
 * QuerySender that interprets the input message as a query, possibly with attributes.
 * Messages are expected to contain sql-text.
 *
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>&nbsp;</td><td>all parameters present are applied to the statement to be executed</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public class DirectQuerySender extends JdbcQuerySenderBase {

	@Override
	public void configure() throws ConfigurationException {
		configure(false);
	}

	public void configure(boolean trust) throws ConfigurationException {
		super.configure();
		if (!trust) {
			ConfigurationWarnings.add(log, "The class ["+getClass().getName()+"] is used one or more times. Please change to ["+FixedQuerySender.class.getName()+"] to avoid potential SQL injections!");
		}
	}

	@Override
	protected String getQuery(Message message) throws SenderException {
		try {
			return message.asString();
		} catch (IOException e) {
			throw new SenderException(e);
		}
	}

}
