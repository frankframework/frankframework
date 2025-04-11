/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2021 WeAreFrank!

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
package org.frankframework.core;

import org.springframework.transaction.TransactionDefinition;

import org.frankframework.configuration.ConfigurationException;

/**
 * The <code>HasTransactionAttribute</code> allows Pipes to declare transaction and isolation behavior.
 * The pipeline uses this to start a new transaction or suspend the current one when required.
 *
 * @author  Gerrit van Brakel
 * @since   4.5
 */
public interface HasTransactionAttribute {

	/**
	 * The <code>transactionAttribute</code> declares transactional behavior of execution. It applies both to database transactions and XA transactions.
	 * The pipeline uses this to start a new transaction or suspend the current one when required.
	 * For developers: it is equal to <a href="https://docs.oracle.com/javaee/7/tutorial/transactions003.htm">EJB transaction attribute</a>.
	 * Possible values for transactionAttribute:
	 * <table border="1">
	 *     <tr><th>transactionAttribute</th><th>callers Transaction</th><th>Pipeline excecuted in Transaction</th></tr>
	 *     <tr><td colspan="1" rowspan="2">Required</td>    <td>none</td><td>T2</td></tr>
	 * 											      <tr><td>T1</td>  <td>T1</td></tr>
	 *     <tr><td colspan="1" rowspan="2">RequiresNew</td> <td>none</td><td>T2</td></tr>
	 * 											      <tr><td>T1</td>  <td>T2</td></tr>
	 *     <tr><td colspan="1" rowspan="2">Mandatory</td>   <td>none</td><td>error</td></tr>
	 * 											      <tr><td>T1</td>  <td>T1</td></tr>
	 *     <tr><td colspan="1" rowspan="2">NotSupported</td><td>none</td><td>none</td></tr>
	 * 											      <tr><td>T1</td>  <td>none</td></tr>
	 *     <tr><td colspan="1" rowspan="2">Supports</td>    <td>none</td><td>none</td></tr>
	 *  										      <tr><td>T1</td>  <td>T1</td></tr>
	 *     <tr><td colspan="1" rowspan="2">Never</td>       <td>none</td><td>none</td></tr>
	 * 											      <tr><td>T1</td>  <td>error</td></tr>
	 *   </table>
	 * @ff.default Supports
	 */
	void setTransactionAttribute(TransactionAttribute attribute) throws ConfigurationException;
	TransactionAttribute getTransactionAttribute();

	/**
	 * Timeout (in seconds) of transaction started to process a message.
	 * @ff.default <code>0</code> (use system default)
	 */ //TODO use Integer and set to NULL by default
	void setTransactionTimeout(int i);
	int getTransactionTimeout();

	TransactionDefinition getTxDef();
}
