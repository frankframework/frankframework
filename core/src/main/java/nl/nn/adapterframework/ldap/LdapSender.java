/*
   Copyright 2013, 2020 Nationale-Nederlanden

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
package nl.nn.adapterframework.ldap;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.digester.Digester;
import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.jms.JNDIBase;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Sender to obtain information from and write to an LDAP Directory.
 * Returns the set of attributes in an XML format. Examples are shown below.
 * 
 * 
 * 
 * <p>
 * <b>Parameters:</b>
 * <table border="1">
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>entryName</td><td>Represents entryName (RDN) of interest.</td></tr>
 * <tr><td>filterExpression</td><td>Filter expression (handy with searching - see RFC2254).</td></tr>
 * <tr><td>principal</td><td>Will overwrite jndiAuthAlias, principal and credential attributes together with parameter credentials which is expected to be present too. This will also have the effect of usePooling being set to false and the LDAP connection being made at runtime only (skipped at configuration time).</td></tr>
 * <tr><td>credentials</td><td>See parameter principal. It's advised to set attribute hidden to true for parameter credentials.</td></tr>
 * </table>
 * </p>
 * 
 * <p>
 * current requirements for input and configuration
 * <table border="1">
 * <tr><th>operation</th><th>requirements</th></tr>
 * <tr><td>read</td><td>
 * <ul>
 * 	  <li>parameter 'entryName', resolving to RDN of entry to read</li>
 * 	  <li>optional xml-inputmessage containing attributes to be returned</li>
 * </ul>
 * </td></tr>
 * <tr><td>create</td><td>
 * <ul>
 * 	  <li>parameter 'entryName', resolving to RDN of entry to create</li>
 * 	  <li>xml-inputmessage containing attributes to create</li>
 * </ul>
 * </td></tr>
 * <tr><td>update</td><td>
 * <ul>
 * 	  <li>parameter 'entryName', resolving to RDN of entry to update</li>
 * 	  <li>xml-inputmessage containing attributes to update</li>
 * 	  <li>optional parameter 'newEntryName', new RDN of entry</li>
 * </ul>
 * </td></tr>
 * <tr><td>delete</td><td>
 * <ul>
 * 	  <li>parameter 'entryName', resolving to RDN of entry to delete</li>
 * 	  <li>when manipulationSubject is set to attribute: xml-inputmessage containing attributes to be deleted</li>
 * </ul>
 * </td></tr>
 * <tr><td>getTree</td><td>
 * <ul>
 * 	  <li>parameter 'entryName', resolving to RDN of entry that is root of tree to read</li>
 * 	  <li>no specific inputmessage required</li>
 * </ul>
 * </td></tr>
 * <tr><td>search</td><td>
 * <ul>
 * 	  <li>parameter 'entryName', resolving to RDN of entry to read</li>
 *    <li>parameter 'filterExpression', specifying the entries searched for</li>
 * 	  <li>optional attribute 'attributesReturned' containing attributes to be returned</li>
 * </ul>
 * </td></tr>
 * <tr><td>deepSearch</td><td>
 * <ul>
 * 	  <li>parameter 'entryName', resolving to RDN of entry to read</li>
 *    <li>parameter 'filterExpression', specifying the entries searched for</li>
 * 	  <li>optional attribute 'attributesReturned' containing attributes to be returned</li>
 * </ul>
 * </td></tr>
 * <tr><td>getSubcontexts</td><td>
 * <ul>
 * 	  <li>parameter 'entryName', resolving to RDN of entry to read</li>
 * 	  <li>optional attribute 'attributesReturned' containing attributes to be returned</li>
 * </ul>
 * </td></tr>
 * <tr><td>getTree</td><td>
 * <ul>
 * 	  <li>parameter 'entryName', resolving to RDN of entry to read</li>
 * 	  <li>optional attribute 'attributesReturned' containing attributes to be returned</li>
 * </ul>
 * </td></tr>
 * <tr><td>challenge</td><td>
 * <ul>
 * 	  <li>parameter 'principal', resolving to RDN of user who's password should be verified</li>
 * 	  <li>parameter 'credentials', password to verify</li>
 * </ul>
 * </td></tr>
 * <tr><td>changeUnicodePwd</td><td>
 * <ul>
 * 	  <li>parameter 'entryName', resolving to RDN of user who's password should be changed</li>
 * 	  <li>parameter 'oldPassword', current password, will be encoded as required by Active Directory (a UTF-16 encoded Unicode string containing the password surrounded by quotation marks) before sending it to the LDAP server. It's advised to set attribute hidden to true for parameter.</li>
 * 	  <li>parameter 'newPassword', new password, will be encoded as required by Active Directory (a UTF-16 encoded Unicode string containing the password surrounded by quotation marks) before sending it to the LDAP server. It's advised to set attribute hidden to true for parameter.</li>
 * </ul>
 * </td></tr>
 * </table>
 * </p>
 * 
 * <h2>example</h2>
 * Consider the following configuration example:
 * <code>
 * <pre>
 *   &lt;sender
 *        className="nl.nn.adapterframework.ldap.LdapSender"
 *        ldapProviderURL="ldap://servername:389/o=ing"
 *        operation="read"
 *        attributesToReturn="givenName,sn,telephoneNumber" &gt;
 *     <&ltparam name="entryName" xpathExpression="entryName" /&gt;
 *   &lt;/sender&gt;
 * </pre>
 * </code>
 * <br/>
 * 
 * This may result in the following output:
 * <code><pre>
 * &lt;ldap&gt;
 *	&lt;entryName&gt;uid=srp,ou=people&lt;/entryName&gt;
 *
 *	&lt;attributes&gt;
 *		&lt;attribute attrID="givenName"&gt;
 *			&lt;value&gt;Jan&lt;/value&gt;
 *		&lt;/attribute&gt;
 *
 *		&lt;attribute attrID="telephoneNumber"&gt;
 *			&lt;value&gt;010 5131123&lt;/value&gt;
 *			&lt;value&gt;06 23456064&lt;/value&gt;
 *		&lt;/attribute&gt;
 *
 *		&lt;attribute attrID="sn"&gt;
 *			&lt;value&gt;Jansen&lt;/value&gt;
 *		&lt;/attribute&gt;
 *	&lt;/attributes&gt;
 * &lt;/ldap&gt;
 *  </pre></code> <br/>
 * 
 * Search or Read?
 * 
 * Read retrieves all the attributes of the specified entry.
 * 
 * Search retrieves all the entries of the specified (by entryName) context that have the specified attributes,
 * together with the attributes. If the specified attributes are null or empty all the attributes of all the entries within the 
 * specified context are returned.
 *  
 * Sample result of a <code>read</code> operation:<br/><code><pre>
 *	&lt;attributes&gt;
 *	    &lt;attribute&gt;
 *	    &lt;attribute name="employeeType" value="Extern"/&gt;
 *	    &lt;attribute name="roomNumber" value="DP 2.13.025"/&gt;
 *	    &lt;attribute name="departmentCode" value="358000"/&gt;
 *	    &lt;attribute name="organizationalHierarchy"&gt;
 *	        &lt;item value="ou=ING-EUR,ou=Group,ou=Organization,o=ing"/&gt;
 *	        &lt;item value="ou=OPS&amp;IT,ou=NL,ou=ING-EUR,ou=Group,ou=Organization,o=ing"/&gt;
 *	        &lt;item value="ou=000001,ou=OPS&amp;IT,ou=NL,ou=ING-EUR,ou=Group,ou=Organization,o=ing"/&gt;
 *	    &lt;/attribute>
 *	    &lt;attribute name="givenName" value="Gerrit"/>
 *	&lt;/attributes&gt;
 * 
 * </pre></code> <br/>
 * Sample result of a <code>search</code> operation:<br/><code><pre>
 *	&lt;entries&gt;
 *	 &lt;entry name="uid=srp"&gt;
 *	   &lt;attributes&gt;
 *	    &lt;attribute&gt;
 *	    &lt;attribute name="employeeType" value="Extern"/&gt;
 *	    &lt;attribute name="roomNumber" value="DP 2.13.025"/&gt;
 *	    &lt;attribute name="departmentCode" value="358000"/&gt;
 *	    &lt;attribute name="organizationalHierarchy"&gt;
 *	        &lt;item value="ou=ING-EUR,ou=Group,ou=Organization,o=ing"/&gt;
 *	        &lt;item value="ou=OPS&amp;IT,ou=NL,ou=ING-EUR,ou=Group,ou=Organization,o=ing"/&gt;
 *	        &lt;item value="ou=000001,ou=OPS&amp;IT,ou=NL,ou=ING-EUR,ou=Group,ou=Organization,o=ing"/&gt;
 *	    &lt;/attribute>
 *	    &lt;attribute name="givenName" value="Gerrit"/>
 *	   &lt;/attributes&gt;
 *	  &lt;/entry&gt;
 *   &lt;entry&gt; .... &lt;/entry&gt;
 *   .....
 *	&lt;/entries&gt;
 * </pre></code> <br/>
 *
 * <h2>upgrading from earlier versions (pre 4.6)</h2>
 * <ul>
 *   <li>In earlier versions, the entryName was taken from the first parameter. To upgrade, call your first parameter 'entryName'</li> 
 *   <li>In earlier versions, the filterExpression was taken from the first parameter. To upgrade, call your second parameter 'filterExpression'</li> 
 * </ul>
 *  
 * @author Gerrit van Brakel
 * @author Jaco de Groot
 */
