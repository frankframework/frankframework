/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.adapterframework.core;

import org.springframework.transaction.TransactionDefinition;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.doc.IbisDoc;

/**
 * The <code>HasTransactionAttribute</code> allows Pipes to declare transaction and isolation behavior.
 * The pipeline uses this to start a new transaction or suspend the current one when required.
 * 
 * @author  Gerrit van Brakel
 * @since   4.5
 */
public interface HasTransactionAttribute {

	@IbisDoc({"1", "The <code>transactionAttribute</code> declares transactional behavior of execution. It "
			+ "applies both to database transactions and XA transactions."
			+ "The pipeline uses this to start a new transaction or suspend the current one when required. "
			+ "For developers: it is equal"
			+ "to <a href=\"https://docs.oracle.com/javaee/7/tutorial/transactions003.htm\">EJB transaction attribute</a>. "
			+ "Possible values for transactionAttribute:"
			+ "  <table border=\"1\">"
			+ "    <tr><th>transactionAttribute</th><th>callers Transaction</th><th>Pipeline excecuted in Transaction</th></tr>"
			+ "    <tr><td colspan=\"1\" rowspan=\"2\">Required</td>    <td>none</td><td>T2</td></tr>"
			+ "											      <tr><td>T1</td>  <td>T1</td></tr>"
			+ "    <tr><td colspan=\"1\" rowspan=\"2\">RequiresNew</td> <td>none</td><td>T2</td></tr>"
			+ "											      <tr><td>T1</td>  <td>T2</td></tr>"
			+ "    <tr><td colspan=\"1\" rowspan=\"2\">Mandatory</td>   <td>none</td><td>error</td></tr>"
			+ "											      <tr><td>T1</td>  <td>T1</td></tr>"
			+ "    <tr><td colspan=\"1\" rowspan=\"2\">NotSupported</td><td>none</td><td>none</td></tr>"
			+ "											      <tr><td>T1</td>  <td>none</td></tr>"
			+ "    <tr><td colspan=\"1\" rowspan=\"2\">Supports</td>    <td>none</td><td>none</td></tr>"
			+ " 										      <tr><td>T1</td>  <td>T1</td></tr>"
			+ "    <tr><td colspan=\"1\" rowspan=\"2\">Never</td>       <td>none</td><td>none</td></tr>"
			+ "											      <tr><td>T1</td>  <td>error</td></tr>"
			+ "  </table>", "Supports"})
	public void setTransactionAttribute(String attribute) throws ConfigurationException;
	public String getTransactionAttribute();

	@IbisDoc({"2", "Like <code>transactionAttribute</code>, but the chosen "
			+ "option is represented with a number. The numbers mean:"
			+ "<table>"
			+ "<tr><td>0</td><td>Required</td></tr>"
			+ "<tr><td>1</td><td>Supports</td></tr>"
			+ "<tr><td>2</td><td>Mandatory</td></tr>"
			+ "<tr><td>3</td><td>RequiresNew</td></tr>"
			+ "<tr><td>4</td><td>NotSupported</td></tr>"
			+ "<tr><td>5</td><td>Never</td></tr>"
			+ "</table>", "1"})
	public void setTransactionAttributeNum(int i);
	public int getTransactionAttributeNum();

	@IbisDoc({"3", "Timeout (in seconds) of transaction started to process a message.", "<code>0</code> (use system default)"})
	public void setTransactionTimeout(int i);
	public int getTransactionTimeout();

	public TransactionDefinition getTxDef();
}
