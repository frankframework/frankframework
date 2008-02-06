/*
 * $Log: HasTransactionAttribute.java,v $
 * Revision 1.3  2008-02-06 15:48:47  europe\L190409
 * added support for setting of transaction timeout
 *
 * Revision 1.2  2006/08/21 15:06:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected version string
 *
 * Revision 1.1  2006/08/21 15:05:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of transaction attribute handling
 *
 */
package nl.nn.adapterframework.core;

/**
 * The <code>HasTransactionAttribute</code> is allows Pipes to declare that they have a transactionAttribute.
 * The pipeline uses this to start a new transaction or suspend the current one when required.
 * Equal to <A href="http://java.sun.com/j2ee/sdk_1.2.1/techdocs/guides/ejb/html/Transaction2.html#10494">EJB transaction attribute</a>. 
 * Possible values for transactionAttribute:
 *   <table border="1">
 *   <tr><th>transactionAttribute</th><th>callers Transaction</th><th>Pipeline excecuted in Transaction</th></tr>
 *   <tr><td colspan="1" rowspan="2">Required</td>    <td>none</td><td>T2</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">RequiresNew</td> <td>none</td><td>T2</td></tr>
 * 											      <tr><td>T1</td>  <td>T2</td></tr>
 *   <tr><td colspan="1" rowspan="2">Mandatory</td>   <td>none</td><td>error</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">NotSupported</td><td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>none</td></tr>
 *   <tr><td colspan="1" rowspan="2">Supports</td>    <td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">Never</td>       <td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>error</td></tr>
 *  </table> 
 * 
 * @version HasSender.java,v 1.3 2004/03/23 16:42:59 L190409 Exp $
 * @author  Gerrit van Brakel
 * @since   4.5
 */
public interface HasTransactionAttribute {
	public static final String version = "$RCSfile: HasTransactionAttribute.java,v $ $Revision: 1.3 $ $Date: 2008-02-06 15:48:47 $";

	public String getTransactionAttribute();
	public int getTransactionAttributeNum();

	public int getTransactionTimeout();
}