public class LdapSender extends JNDIBase implements ISenderWithParameters {

	private String FILTER = "filterExpression";
	private String ENTRYNAME = "entryName";

	private int searchTimeout=20000;

	private static final String INITIAL_CONTEXT_FACTORY ="com.sun.jndi.ldap.LdapCtxFactory";

	public static final String LDAP_ERROR_MAGIC_STRING="[LDAP: error code";

	public static final String OPERATION_READ   = "read";
	public static final String OPERATION_CREATE = "create";
	public static final String OPERATION_UPDATE = "update";
	public static final String OPERATION_DELETE = "delete";
	public static final String OPERATION_SEARCH = "search";
	public static final String OPERATION_DEEP_SEARCH = "deepSearch";
	public static final String OPERATION_SUB_CONTEXTS = "getSubContexts";
	public static final String OPERATION_GET_TREE = "getTree";
	public static final String OPERATION_CHALLENGE = "challenge";
	public static final String OPERATION_CHANGE_UNICODE_PWD = "changeUnicodePwd";

	public static final String MANIPULATION_ENTRY = "entry";
	public static final String MANIPULATION_ATTRIBUTE = "attribute";

	//The results to return if the modifying operation succeeds (an XML, to make it "next pipe ready")  
	private static final String DEFAULT_RESULT = "<LdapResult>Success</LdapResult>";
	private static final String DEFAULT_RESULT_READ = "<LdapResult>No such object</LdapResult>";
	private static final String DEFAULT_RESULT_SEARCH = "<LdapResult>Object not found</LdapResult>";
			
	private static final String DEFAULT_RESULT_DELETE = "<LdapResult>Delete Success - Never Existed</LdapResult>";
	private static final String DEFAULT_RESULT_CREATE_OK = "<LdapResult>Create Success - Already There</LdapResult>";
	private static final String DEFAULT_RESULT_CREATE_NOK = "<LdapResult>Create FAILED - Entry with given name already exists</LdapResult>";
	private static final String DEFAULT_RESULT_UPDATE_NOK = "<LdapResult>Update FAILED</LdapResult>";
	private static final String DEFAULT_RESULT_CHALLENGE_OK = DEFAULT_RESULT;
	private static final String DEFAULT_RESULT_CHALLENGE_NOK = "<LdapResult>Challenge FAILED - Invalid credentials</LdapResult>";
	private static final String DEFAULT_RESULT_CHANGE_UNICODE_PWD_OK = DEFAULT_RESULT;
	private static final String DEFAULT_RESULT_CHANGE_UNICODE_PWD_NOK = "<LdapResult>Change unicodePwd FAILED - Invalid old and/or new password</LdapResult>";

	private String name;
	private String operation = OPERATION_READ;
	private String manipulationSubject = MANIPULATION_ATTRIBUTE;
	private String ldapProviderURL;
	private String attributesToReturn;
	private boolean usePooling=true;

	private String errorSessionKey="errorReason";
	private int maxEntriesReturned=0;
	private boolean unicodePwd = false;
	private boolean replyNotFound = false;

	protected ParameterList paramList = null;
	private boolean principalParameterFound = false;
	private Hashtable jndiEnv=null;

	public LdapSender() {
		super();
		setInitialContextFactoryName(INITIAL_CONTEXT_FACTORY);
	}

	@Override
	public void configure() throws ConfigurationException {
		if (paramList == null ||
				(paramList.findParameter(ENTRYNAME) == null && !getOperation().equals(OPERATION_CHALLENGE))) {
			throw new ConfigurationException("[" + getName()+ "] Required parameter with the name [entryName] not found!");
		}
		paramList.configure();

		if (getOperation() == null
			|| !(getOperation().equals(OPERATION_READ)
				|| getOperation().equals(OPERATION_SEARCH)
				|| getOperation().equals(OPERATION_DEEP_SEARCH)
				|| getOperation().equals(OPERATION_CREATE)
				|| getOperation().equals(OPERATION_UPDATE)
				|| getOperation().equals(OPERATION_DELETE)
				|| getOperation().equals(OPERATION_SUB_CONTEXTS)
				|| getOperation().equals(OPERATION_GET_TREE)
				|| getOperation().equals(OPERATION_CHALLENGE)
				|| getOperation().equals(OPERATION_CHANGE_UNICODE_PWD))) {
			throw new ConfigurationException("attribute opereration ["	+ getOperation()
												+ "] must be one of ("
												+ OPERATION_READ+ ","
												+ OPERATION_CREATE+ ","
												+ OPERATION_UPDATE+ ","
												+ OPERATION_DELETE+ ","
												+ OPERATION_SEARCH+ ","
												+ OPERATION_DEEP_SEARCH+ ","
												+ OPERATION_SUB_CONTEXTS+ ","
												+ OPERATION_GET_TREE+ ","
												+ OPERATION_CHALLENGE+ ","
												+ OPERATION_CHANGE_UNICODE_PWD+ ")");
		}
		if (getOperation().equals(OPERATION_CREATE)
			|| getOperation().equals(OPERATION_DELETE)) {
			if (!(getManipulationSubject().equals(MANIPULATION_ENTRY)
				|| getManipulationSubject().equals(MANIPULATION_ATTRIBUTE)))
				throw new ConfigurationException("["+ getClass().getName()	+ "] manipulationSubject invalid (must be one of ["
						+ MANIPULATION_ATTRIBUTE+ ", "+ MANIPULATION_ENTRY	+ "])");
		}
		if (getOperation().equals(OPERATION_UPDATE)) {
			if (!(getManipulationSubject().equals(MANIPULATION_ATTRIBUTE)))
				throw new ConfigurationException("["+ getClass().getName()	+ "] manipulationSubject invalid for update operation (must be ['"
						+ MANIPULATION_ATTRIBUTE	+ "'], which is default - remove from <pipe>)");
		}
		if (getOperation().equals(OPERATION_CHALLENGE)) {
			if (paramList.findParameter("principal") == null)
				throw new ConfigurationException("principal should be specified using a parameter when using operation challenge");
		}
		Parameter credentials = paramList.findParameter("credentials");
		if (credentials != null && !credentials.isHidden()) {
			ConfigurationWarnings.add(this, log, "It's advised to set attribute hidden to true for parameter credentials.");
		}
		Parameter oldPassword = paramList.findParameter("oldPassword");
		if (oldPassword != null && !oldPassword.isHidden()) {
			ConfigurationWarnings.add(this, log, "It's advised to set attribute hidden to true for parameter oldPassword.");
		}
		Parameter newPassword = paramList.findParameter("newPassword");
		if (newPassword != null && !newPassword.isHidden()) {
			ConfigurationWarnings.add(this, log, "It's advised to set attribute hidden to true for parameter newPassword.");
		}
		if (paramList.findParameter("principal") != null) {
			if (paramList.findParameter("credentials") == null) {
				throw new ConfigurationException("principal set as parameter, but no credentials parameter found");
			}
			principalParameterFound = true;
			setUsePooling(false);
		} else {
			DirContext dirContext=null;
			try {
				dirContext = getDirContext(null);
			} catch (Exception e) {
				throw new ConfigurationException("["+ getClass().getName() + "] Context could not be found ", e);
			} finally {
				closeDirContext(dirContext);
			}
		}
	}

	public void storeLdapException(Throwable t, IPipeLineSession session) {
		if (StringUtils.isNotEmpty(getErrorSessionKey()) && session!=null && t!=null) {
			XmlBuilder ldapError=new XmlBuilder("ldapError");
			ldapError.addAttribute("class",ClassUtils.nameOf(t));
			String message=t.getMessage();
			int magicPos=message.indexOf(LDAP_ERROR_MAGIC_STRING);
			if (magicPos>=0) {
				int dashPos=message.indexOf('-',magicPos);
				if (dashPos>magicPos) {
					String codeString = message.substring(magicPos+LDAP_ERROR_MAGIC_STRING.length(),dashPos).trim();
					ldapError.addAttribute("code",codeString);
					int bracketPos=message.indexOf(']',dashPos);
					if (bracketPos>dashPos) {
						String description=message.substring(dashPos+1,bracketPos).trim();
						ldapError.addAttribute("description",description);
						String msgPart1=message.substring(0,magicPos).trim();
						String msgPart2=message.substring(bracketPos+1).trim();
						if (msgPart1.endsWith(":")) {
							msgPart1=msgPart1.substring(0,msgPart1.length()-1).trim();
						}
						if (msgPart2.startsWith(";")) {
							msgPart2=msgPart2.substring(1).trim();
						}
						message=(msgPart1+" "+msgPart2).trim();
 					}
				}
			}
			ldapError.setValue(message);
			String reasonXml=ldapError.toXML();
			if (log.isDebugEnabled()) { log.debug("sessionKey ["+getErrorSessionKey()+"] loaded with error message ["+reasonXml+"]"); }
			session.put(getErrorSessionKey(),reasonXml);
		}
		log.debug("exit storeLdapException");
	}

	/**
	 * Makes an String array attrIds from the comma separated parameter attributesToReturn
	 * attrIds is used as an argument to the function getAttributes(context, attrIds) when only
	 * specific attributes are required - 
	 */
	private String[] getAttributesReturnedParameter() {
		//since 1.4: return attributesToReturn == null ? null : attributesToReturn.split(",");
		//since 1.3 below:
		return getAttributesToReturn() == null ? null : splitCommaSeparatedString(getAttributesToReturn());
	}

	private String[] splitCommaSeparatedString(String toSeparate) {

		if(toSeparate == null || toSeparate == "") return null;		
		
		List list = new ArrayList();
		String[] strArr = new String[1]; //just do determine the type of the array in list.toArray(Object[] o)
		
		StringBuffer sb = new StringBuffer(toSeparate);
		for (int i = 0; i < sb.length(); i++) {
			if(sb.charAt(i) == ' ')
				sb.deleteCharAt(i);
		}
		int start = 0;
		for (int i = 0; i < sb.length(); i++) {
			if(sb.charAt(i) == ',' || i == sb.length()-1)
			{
				list.add(sb.substring(start, i == sb.length()-1 ? i+1 : i));
				start = i+1;
			}
		}
		
		Object[] objArr = null;
		objArr = list.toArray(strArr);
		return (String[])objArr;
	}




	@Override
	public void open() throws SenderException {
	}


	@Override
	public boolean isSynchronous() {
		return true;
	}


	private String performOperationRead(String entryName, IPipeLineSession session, Map paramValueMap) throws SenderException, ParameterException {
		DirContext dirContext = null;
		try{
			dirContext = getDirContext(paramValueMap);
			return attributesToXml(dirContext.getAttributes(entryName, getAttributesReturnedParameter())).toXML();
		} catch(NamingException e) {
			// https://wiki.servicenow.com/index.php?title=LDAP_Error_Codes:
			//   32 LDAP_NO_SUCH_OBJECT Indicates the target object cannot be found. This code is not returned on following operations: Search operations that find the search base but cannot find any entries that match the search filter. Bind operations. 
			// Sun:
			//   [LDAP: error code 32 - No Such Object...
			if(e.getMessage().startsWith("[LDAP: error code 32 - ") ) {
				if (log.isDebugEnabled()) log.debug("Operation [" + getOperation()+ "] found nothing - no such entryName: " + entryName);
				return DEFAULT_RESULT_READ;	
			} else {
				storeLdapException(e, session);
				throw new SenderException("Exception in operation [" + getOperation()+ "] entryName=["+entryName+"]", e);	
			}
		} finally {
			closeDirContext(dirContext);
		}
	}

	private String performOperationUpdate(String entryName, IPipeLineSession session, Map paramValueMap, Attributes attrs) throws SenderException, ParameterException {
		String entryNameAfter = entryName;
		if (paramValueMap != null){
			String newEntryName = (String)paramValueMap.get("newEntryName");
			if (newEntryName != null && StringUtils.isNotEmpty(newEntryName)) {
				if (log.isDebugEnabled()) log.debug("newEntryName=["+newEntryName+"]");
				DirContext dirContext = null;
				try{
					dirContext = getDirContext(paramValueMap);
					dirContext.rename(entryName, newEntryName);
					entryNameAfter = newEntryName;
				} catch(NamingException e) {
					String msg;
					// https://wiki.servicenow.com/index.php?title=LDAP_Error_Codes:
					//   32 LDAP_NO_SUCH_OBJECT Indicates the target object cannot be found. This code is not returned on following operations: Search operations that find the search base but cannot find any entries that match the search filter. Bind operations. 
					// Sun:
					//   [LDAP: error code 32 - No Such Object...
					if (e.getMessage().startsWith("[LDAP: error code 32 - ")) {
						msg="Operation [" + getOperation()+ "] failed - wrong entryName ["+ entryName+"]";	
					} else {
						msg="Exception in operation [" + getOperation()+ "] entryName ["+entryName+"]";									
					}
					storeLdapException(e, session);
					throw new SenderException(msg,e);
				} finally {
					closeDirContext(dirContext);
				}
			}
		}
		
		if (manipulationSubject.equals(MANIPULATION_ATTRIBUTE)) {
			if (attrs == null && !entryNameAfter.equals(entryName)) {
				// it should be possible to only 'rename' the entry (without attribute change) 
				return DEFAULT_RESULT;
			}
			NamingEnumeration na = attrs.getAll();
			while(na.hasMoreElements()) {
				Attribute a = (Attribute)na.nextElement();
				log.debug("Update attribute: " + a.getID());
				NamingEnumeration values;
				try {
					values = a.getAll();
				} catch (NamingException e1) {
					storeLdapException(e1, session);
					throw new SenderException("cannot obtain values of Attribute ["+a.getID()+"]",e1);
				}
				while(values.hasMoreElements()) {
					Attributes partialAttrs = new BasicAttributes();
					Attribute singleValuedAttribute;
					String id = a.getID();
					Object value = values.nextElement();
					if (log.isDebugEnabled()) {
						if (id.toLowerCase().contains("password") || id.toLowerCase().contains("pwd")) {
							log.debug("Update value: ***");
						} else {
							log.debug("Update value: " + value);
						}
					}
					if (unicodePwd && "unicodePwd".equalsIgnoreCase(id)) {
						singleValuedAttribute = new BasicAttribute(id, encodeUnicodePwd(value));
					} else {
						singleValuedAttribute = new BasicAttribute(id, value);
					}
					partialAttrs.put(singleValuedAttribute);
					DirContext dirContext = null;
					try {
						dirContext = getDirContext(paramValueMap);
						dirContext.modifyAttributes(entryNameAfter,	DirContext.REPLACE_ATTRIBUTE, partialAttrs);
					} catch(NamingException e) {
						String msg;
						// https://wiki.servicenow.com/index.php?title=LDAP_Error_Codes:
						//   32 LDAP_NO_SUCH_OBJECT Indicates the target object cannot be found. This code is not returned on following operations: Search operations that find the search base but cannot find any entries that match the search filter. Bind operations. 
						// Sun:
						//   [LDAP: error code 32 - No Such Object...
						if (e.getMessage().startsWith("[LDAP: error code 32 - ")) {
							msg="Operation [" + getOperation()+ "] failed - wrong entryName ["+ entryNameAfter+"]";	
						} else {
							msg="Exception in operation [" + getOperation()+ "] entryName ["+entryNameAfter+"]";									
						}
						//result = DEFAULT_RESULT_UPDATE_NOK;
						storeLdapException(e, session);
						throw new SenderException(msg,e);
					} finally {
						closeDirContext(dirContext);
					}
				}
			}
			return DEFAULT_RESULT;
		} else {
			DirContext dirContext = null;
			try {
				dirContext = getDirContext(paramValueMap);
				//dirContext.rename(newEntryName, oldEntryName);
				//result = DEFAULT_RESULT;
				dirContext.rename(entryName, entryName);
				return "<LdapResult>Deze functionaliteit is nog niet beschikbaar - naam niet veranderd.</LdapResult>";
			} catch (NamingException e) {
				// https://wiki.servicenow.com/index.php?title=LDAP_Error_Codes:
				//   68 LDAP_ALREADY_EXISTS Indicates that the add operation attempted to add an entry that already exists, or that the modify operation attempted to rename an entry to the name of an entry that already exists.
				// Sun:
				//   [LDAP: error code 68 - Entry Already Exists]
				if(!e.getMessage().startsWith("[LDAP: error code 68 - ")) {
					storeLdapException(e, session);
					throw new SenderException(e);
				}
				return DEFAULT_RESULT_CREATE_NOK;
			} finally {
				closeDirContext(dirContext);
			}
		}
	}

	private String performOperationCreate(String entryName, IPipeLineSession session, Map paramValueMap, Attributes attrs) throws SenderException, ParameterException {
		if (manipulationSubject.equals(MANIPULATION_ATTRIBUTE)) {
			String result=null;
			NamingEnumeration na = attrs.getAll();
			while(na.hasMoreElements()) {
				Attribute a = (Attribute)na.nextElement();
				log.debug("Create attribute: " + a.getID());
				NamingEnumeration values;
				try {
					values = a.getAll();
				} catch (NamingException e1) {
					storeLdapException(e1, session);
					throw new SenderException("cannot obtain values of Attribute ["+a.getID()+"]",e1);
				}
				while(values.hasMoreElements()) {
					Attributes partialAttrs = new BasicAttributes();
					Attribute singleValuedAttribute;
					String id = a.getID();
					Object value = values.nextElement();
					if (log.isDebugEnabled()) {
						if (id.toLowerCase().contains("password") || id.toLowerCase().contains("pwd")) {
							log.debug("Create value: ***");
						} else {
							log.debug("Create value: " + value);
						}
					}
					if (unicodePwd && "unicodePwd".equalsIgnoreCase(id)) {
						singleValuedAttribute = new BasicAttribute(id, encodeUnicodePwd(value));
					} else {
						singleValuedAttribute = new BasicAttribute(id, value);
					}
					partialAttrs.put(singleValuedAttribute);
					DirContext dirContext = null;
					try {
						dirContext = getDirContext(paramValueMap);
						dirContext.modifyAttributes(entryName, DirContext.ADD_ATTRIBUTE, partialAttrs);
					} catch(NamingException e){
						// https://wiki.servicenow.com/index.php?title=LDAP_Error_Codes:
						//   20 LDAP_TYPE_OR_VALUE_EXISTS Indicates that the attribute value specified in a modify or add operation already exists as a value for that attribute.
						// Sun:
						//   [LDAP: error code 20 - Attribute Or Value Exists]
						if (e.getMessage().startsWith("[LDAP: error code 20 - ")) {
							if (log.isDebugEnabled()) log.debug("Operation [" + getOperation()+ "] successful: " + e.getMessage());	
							result = DEFAULT_RESULT_CREATE_OK;
						} else {
							storeLdapException(e, session);
							throw new SenderException("Exception in operation [" + getOperation()+ "] entryName ["+entryName+"]", e );
						}
					} finally {
						closeDirContext(dirContext);
					}
				}
			}
			if (result!=null) {
				return result;
			} 
			return DEFAULT_RESULT;
		} else {
			DirContext dirContext = null;
			try {
				if (unicodePwd) {
					Enumeration enumeration = attrs.getIDs();
					while (enumeration.hasMoreElements()) {
						String id = (String)enumeration.nextElement();
						if ("unicodePwd".equalsIgnoreCase(id)) {
							Attribute attr = attrs.get(id);
							for (int i = 0; i < attr.size(); i++) {
								attr.set(i, encodeUnicodePwd(attr.get(i)));
							}
						} 
					}
				}
				dirContext = getDirContext(paramValueMap);
				dirContext.bind(entryName, null, attrs);
				return DEFAULT_RESULT;
			} catch (NamingException e) {
				// if (log.isDebugEnabled()) log.debug("Exception in operation [" + getOperation()+ "] entryName ["+entryName+"]", e);
				if (log.isDebugEnabled()) log.debug("Exception in operation [" + getOperation()+ "] entryName ["+entryName+"]: "+ e.getMessage());
				// https://wiki.servicenow.com/index.php?title=LDAP_Error_Codes:
				//   68 LDAP_ALREADY_EXISTS Indicates that the add operation attempted to add an entry that already exists, or that the modify operation attempted to rename an entry to the name of an entry that already exists.
				// Sun:
				//   [LDAP: error code 68 - Entry Already Exists]
				if(e.getMessage().startsWith("[LDAP: error code 68 - ")) {
					return DEFAULT_RESULT_CREATE_OK;
				} else {
					storeLdapException(e, session);
					throw new SenderException(e);
				}
			} finally {
				closeDirContext(dirContext);
			}
		}
		
	}
	
	private String performOperationDelete(String entryName, IPipeLineSession session, Map paramValueMap, Attributes attrs) throws SenderException, ParameterException {
		if (manipulationSubject.equals(MANIPULATION_ATTRIBUTE)) {
			String result=null;
			NamingEnumeration na = attrs.getAll();
			while(na.hasMoreElements()) {
				Attribute a = (Attribute)na.nextElement();
				log.debug("Delete attribute: " + a.getID());
				NamingEnumeration values;
				try {
					values = a.getAll();
				} catch (NamingException e1) {
					storeLdapException(e1, session);
					throw new SenderException("cannot obtain values of Attribute ["+a.getID()+"]",e1);
				}
				while(values.hasMoreElements()) {
					Attributes partialAttrs = new BasicAttributes();
					Attribute singleValuedAttribute;
					String id = a.getID();
					Object value = values.nextElement();
					if (log.isDebugEnabled()) {
						if (id.toLowerCase().contains("password") || id.toLowerCase().contains("pwd")) {
							log.debug("Delete value: ***");
						} else {
							log.debug("Delete value: " + value);
						}
					}
					if (unicodePwd && "unicodePwd".equalsIgnoreCase(id)) {
						singleValuedAttribute = new BasicAttribute(id, encodeUnicodePwd(value));
					} else {
						singleValuedAttribute = new BasicAttribute(id, value);
					}
					partialAttrs.put(singleValuedAttribute);
					DirContext dirContext = null;
					try {
						dirContext = getDirContext(paramValueMap);
						dirContext.modifyAttributes(entryName,	DirContext.REMOVE_ATTRIBUTE, partialAttrs);
					} catch(NamingException e) {
						// https://wiki.servicenow.com/index.php?title=LDAP_Error_Codes:
						//   16 LDAP_NO_SUCH_ATTRIBUTE Indicates that the attribute specified in the modify or compare operation does not exist in the entry.
						//   32 LDAP_NO_SUCH_OBJECT Indicates the target object cannot be found. This code is not returned on following operations: Search operations that find the search base but cannot find any entries that match the search filter. Bind operations. 
						// Sun:
						//   [LDAP: error code 16 - No Such Attribute...
						//   [LDAP: error code 32 - No Such Object...
						// AD:
						//   [LDAP: error code 16 - 00002085: AtrErr: DSID-03151F03, #1...
						if (e.getMessage().startsWith("[LDAP: error code 16 - ")
								|| e.getMessage().startsWith("[LDAP: error code 32 - ")) {
							if (log.isDebugEnabled()) log.debug("Operation [" + getOperation()+ "] successful: " + e.getMessage());
							result = DEFAULT_RESULT_DELETE;
						} else {
							storeLdapException(e, session);
							throw new SenderException("Exception in operation [" + getOperation()+ "] entryName ["+entryName+"]", e);
						}
					} finally {
						closeDirContext(dirContext);
					}
				}
			}
			if (result!=null) {
				return result;
			} 
			return DEFAULT_RESULT;
		} else {
			DirContext dirContext = null;
			try {
				dirContext = getDirContext(paramValueMap);
				dirContext.unbind(entryName);
				return DEFAULT_RESULT;
			} catch (NamingException e) {
				// https://wiki.servicenow.com/index.php?title=LDAP_Error_Codes:
				//   32 LDAP_NO_SUCH_OBJECT Indicates the target object cannot be found. This code is not returned on following operations: Search operations that find the search base but cannot find any entries that match the search filter. Bind operations. 
				// Sun:
				//   [LDAP: error code 32 - No Such Object...
				if (e.getMessage().startsWith("[LDAP: error code 32 - ")) {
					if (log.isDebugEnabled()) log.debug("Operation [" + getOperation()+ "] successful: " + e.getMessage());
					return DEFAULT_RESULT_DELETE;
				} else {
					storeLdapException(e, session);
					throw new SenderException("Exception in operation [" + getOperation()+ "] entryName ["+entryName+"]", e);
				}
			} finally {
				closeDirContext(dirContext);
			}
		}
	}

//	Constructs a search constraints using arguments.
//	public SearchControls(int scope,long countlim,int timelim, String[] attrs, boolean retobj,boolean deref)
//		Parameters:
//		scope - The search scope. One of: OBJECT_SCOPE, ONELEVEL_SCOPE, SUBTREE_SCOPE.
//		timelim - The number of milliseconds to wait before returning. If 0, wait indefinitely.
//		deref - If true, dereference links during search.
//		countlim - The maximum number of entries to return. If 0, return all entries that satisfy filter.
//		retobj - If true, return the object bound to the name of the entry; if false, do not return object.
//		attrs - The identifiers of the attributes to return along with the entry. If null, return all attributes. If empty return no attributes.

	private String performOperationSearch(String entryName, IPipeLineSession session, Map paramValueMap, String filterExpression, int scope) throws SenderException, ParameterException {
		int timeout=getSearchTimeout();
		SearchControls controls = new SearchControls(scope, getMaxEntriesReturned(), timeout, 
													 getAttributesReturnedParameter(), false, false);
//		attrs = parseAttributesFromMessage(message);
		DirContext dirContext = null;
		try {
			dirContext = getDirContext(paramValueMap);
			return searchResultsToXml( dirContext.search(entryName, filterExpression, controls) ).toXML();
		} catch (NamingException e) {
			if (isReplyNotFound() && e.getMessage().equals("Unprocessed Continuation Reference(s)")) {
				if (log.isDebugEnabled()) log.debug("Searching object not found using filter[" + filterExpression + "]");
				return DEFAULT_RESULT_SEARCH;
			} else {
			storeLdapException(e, session);
				throw new SenderException("Exception searching using filter ["+filterExpression+"]", e);
			}
		} finally {
			closeDirContext(dirContext);
		}
	}

	private String performOperationGetSubContexts(String entryName, IPipeLineSession session, Map paramValueMap) throws SenderException, ParameterException {
		DirContext dirContext = null;
		try {
			dirContext = getDirContext(paramValueMap);
			String[] subs = getSubContextList(dirContext, entryName, session);
			return subContextsToXml(entryName, subs, dirContext).toXML();
		} catch (NamingException e) {
			storeLdapException(e, session);
			throw new SenderException(e);
		} finally {
			closeDirContext(dirContext);
		}
	}
		
	private String performOperationGetTree(String entryName, IPipeLineSession session, Map paramValueMap) throws SenderException, ParameterException {
		DirContext dirContext = null;
		try {
			dirContext = getDirContext(paramValueMap);
			return getTree(dirContext, entryName, session, paramValueMap).toXML();
		} finally {
			closeDirContext(dirContext);
		}
	}

	private String performOperationChallenge(String principal, IPipeLineSession session, Map paramValueMap) throws SenderException, ParameterException {
		DirContext dirContext = null;
		try{
			// Use loopkupDirContext instead of getDirContext to prevent
			// NamingException (with error code 49) being converted to
			// SenderException.
			dirContext = loopkupDirContext(paramValueMap);
			attributesToXml(dirContext.getAttributes(principal, getAttributesReturnedParameter())).toXML();
			return DEFAULT_RESULT_CHALLENGE_OK;
		} catch(NamingException e) {
			// https://wiki.servicenow.com/index.php?title=LDAP_Error_Codes:
			//   49 LDAP_INVALID_CREDENTIALS Indicates that during a bind operation one of the following occurred: The client passed either an incorrect DN or password, or the password is incorrect because it has expired, intruder detection has locked the account, or another similar reason. This is equivalent to AD error code 52e.
			if(e.getMessage().startsWith("[LDAP: error code 49 - ") ) {
				if (log.isDebugEnabled()) log.debug("Operation [" + getOperation()+ "] invalid credentials for: " + principal);
				return DEFAULT_RESULT_CHALLENGE_NOK;	
			} else {
				storeLdapException(e, session);
				throw new SenderException("Exception in operation [" + getOperation()+ "] principal=["+principal+"]", e);	
			}
		} finally {
			closeDirContext(dirContext);
		}
	}

	private String performOperationChangeUnicodePwd(String entryName, IPipeLineSession session, Map paramValueMap) throws SenderException, ParameterException {
		ModificationItem[] modificationItems = new ModificationItem[2];
		modificationItems[0] = new ModificationItem(
				DirContext.REMOVE_ATTRIBUTE,
				new BasicAttribute("unicodePwd", encodeUnicodePwd(paramValueMap.get("oldPassword"))));
		modificationItems[1] = new ModificationItem(
				DirContext.ADD_ATTRIBUTE,
				new BasicAttribute("unicodePwd", encodeUnicodePwd(paramValueMap.get("newPassword"))));
		DirContext dirContext = null;
		try{
			dirContext = getDirContext(paramValueMap);
			dirContext.modifyAttributes(entryName, modificationItems);
			return DEFAULT_RESULT_CHANGE_UNICODE_PWD_OK;
		} catch(NamingException e) {
			// https://wiki.servicenow.com/index.php?title=LDAP_Error_Codes:
			//   19 LDAP_CONSTRAINT_VIOLATION Indicates that the attribute value specified in a modify, add, or modify DN operation violates constraints placed on the attribute. The constraint can be one of size or content (string only, no binary).
			// AD:
			//   [LDAP: error code 19 - 0000052D: AtrErr: DSID-03191041, #1...
			if(e.getMessage().startsWith("[LDAP: error code 19 - ") ) {
				if (log.isDebugEnabled()) log.debug("Operation [" + getOperation()+ "] old password doesn't match or new password doesn't comply with policy for: " + entryName);
				return DEFAULT_RESULT_CHANGE_UNICODE_PWD_NOK;
			} else {
				storeLdapException(e, session);
				throw new SenderException("Exception in operation [" + getOperation()+ "] entryName ["+entryName+"]", e);
			}
		} finally {
			closeDirContext(dirContext);
		}
	}

	/**
	 * Performs the specified operation and returns the results.
	 *  
	 * @return - Depending on operation, DEFAULT_RESULT or read/search result (always XML)
	 */
	public String performOperation(Message message, IPipeLineSession session) throws SenderException, ParameterException {
		Map<String,Object> paramValueMap = null;
		String entryName = null;
		if (paramList != null){
			paramValueMap = paramList.getValues(message, session).getValueMap();
			entryName = (String)paramValueMap.get("entryName");
			if (log.isDebugEnabled()) log.debug("entryName=["+entryName+"]");
		}
		if ((entryName == null || StringUtils.isEmpty(entryName))
				&&  !getOperation().equals(OPERATION_CHALLENGE)) {
			throw new SenderException("entryName must be defined through params, operation ["+ getOperation()+ "]");
		}
		if (getOperation().equals(OPERATION_READ)) {
			return performOperationRead(entryName, session, paramValueMap);
		} else if (getOperation().equals(OPERATION_UPDATE)) {
			return performOperationUpdate(entryName, session, paramValueMap, parseAttributesFromMessage(message));
		} else if (getOperation().equals(OPERATION_CREATE)) {
			return performOperationCreate(entryName, session, paramValueMap, parseAttributesFromMessage(message));
		} else if (getOperation().equals(OPERATION_DELETE)) {
			return performOperationDelete(entryName, session, paramValueMap, parseAttributesFromMessage(message));
		} else if (getOperation().equals(OPERATION_SEARCH)) {
			return performOperationSearch(entryName, session, paramValueMap, (String)paramValueMap.get(FILTER), SearchControls.ONELEVEL_SCOPE);
		} else if (getOperation().equals(OPERATION_DEEP_SEARCH)) {
			return performOperationSearch(entryName, session, paramValueMap, (String)paramValueMap.get(FILTER), SearchControls.SUBTREE_SCOPE);
		} else if (getOperation().equals(OPERATION_SUB_CONTEXTS)) {
			return performOperationGetSubContexts(entryName, session, paramValueMap);
		} else if (getOperation().equals(OPERATION_GET_TREE)) {
			return performOperationGetTree(entryName, session, paramValueMap);
		} else if (getOperation().equals(OPERATION_CHALLENGE)) {
			return performOperationChallenge((String)paramValueMap.get("principal"), session, paramValueMap);
		} else if (getOperation().equals(OPERATION_CHANGE_UNICODE_PWD)) {
			return performOperationChangeUnicodePwd(entryName, session, paramValueMap);
		} else {
			throw new SenderException("unknown operation [" + getOperation() + "]");
		}
	}

	/** 
	 * Return xml element containing all of the subcontexts of the parent context with their attributes. 
	 * @return tree xml.
	 */ 
	private XmlBuilder getTree(DirContext parentContext, String context, IPipeLineSession session, Map paramValueMap)
	{
		XmlBuilder contextElem = new XmlBuilder("context");
		contextElem.addAttribute("name", context);
		
		String[] subCtxList = getSubContextList(parentContext, context, session);
		try	{
			if (subCtxList.length == 0) {
				XmlBuilder attrs = attributesToXml(parentContext.getAttributes(context, getAttributesReturnedParameter()));
				contextElem.addSubElement(attrs);
			}
			else {
				for (int i = 0; i < subCtxList.length; i++)
				{
					contextElem.addSubElement( getTree((DirContext)parentContext.lookup(context), subCtxList[i], session, paramValueMap) );
				}
				contextElem.addSubElement( attributesToXml(parentContext.getAttributes(context, getAttributesReturnedParameter())));
			}

		} catch (NamingException e) {
			storeLdapException(e, session);
			log.error("Exception in operation [" + getOperation()+ "]: ", e);
		}

		return contextElem;
	}
	
	private XmlBuilder subContextsToXml(String entryName, String[] subs, DirContext dirContext) throws NamingException {
		
		XmlBuilder contextElem = new XmlBuilder("Context");
		XmlBuilder currentContextElem = new XmlBuilder("CurrentContext");
		currentContextElem.setValue(entryName + ","+ dirContext.getNameInNamespace());
		contextElem.addSubElement(currentContextElem);
		
		if (subs != null) {
			log.error("Subs.length = " + subs.length);
			for (int i = 0; i<subs.length; i++) {
				XmlBuilder subContextElem = new XmlBuilder("SubContext");
				
				subContextElem.setValue(subs[i]);
				
				contextElem.addSubElement(subContextElem);
			}
		}	
		return contextElem;
	}

	/** 
	 * Return a list of all of the subcontexts of the current context, which is relative to parentContext. 
	 * @return an array of Strings containing a list of the subcontexts for a current context.
	 */ 
	public String[] getSubContextList (DirContext parentContext, String relativeContext, IPipeLineSession session) {
		String[] retValue = null;

		try {
			// Create a vector object and add the names of all of the subcontexts
			//  to it
			Vector n = new Vector();
			NamingEnumeration list = parentContext.list(relativeContext);
			if (log.isDebugEnabled()) log.debug("getSubCOntextList(context) : context = " + relativeContext);
			for (int x = 0; list.hasMore(); x++) {
				NameClassPair nc = (NameClassPair)list.next();
				n.addElement (nc);
			}

			// Create a string array of the same size as the vector object
			String contextList[] = new String[n.size()];
			for (int x = 0; x < n.size(); x++) {
				// Add each name to the array
				contextList[x] = ((NameClassPair)(n.elementAt(x))).getName();
			}
			retValue = contextList;

		} catch (NamingException e) {
			storeLdapException(e, session);
			log.error("Exception in operation [" + getOperation()+ "] ", e);
		}
		
		return retValue;
	}

	/**
	 * Digests the input message and creates a <code>BasicAttributes</code> object, containing <code>BasicAttribute</code> objects,
	 * which represent the attributes of the specified entry.
	 * 
	 * <pre>
	 * BasicAttributes implements Attributes
	 * contains
	 * BasicAttribute implements Attribute
	 * </pre>
	 * 
	 * @see Attributes
	 * @see BasicAttributes
	 * @see Attribute
	 * @see BasicAttribute
	 */
	private Attributes parseAttributesFromMessage(Message message) throws SenderException {

		Digester digester = new Digester();
		digester.addObjectCreate("*/attributes",BasicAttributes.class);
		digester.addFactoryCreate("*/attributes/attribute",BasicAttributeFactory.class);
		digester.addSetNext("*/attributes/attribute","put");
		digester.addCallMethod("*/attributes/attribute/value","add",0);
		
		try {
			return (Attributes) digester.parse(message.asReader());
		} catch (Exception e) {
			throw new SenderException("[" + this.getClass().getName() + "] exception in digesting",	e);
		}
	}

	@Override
	public Message sendMessage(Message message, IPipeLineSession session) throws SenderException, TimeOutException {
		try {
			return new Message(performOperation(message, session));
		} catch (Exception e) {
			throw new SenderException("cannot obtain resultset for [" + message + "]", e);
		}
	}

	//	protected Attributes getAttributesFromParameters(ParameterResolutionContext prc) throws ParameterException {
	//		Parameter2AttributeHelper helper = new Parameter2AttributeHelper();
	//		prc.forAllParameters(paramList, helper);
	//		Attributes result = helper.result; 
	//		
	//		log.debug("LDAP STEP:	applyParameters(String message, ParameterResolutionContext prc)");
	//		log.debug("collected LDAP Attributes from parameters ["+result.toString()+"]");
	//		return result;
	//	}
	//	
	//	private class Parameter2AttributeHelper implements IParameterHandler {
	//		private Attributes result = new BasicAttributes(true); // ignore attribute name case
	//
	//		public void handleParam(String paramName, Object value) throws ParameterException {
	//			
	//			if (result.get(paramName) == null)
	//				result.put(new BasicAttribute(paramName, value));
	//			else
	//				result.get(paramName).add(value);
	//		
	//			log.debug("LDAP STEP:	(Parameter2 ATTRIBUTE Helper)handleParam(String paramName, Object value) - result = [" + result.toString() +"]");
	//		}
	//	}

	/**
	 *Strips all the values from the attributes in <code>input</code>. This is performed to be able to delete 
	 *the attributes without having to match the values. If values exist they must be exactly matched too in
	 *order to delete the attribute.
	 */
	protected Attributes removeValuesFromAttributes(Attributes input) {
		Attributes result = new BasicAttributes(true);
		// ignore attribute name case
		NamingEnumeration enumeration = input.getIDs();
		while (enumeration.hasMoreElements()) {
			String attrId = (String) enumeration.nextElement();
			result.put(new BasicAttribute(attrId));
		}
		return result;
	}

	/**
	 * Retrieves the DirContext from the JNDI environment and sets the <code>providerURL</code> back to <code>ldapProviderURL</code> if specified.
	 */
	protected synchronized DirContext loopkupDirContext(Map paramValueMap) throws NamingException, ParameterException {
		DirContext dirContext;
		if (jndiEnv==null) {
			Hashtable newJndiEnv = getJndiEnv();
			//newJndiEnv.put("com.sun.jndi.ldap.trace.ber", System.err);//ldap response in log for debug purposes
			if (getLdapProviderURL() != null) {
				//Overwriting the (realm)providerURL if specified in configuration
				newJndiEnv.put("java.naming.provider.url", getLdapProviderURL());
			}
			if (principalParameterFound) {
				newJndiEnv.put(Context.SECURITY_PRINCIPAL, paramValueMap.get("principal"));
				newJndiEnv.put(Context.SECURITY_CREDENTIALS, paramValueMap.get("credentials"));
			}
			if (isUsePooling()) {
				// Enable connection pooling
				newJndiEnv.put("com.sun.jndi.ldap.connect.pool", "true");
				//see http://java.sun.com/products/jndi/tutorial/ldap/connect/config.html 
//				newJndiEnv.put("com.sun.jndi.ldap.connect.pool.maxsize", "20" );
//				newJndiEnv.put("com.sun.jndi.ldap.connect.pool.prefsize", "10" );
//				newJndiEnv.put("com.sun.jndi.ldap.connect.pool.timeout", "300000" );
			} else {
				// Disable connection pooling
				newJndiEnv.put("com.sun.jndi.ldap.connect.pool", "false");
			}
			if (log.isDebugEnabled()) log.debug("created environment for LDAP provider URL [" + newJndiEnv.get("java.naming.provider.url") + "]");
			dirContext = new InitialDirContext(newJndiEnv);
			if (!principalParameterFound) {
				jndiEnv = newJndiEnv;
			}
		} else {
			dirContext = new InitialDirContext(jndiEnv);
		}
		return dirContext;
//		return (DirContext) dirContextTemplate.lookup(""); 	// return copy to be thread-safe
	}

	protected DirContext getDirContext(Map paramValueMap) throws SenderException, ParameterException {
		try {
			return loopkupDirContext(paramValueMap);
		} catch (NamingException e) {
			throw new SenderException("cannot create InitialDirContext for ldapProviderURL ["+ getLdapProviderURL()	+ "]",e);
		}
	}

	protected void closeDirContext(DirContext dirContext) {
		if (dirContext!=null) {
			try {
				dirContext.close();
			} catch (NamingException e) {
				log.warn("Exception closing DirContext", e);
			}
		}
	}

	/*	protected String searchResultsToXml(NamingEnumeration searchresults) {
			// log.debug("SearchResultsToXml for class ["+searchresults.getClass().getName()+"]:"+ToStringBuilder.reflectionToString(searchresults));
			log.debug("LDAP STEP:	SearchResultsToXml(NamingEnumeration searchresults)");
			XmlBuilder searchresultsElem = new XmlBuilder("searchresults");
			if (searchresults!=null) {
				try {
					while (searchresults.hasMore()) {
						SearchResult sr = (SearchResult)searchresults.next();
						// log.debug("result:"+ sr.toString());
	
						XmlBuilder itemElem = new XmlBuilder("item");
						itemElem.addAttribute("name",sr.getName());
						try {
							itemElem.addSubElement(attributesToXml(sr.getAttributes()));
						} catch (NamingException e) {
							itemElem.addAttribute("exceptionType",e.getClass().getName());
							itemElem.addAttribute("exceptionExplanation",e.getExplanation());
						} catch (Throwable t) {
							itemElem.addAttribute("exceptionType",t.getClass().getName());
							itemElem.addAttribute("exceptionExplanation",t.getMessage());
							itemElem.addAttribute("itemclass",sr.getClass().getName());
						}
						searchresultsElem.addSubElement(itemElem);
					}
				} catch (NamingException e) {
					searchresultsElem.addAttribute("exceptionType",e.getClass().getName());
					searchresultsElem.addAttribute("exceptionExplanation",e.getExplanation());
				}
			}
			return searchresultsElem.toXML();
		}*/

	protected XmlBuilder attributesToXml(Attributes atts)
		throws NamingException {
		XmlBuilder attributesElem = new XmlBuilder("attributes");
		
		NamingEnumeration all = atts.getAll();
		while (all.hasMore()) {
			Attribute attribute = (Attribute) all.next();
			XmlBuilder attributeElem = new XmlBuilder("attribute");
			attributeElem.addAttribute("name", attribute.getID());
			if (attribute.size() == 1 && attribute.get() != null) {
				attributeElem.addAttribute("value", XmlUtils.encodeCharsAndReplaceNonValidXmlCharacters(attribute.get().toString()));
			} else {
				NamingEnumeration values = attribute.getAll();
				while (values.hasMore()) {
					Object value = values.next();
					XmlBuilder itemElem = new XmlBuilder("item");
					itemElem.addAttribute("value", XmlUtils.encodeCharsAndReplaceNonValidXmlCharacters(value.toString()));
					attributeElem.addSubElement(itemElem);
				}
			}
			attributesElem.addSubElement(attributeElem);
		}
		return attributesElem;
	}

	private XmlBuilder searchResultsToXml(NamingEnumeration entries)
		throws NamingException {
		
		XmlBuilder entriesElem = new XmlBuilder("entries");
		int row=0;
		while ((getMaxEntriesReturned()==0 || row<getMaxEntriesReturned()) && entries.hasMore()) {
			SearchResult searchResult = (SearchResult) entries.next();
			XmlBuilder entryElem = new XmlBuilder("entry");
			 
			entryElem.addAttribute("name", searchResult.getName());
			entryElem.addSubElement(attributesToXml(searchResult.getAttributes()));
			
			entriesElem.addSubElement(entryElem);
			row++;
		}
		return entriesElem;
	}

	/**
	 * For more information see:
	 * http://msdn.microsoft.com/en-us/library/cc223248.aspx and
	 * http://stackoverflow.com/questions/15335614/changing-active-directory-user-password-from-java-program
	 * http://blogs.msdn.com/b/alextch/archive/2012/05/15/how-to-set-active-directory-password-from-java-application.aspx
	 * @throws SenderException 
	 */
	private byte[] encodeUnicodePwd(Object value) throws SenderException {
		log.debug("Encode unicodePwd value");
		String quotedPassword = "\"" + value + "\"";
		try {
			return quotedPassword.getBytes("UTF-16LE");
		} catch (UnsupportedEncodingException e) {
			throw new SenderException(e);
		}
	}

	@Override
	public void addParameter(Parameter p) {
		if (paramList == null) {
			paramList = new ParameterList();
		}
		paramList.add(p);
	}

	@Override
	public ParameterList getParameterList() {
		return paramList;
	}

	@IbisDoc({"name of the sender", ""})
	@Override
	public void setName(String name) {
		this.name = name;
	}
	@Override
	public String getName() {
		return name;
	}

	@IbisDoc({"specifies operation to perform. Must be one of <ul><li><code>read</code>: read the contents of an entry</li><li><code>create</code>: create an attribute or an entry</li><li><code>update</code>: update an attribute or an entry</li><li><code>delete</code>: delete an attribute or an entry</li><li><code>search</code>: search for an entry in the direct children of the specified root</li><li><code>deepSearch</code>: search for an entry in the complete tree below the specified root</li><li><code>getSubContexts</code>: get a list of the direct children of the specifed root</li><li><code>getTree</code>: get a copy of the complete tree below the specified root</li><li><code>challenge</code>: check username and password against LDAP specifying principal and credential using parameters</li><li><code>changeUnicodePwd</code>: typical user change-password operation (one of the two methods to modify the unicodePwd attribute in AD (http://support.microsoft.com/kb/263991))</li></ul>", "read"})
	public void setOperation(String string) {
		operation = string;
	}
	public String getOperation() {
		return operation;
	}

	@IbisDoc({"url to context to search in, e.g. 'ldap://edsnlm01.group.intranet/ou=people, o=ing' to search in te people group of ing cds. used to overwrite the providerurl specified in jmsrealm.", ""})
	public void setLdapProviderURL(String string) {
		ldapProviderURL = string;
	}
	public String getLdapProviderURL() {
		return ldapProviderURL;
	}

	@IbisDoc({"specifies subject to perform operation on. must be one of 'entry' or 'attribute'", "attribute"})
	public void setManipulationSubject(String string) {
		manipulationSubject = string;
	}
	public String getManipulationSubject() {
		return manipulationSubject;
	}

	@IbisDoc({"comma separated list of attributes to return. when no are attributes specified, all the attributes from the object read are returned.", "<i>all attributes</i>"})
	public void setAttributesToReturn(String string) {
		attributesToReturn = string;
	}
	public String getAttributesToReturn() {
		return attributesToReturn;
	}

	@IbisDoc({"specifies whether connection pooling is used or not", "true when principal not set as parameter, false otherwise"})
	public void setUsePooling(boolean b) {
		usePooling = b;
	}
	public boolean isUsePooling() {
		return usePooling;
	}

	@IbisDoc({"specifies the time (in ms) that is spent searching for results for operation search", "20000 ms"})
	public void setSearchTimeout(int i) {
		searchTimeout = i;
	}
	public int getSearchTimeout() {
		return searchTimeout;
	}


	@IbisDoc({"key of session variable used to store cause of errors", "errorreason"})
	public void setErrorSessionKey(String string) {
		errorSessionKey = string;
	}
	public String getErrorSessionKey() {
		return errorSessionKey;
	}

	@IbisDoc({"the maximum number of entries to be returned by a search query, or 0 for unlimited", "<i>0 (unlimited)</i>"})
	public void setMaxEntriesReturned(int i) {
		maxEntriesReturned = i;
	}
	public int getMaxEntriesReturned() {
		return maxEntriesReturned;
	}

	@IbisDoc({"when true the attributes passed by the input xml are scanned for an attribute with id unicodepwd, when found the value of this attribute will be encoded as required by active directory (a utf-16 encoded unicode string containing the password surrounded by quotation marks) before sending it to the ldap server", "false"})
	public void setUnicodePwd(boolean b) {
		unicodePwd = b;
	}
	public boolean getUnicodePwd() {
		return unicodePwd;
	}

	@IbisDoc({"(only used when <code>operation=search/deepsearch</code>) when <code>true</code> the xml '&lt;ldapresult&gt;object not found&lt;/ldapresult&gt;' is returned instead of the partialresultexception 'unprocessed continuation reference(s)'", "false"})
	public void setReplyNotFound(boolean b) {
		replyNotFound = b;
	}
	public boolean isReplyNotFound() {
		return replyNotFound;
	}
}
