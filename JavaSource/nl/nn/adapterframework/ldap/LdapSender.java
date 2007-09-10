/*
 * $Log: LdapSender.java,v $
 * Revision 1.27  2007-09-10 11:18:41  europe\L190409
 * updated javadoc
 *
 * Revision 1.26  2007/09/04 07:59:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made storeLdapException public
 *
 * Revision 1.25  2007/08/30 15:09:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * store additional information under reason session key
 *
 * Revision 1.24  2007/07/17 15:11:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improve javadoc
 *
 * Revision 1.23  2007/07/17 09:36:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reworked javadoc
 *
 * Revision 1.22  2007/07/16 09:40:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added deepSearch
 *
 * Revision 1.21  2007/07/10 07:20:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved javadoc
 *
 * Revision 1.20  2007/05/31 06:59:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fix check on parameter
 *
 * Revision 1.19  2007/05/29 11:09:06  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated some javadoc
 *
 * Revision 1.18  2007/05/21 12:19:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.17  2007/05/16 11:42:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cleanup code, remove threading problems, improve javadoc
 *
 * Revision 1.16  2007/04/24 11:36:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * avoid NPE on close
 *
 * Revision 1.15  2007/02/27 12:44:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected digesterrules
 * pooling optional
 * context.close() in finally in configure()
 *
 * Revision 1.14  2007/02/26 15:56:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * update of LDAP code, after a snapshot from Ibis4Toegang
 *
 */
package nl.nn.adapterframework.ldap;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jms.JNDIBase;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.digester.Digester;
import org.apache.commons.lang.StringUtils;

/**
 * Sender to obtain information from and write to an LDAP Directory.
 * Returns the set of attributes in an XML format. Examples are shown below.
 * 
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.ldap.LdapSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setLdapProviderURL(String) ldapProviderURL}</td><td>URL to context to search in, e.g. 'ldap://edsnlm01.group.intranet/ou=People, o=ing' to search in te People group of ING CDS. Used to overwrite the providerURL specified in jmsRealm.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>sets jndi parameters from defined realm (including authentication)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOperation(String) operation}</td><td>specifies operation to perform. Must be one of 
 * <ul>
 * <li><code>read</code>: read the contents of an entry</li>
 * <li><code>create</code>: create an attribute or an entry</li>
 * <li><code>update</code>: update an attribute or an entry</li>
 * <li><code>delete</code>: delete an attribute or an entry</li>
 * <li><code>search</code>: search for an entry in the direct children of the specified root</li>
 * <li><code>deepSearch</code>: search for an entry in the complete tree below the specified root</li>
 * <li><code>getSubContexts</code>: get a list of the direct children of the specifed root</li>
 * <li><code>getTree</code>: get a copy of the complete tree below the specified root</li>
 * </ul></td><td>read</td></tr>
 * <tr><td>{@link #setManipulationSubject(String) manipulationSubject}</td><td>specifies subject to perform operation on. Must be one of 'enrty' or 'attribute'</td><td>attribute</td></tr>
 * <tr><td>{@link #setErrorSessionKey(String) errorSessionKey}</td><td>key of session variable used to store cause of errors</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSearchTimeout(int) searchTimeout}</td><td>specifies the time (in ms) that is spent searching for results for operation Search</td><td>20000 ms</td></tr>
 * <tr><td>{@link #setUsePooling(boolean) usePooling}</td><td>specifies whether connection pooling is used or not</td><td>true</td></tr>
 * <tr><td>{@link #setInitialContextFactoryName(String) initialContextFactoryName}</td><td>class to use as initial context factory</td><td>com.sun.jndi.ldap.LdapCtxFactory</td></tr>
 * <tr><td>{@link #setAttributesToReturn(String) attributesToReturn}</td>  <td>comma separated list of attributes to return. when no are attributes specified, all the attributes from the object read are returned.</td><td><i>all attributes</i></td></tr>
 * <tr><td>{@link #setJndiAuthAlias(String) jndiAuthAlias}</td><td>Authentication alias, may be used to override principal and credential-settings</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * If there is only one parameter in the configaration of the pipe it will represent entryName (RDN) of interest.
 * The name of the parameter MUST be entryName (since version 4.6.0).
 * <p> 
 * If there are more then one parameter then the names are compulsory, in the following manner:
 * <ul>
 * <li>Object of interest must have the name [entryName] and must be present</li>
 * <li>filter expression (handy with searching - see RFC2254) - must have the name [filterExpression]</li>
 * </ul>
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
 * <tr><td>delete</td><td>
 * <ul>
 * 	  <li>parameter 'entryName', resolving to RDN of entry to delete</li>
 * 	  <li>no specific inputmessage required</li>
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
 * 
 * @version Id
 */
public class LdapSender extends JNDIBase implements ISenderWithParameters {
	public static final String version = "$RCSfile: LdapSender.java,v $  $Revision: 1.27 $ $Date: 2007-09-10 11:18:41 $";

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

	public static final String MANIPULATION_ENTRY = "entry";
	public static final String MANIPULATION_ATTRIBUTE = "attribute";

	//The results to return if the modifying operation succeeds (an XML, to make it "next pipe ready")  
	private static final String DEFAULT_RESULT = "<LdapResult>Success</LdapResult>";
	private static final String DEFAULT_RESULT_READ = "<LdapResult>No such object</LdapResult>";
			
	private static final String DEFAULT_RESULT_DELETE = "<LdapResult>Delete Success - Never Existed</LdapResult>";
	private static final String DEFAULT_RESULT_CREATE_OK= "<LdapResult>Create Success - Already There</LdapResult>";
	private static final String DEFAULT_RESULT_CREATE_NOK= "<LdapResult>Create FAILED - Entry with given name already exists</LdapResult>";
	private static final String DEFAULT_RESULT_UPDATE_NOK= "<LdapResult>Update FAILED</LdapResult>";
		
		

	private String name;
	private String operation = OPERATION_READ;
	private String manipulationSubject = MANIPULATION_ATTRIBUTE;
	private String ldapProviderURL;
	private String attributesToReturn;
	private boolean usePooling=true;
	private String errorSessionKey=null;

	protected ParameterList paramList = null;
	private Hashtable jndiEnv=null;
	
//	private TransformerPool entryNameExtractor=null;

	public LdapSender() {
		super();
		setInitialContextFactoryName(INITIAL_CONTEXT_FACTORY);
	}

	public void configure() throws ConfigurationException {
		if (paramList == null || !entryNameParameterPresent()) {
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
				|| getOperation().equals(OPERATION_GET_TREE))) {
			throw new ConfigurationException("attribute opereration ["	+ getOperation()
												+ "] must be one of ("
												+ OPERATION_READ+ ","
												+ OPERATION_CREATE+ ","
												+ OPERATION_UPDATE+ ","
												+ OPERATION_DELETE+ ","
												+ OPERATION_SEARCH+ ","
												+ OPERATION_DEEP_SEARCH+ ","
												+ OPERATION_SUB_CONTEXTS+ ","
												+ OPERATION_GET_TREE+ ")");
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
//		if(attributesToReturn != null) setAttribubtesParameter();
//		if (StringUtils.isNotEmpty(getDigesterRulesFile())) {
//			try {
//				rulesURL = ClassUtils.getResourceURL(this, getDigesterRulesFile());
//				DigesterLoader.createDigester(rulesURL);
//				// load rules to check if they can be loaded when needed
//			} catch (Exception e) {
//				throw new ConfigurationException("["+ getClass().getName()	+ "] Digester rules file ["	+ getDigesterRulesFile() + "] not found", e);
//			}
//		}

//		fillSyntax();

		DirContext dirContext=null;
		try {
			dirContext = getDirContext();
		} catch (Exception e) {
			throw new ConfigurationException("["+ getClass().getName() + "] Context could not be found ", e);
		} finally {
			if (dirContext!=null) {
				try {
					dirContext.close();
				} catch (NamingException e) {
					log.warn("["+ getClass().getName() + "] Context could not be closed", e);
				}
			}
		}
	}

	/**
	 * Checks if the param with the name entryName is present. This param represents the RDN of the object in de active directory.
	 */
	private boolean entryNameParameterPresent() {
		
		boolean result = false;
		
		Iterator it = paramList.iterator();
		while (it.hasNext())
		{
			Parameter p = (Parameter)it.next();
			if (p.getName().equals(ENTRYNAME)) {
				result = true;
				break;
			}
		}
		return result;
	}

	public void storeLdapException(Throwable t, PipeLineSession pls) {
		if (StringUtils.isNotEmpty(getErrorSessionKey()) && pls!=null && t!=null) {
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
			pls.put(getErrorSessionKey(),reasonXml);
		}
		log.info("exit storeLdapException");
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
		
		ArrayList list = new ArrayList();
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




	public void open() throws SenderException {
	}

	public void close() throws SenderException {
	}

	public boolean isSynchronous() {
		return true;
	}

	/*
	 * Uses <code>Parameters2NameHelper</code> to create a CompositeName from parameter 
	 */
//	protected Name getNameFromParams(ParameterResolutionContext prc)
//		throws ParameterException, InvalidNameException {
//		Parameters2NameHelper helper = new Parameters2NameHelper(new CompositeName());
//		prc.forAllParameters(paramList, helper);
//		Name name = helper.result;
//
//		log.debug("constructed LDAP Names from parameters [" + name + "]");
//		return name;
//	}

//	private class Parameters2NameHelper implements IParameterHandler {
//		private Name result;
//		Parameters2NameHelper(Name base) {
//			super();
//			result = base;
//		}
//
//		public void handleParam(String paramName, Object value)
//			throws ParameterException {
//			try {
//				//				result.add(paramName+"='"+value+"'");
//				result.add((String) value);
//			} catch (InvalidNameException e) {
//				throw new ParameterException("cannot make name from parameter ["+ paramName	+ "] value ["+ value + "]",	e);
//			}
//		}
//	}

	private String performOperationRead(DirContext dirContext, String entryName, PipeLineSession pls) throws SenderException {
		try{
			return attributesToXml(dirContext.getAttributes(entryName, getAttributesReturnedParameter())).toXML();				
		} catch(NamingException e) {
			storeLdapException(e,pls);
			if(	e.getMessage().startsWith("[LDAP: error code 32 - No Such Object") ) {
				log.info("Operation [" + getOperation()+ "] found nothing - no such entryName: " + entryName);
				return DEFAULT_RESULT_READ;	
			} else {
				throw new SenderException("Exception in operation [" + getOperation()+ "] entryName=["+entryName+"]", e);	
			}
		}
	}

	private String performOperationUpdate(DirContext dirContext, String entryName, PipeLineSession pls, Attributes attrs) throws SenderException {
		if (manipulationSubject.equals(MANIPULATION_ATTRIBUTE)) {
			NamingEnumeration na = attrs.getAll();
			while(na.hasMoreElements()) {
				Attribute a = (Attribute)na.nextElement();
				NamingEnumeration values;
				try {
					values = a.getAll();
				} catch (NamingException e1) {
					storeLdapException(e1,pls);
					throw new SenderException("cannot obtain values of Attribute ["+a.getID()+"]",e1);
				}
				while(values.hasMoreElements()) {
					Attributes partialAttrs = new BasicAttributes();
					Attribute singleValuedAttribute = new BasicAttribute(a.getID(),values.nextElement());
					partialAttrs.put(singleValuedAttribute);
					try {
						dirContext.modifyAttributes(entryName,	DirContext.REPLACE_ATTRIBUTE, partialAttrs);
					} catch(NamingException e) {
						storeLdapException(e,pls);
						String msg;
						if (e.getMessage().startsWith("[LDAP: error code 32 - No Such Object") ) {
							msg="Operation [" + getOperation()+ "] failed - wrong entryName ["+ entryName+"]";	
						} else {
							msg="Exception in operation [" + getOperation()+ "] entryName ["+entryName+"]";									
						}
						//result = DEFAULT_RESULT_UPDATE_NOK;
						throw new SenderException(msg,e);
					}
				}
			}
			return DEFAULT_RESULT;
		} else {
			try {
				//dirContext.rename(newEntryName, oldEntryName);
				//result = DEFAULT_RESULT;
				dirContext.rename(entryName, entryName);
				return "<LdapResult>Deze functionaliteit is nog niet beschikbaar - naam niet veranderd.</LdapResult>";
			} catch (NamingException e) {
				storeLdapException(e,pls);
				log.error("Exception in operation [" + getOperation()+ "] entryName ["+entryName+"]", e);
				if(!e.getMessage().startsWith("[LDAP: error code 68 - Entry Already Exists]")) {
					throw new SenderException(e);
				}
				return DEFAULT_RESULT_CREATE_NOK;
			}
		}
	}

	private String performOperationCreate(DirContext dirContext, String entryName, PipeLineSession pls, Attributes attrs) throws SenderException {
		if (manipulationSubject.equals(MANIPULATION_ATTRIBUTE)) {
			String result=null;
			NamingEnumeration na = attrs.getAll();
			while(na.hasMoreElements())
			{
				Attribute a = (Attribute)na.nextElement();
				NamingEnumeration values;
				try {
					values = a.getAll();
				} catch (NamingException e1) {
					storeLdapException(e1,pls);
					throw new SenderException("cannot obtain values of Attribute ["+a.getID()+"]",e1);
				}
				while(values.hasMoreElements())
				{
					Attributes partialAttrs = new BasicAttributes();
					Attribute singleValuedAttribute = new BasicAttribute(a.getID(),values.nextElement());
					partialAttrs.put(singleValuedAttribute);
					try{
						dirContext.modifyAttributes(entryName,	DirContext.ADD_ATTRIBUTE, partialAttrs);
					}catch(NamingException e){
						if(	e.getMessage().startsWith("[LDAP: error code 20 - Attribute Or Value Exists]") )
						{
							log.info("Operation [" + getOperation()+ "] successful: " + e.getMessage());	
							result = DEFAULT_RESULT_CREATE_OK;
						}
						else{		
							storeLdapException(e,pls);
							throw new SenderException("Exception in operation [" + getOperation()+ "] entryName ["+entryName+"]", e );
						}
					}
				}
			}
			if (result!=null) {
				return result;
			} 
			return DEFAULT_RESULT;		
		} else {
			try {
				dirContext.bind(entryName, null, attrs);
				return DEFAULT_RESULT;
			} catch (NamingException e) {
				storeLdapException(e,pls);
				// log.debug("Exception in operation [" + getOperation()+ "] entryName ["+entryName+"]", e);
				log.debug("Exception in operation [" + getOperation()+ "] entryName ["+entryName+"]: "+ e.getMessage());
				if(e.getMessage().startsWith("[LDAP: error code 68 - Entry Already Exists]")) {
					return DEFAULT_RESULT_CREATE_OK;
				} else {
					throw new SenderException(e);
				}
			}
		}
		
	}
	
	private String performOperationDeleteAttributes(DirContext dirContext, String entryName, PipeLineSession pls, Attributes attrs) throws SenderException {
		NamingEnumeration na = attrs.getAll();
		String result=null;
		while(na.hasMoreElements()) {
			Attribute a = (Attribute)na.nextElement();
			NamingEnumeration values;
			try {
				values = a.getAll();
			} catch (NamingException e1) {
				storeLdapException(e1,pls);
				throw new SenderException("cannot obtain values of Attribute ["+a.getID()+"]",e1);
			}
			while(values.hasMoreElements()) {
				Attributes partialAttrs = new BasicAttributes();
				Attribute singleValuedAttribute = new BasicAttribute(a.getID(),values.nextElement());
				partialAttrs.put(singleValuedAttribute);
				try {
					dirContext.modifyAttributes(entryName,	DirContext.REMOVE_ATTRIBUTE, partialAttrs);
				} catch(NamingException e) {
					if(	e.getMessage().startsWith("[LDAP: error code 16 - No Such Attribute") ||
						e.getMessage().startsWith("[LDAP: error code 32 - No Such Object")) 
					{
						log.info("Operation [" + getOperation()+ "] successful: " + e.getMessage());
						result = DEFAULT_RESULT_DELETE;
					} else {
						storeLdapException(e,pls);
						throw new SenderException("Exception in operation [" + getOperation()+ "] entryName ["+entryName+"]", e);
					}
				}
			}
		}
		if (result!=null) {
			return result;
		} 
		return DEFAULT_RESULT;
	}

	private String performOperationDeleteEntry(DirContext dirContext, String entryName, PipeLineSession pls) throws SenderException {
		try {
			dirContext.unbind(entryName);
			return DEFAULT_RESULT;
		} catch (NamingException e) {
			storeLdapException(e,pls);
			if(	e.getMessage().startsWith("[LDAP: error code 32 - No Such Object")) {
				log.info("Operation [" + getOperation()+ "] successful: " + e.getMessage());
				return DEFAULT_RESULT_DELETE;
			} else {
				throw new SenderException("Exception in operation [" + getOperation()+ "] entryName ["+entryName+"]", e);
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

	private String performOperationSearch(DirContext dirContext, String entryName, PipeLineSession pls, String filterExpression, int scope) throws SenderException {
		int timeout=getSearchTimeout();
		SearchControls controls = new SearchControls(scope, 0, timeout, 
													 getAttributesReturnedParameter(), false, false);
//		attrs = parseAttributesFromMessage(message);
		try {
			return searchResultsToXml( dirContext.search(entryName, filterExpression, controls) ).toXML();
		} catch (NamingException e) {
			storeLdapException(e,pls);
			throw new SenderException("exception searching using filter ["+filterExpression+"]", e);
		}
	}

	private String performOperationGetSubContexts(DirContext dirContext, String entryName, PipeLineSession pls) throws SenderException {
		String[] subs = getSubContextList(dirContext, entryName);
		try {
			return subContextsToXml(entryName, subs, dirContext).toXML();
		} catch (NamingException e) {
			storeLdapException(e,pls);
			throw new SenderException(e);
		}
	}
		
	private String performOperationGetTree(DirContext dirContext, String entryName, PipeLineSession pls) throws SenderException {
		return getTree(dirContext, entryName, pls).toXML();
	}

	/**
	 * Performs the specified operation and returns the results.
	 *  
	 * @return - Depending on operation, DEFAULT_RESULT or read/search result (always XML)
	 */
	public String performOperation( String message,	ParameterResolutionContext prc)
		throws SenderException, ParameterException {
		
		String entryName=null;
		
		if (paramList!=null && prc!=null){
			entryName = (String)prc.getValueMap(paramList).get("entryName");
			log.debug("entryName=["+entryName+"]");
		} else {
//			try {
//				entryName = entryNameExtractor.transform(message,null);
//			} catch (Exception e) {
//				throw new SenderException(e);
//			}
		}
		
		if (entryName == null || StringUtils.isEmpty(entryName)) {
			throw new SenderException("entryName must be defined through params, operation ["+ getOperation()+ "]");
		}
		PipeLineSession pls=null;
		if (prc!=null) {
			pls=prc.getSession();
		}
		DirContext dirContext = getDirContext();

		try {// **************** READ **************** 
			if (getOperation().equals(OPERATION_READ)) {
				return performOperationRead(dirContext,entryName,pls);
			} // **************** UPDATE ****************  
			else if (getOperation().equals(OPERATION_UPDATE)) {
				return performOperationUpdate(dirContext,entryName,pls,parseAttributesFromMessage(message));
			}// **************** CREATE **************** 
			else if (getOperation().equals(OPERATION_CREATE)) {
				return performOperationCreate(dirContext,entryName,pls,parseAttributesFromMessage(message));
			}// **************** DELETE **************** 
			else if (getOperation().equals(OPERATION_DELETE)) {
				if (manipulationSubject.equals(MANIPULATION_ATTRIBUTE)) {
					return performOperationDeleteAttributes(dirContext,entryName,pls,parseAttributesFromMessage(message));
				} else {
					return performOperationDeleteEntry(dirContext,entryName,pls);
				}
			} // **************** SEARCH ****************			
			else if (getOperation().equals(OPERATION_SEARCH)) {
				return performOperationSearch(dirContext,entryName,pls,(String) prc.getValueMap(paramList).get(FILTER), SearchControls.ONELEVEL_SCOPE);
			} // **************** DEEP_SEARCH ****************			
			else if (getOperation().equals(OPERATION_DEEP_SEARCH)) {
				return performOperationSearch(dirContext,entryName,pls,(String) prc.getValueMap(paramList).get(FILTER), SearchControls.SUBTREE_SCOPE);
			} // **************** SUB_CONTEXTS ****************
			else if (getOperation().equals(OPERATION_SUB_CONTEXTS)) {
				return performOperationGetSubContexts(dirContext,entryName,pls);
			} // **************** GET_TREE ****************
			else if (getOperation().equals(OPERATION_GET_TREE)) {
				return performOperationGetTree(dirContext,entryName,pls);
			}
			else {
				throw new SenderException("unknown operation [" + getOperation() + "]");
			}
		} finally {
			if (dirContext!=null) {
				try {
					dirContext.close();
				} catch (NamingException e) {
					log.warn("Exception closing DirContext",e);
				}
			}
		}
	}
	
	
	
	
	/** 
	 * Return xml element containing all of the subcontexts of the parent context with their attributes. 
	 * @return tree xml.
	 */ 
	private XmlBuilder getTree(DirContext parentContext, String context, PipeLineSession pls)
	{
		XmlBuilder contextElem = new XmlBuilder("context");
		contextElem.addAttribute("name", context);
		
		String[] subCtxList = getSubContextList(parentContext, context);
		try	{
			if (subCtxList.length == 0) {
				XmlBuilder attrs = attributesToXml(parentContext.getAttributes(context, getAttributesReturnedParameter()));
				contextElem.addSubElement(attrs);
			}
			else {
				for (int i = 0; i < subCtxList.length; i++)
				{
					contextElem.addSubElement( getTree((DirContext)parentContext.lookup(context), subCtxList[i], pls) );
				}
				contextElem.addSubElement( attributesToXml(parentContext.getAttributes(context, getAttributesReturnedParameter())));
			}

		} catch (NamingException e) {
			storeLdapException(e,pls);
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
	public String[] getSubContextList (DirContext parentContext, String relativeContext) {
		String[] retValue = null;

		try {
			// Create a vector object and add the names of all of the subcontexts
			//  to it
			Vector n = new Vector();
			NamingEnumeration list = parentContext.list(relativeContext);
			log.debug("getSubCOntextList(context) : context = " + relativeContext);
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
	 * @see javax.naming.directory.Attributes
	 * @see javax.naming.directory.BasicAttributes
	 * @see javax.naming.directory.Attribute
	 * @see javax.naming.directory.BasicAttribute  
	 * @return
	 */
	private Attributes parseAttributesFromMessage(String message) throws SenderException {

		Digester digester = new Digester();
		digester.addObjectCreate("*/attributes",BasicAttributes.class);
		digester.addFactoryCreate("*/attributes/attribute",BasicAttributeFactory.class);
		digester.addSetNext("*/attributes/attribute","put");
		digester.addCallMethod("*/attributes/attribute/value","add",0);
		
		try {
			return (Attributes) digester.parse(new StringReader(message));
		} catch (Exception e) {
			throw new SenderException("[" + this.getClass().getName() + "] exception in digesting",	e);
		}
	}

	public String sendMessage(String correlationID, String message)	throws SenderException {
		return sendMessage(correlationID, message, new ParameterResolutionContext(message,null));
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException {
		try {
			return performOperation(message, prc);
		} catch (Exception e) {
			throw new SenderException("cannot obtain resultset for [" + message + "]", e);
		}
	}

	//	protected Attributes getAttributesFromParameters(ParameterResolutionContext prc) throws ParameterException {
	//		Parameter2AttributeHelper helper = new Parameter2AttributeHelper();
	//		prc.forAllParameters(paramList, helper);
	//		Attributes result = helper.result; 
	//		
	//		log.info("LDAP STEP:	applyParameters(String message, ParameterResolutionContext prc)");
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
	//			log.info("LDAP STEP:	(Parameter2 ATTRIBUTE Helper)handleParam(String paramName, Object value) - result = [" + result.toString() +"]");
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
		NamingEnumeration enum = input.getIDs();
		while (enum.hasMoreElements()) {
			String attrId = (String) enum.nextElement();
			result.put(new BasicAttribute(attrId));
		}
		return result;
	}

	/**
	 * Retrieves the DirContext from the JNDI environment and sets the <code>providerURL</code> back to <code>ldapProviderURL</code> if specified.
	 * 
	 */
	protected synchronized DirContext loopkupDirContext() throws NamingException {

		if (jndiEnv==null) {
			jndiEnv = getJndiEnv();
			//jndiEnv.put("com.sun.jndi.ldap.trace.ber", System.err);//ldap response in log for debug purposes
			if (getLdapProviderURL() != null) {
				//Overwriting the (realm)providerURL if specified in configuration
				jndiEnv.put("java.naming.provider.url", getLdapProviderURL());
			}
			if (isUsePooling()) {
				// Enable connection pooling
				jndiEnv.put("com.sun.jndi.ldap.connect.pool", "true");
				//see http://java.sun.com/products/jndi/tutorial/ldap/connect/config.html 
//				jndiEnv.put("com.sun.jndi.ldap.connect.pool.maxsize", "20" );
//				jndiEnv.put("com.sun.jndi.ldap.connect.pool.prefsize", "10" );
//				jndiEnv.put("com.sun.jndi.ldap.connect.pool.timeout", "300000" );
			} else {
				// Enable connection pooling
				jndiEnv.put("com.sun.jndi.ldap.connect.pool", "false");
			}
			if (log.isDebugEnabled()) log.debug("created environment for LDAP provider URL [" + jndiEnv.get("java.naming.provider.url") + "]");
		}
			
		DirContext dirContext = (DirContext) new InitialDirContext(jndiEnv);
		return dirContext;
//		return (DirContext) dirContextTemplate.lookup(""); 	// return copy to be thread-safe
	}

	protected DirContext getDirContext() throws SenderException {
		try {
			return loopkupDirContext();
		} catch (NamingException e) {
			throw new SenderException("cannot create InitialDirContext for ldapProviderURL ["+ getLdapProviderURL()	+ "]",e);
		}
	}

	/*	protected String searchResultsToXml(NamingEnumeration searchresults) {
			// log.debug("SearchResultsToXml for class ["+searchresults.getClass().getName()+"]:"+ToStringBuilder.reflectionToString(searchresults));
			log.info("LDAP STEP:	SearchResultsToXml(NamingEnumeration searchresults)");
			XmlBuilder searchresultsElem = new XmlBuilder("searchresults");
			if (searchresults!=null) {
				try {
					while (searchresults.hasMore()) {
						SearchResult sr = (SearchResult)searchresults.next();
						// log.info("result:"+ sr.toString());
	
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
				attributeElem.addAttribute("value", attribute.get().toString());
			} else {
				NamingEnumeration values = attribute.getAll();
				while (values.hasMore()) {
					Object value = values.next();
					XmlBuilder itemElem = new XmlBuilder("item");
					itemElem.addAttribute("value", value.toString());
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
		while (entries.hasMore()) {
			SearchResult searchResult = (SearchResult) entries.next();
			XmlBuilder entryElem = new XmlBuilder("entry");
			 
			entryElem.addAttribute("name", searchResult.getName());
			entryElem.addSubElement(attributesToXml(searchResult.getAttributes()));
			
			entriesElem.addSubElement(entryElem);
		}
		return entriesElem;
	}

	public void addParameter(Parameter p) {
		if (paramList == null) {
			paramList = new ParameterList();
		}
		paramList.add(p);
	}

	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}

	public void setOperation(String string) {
		operation = string;
	}
	public String getOperation() {
		return operation;
	}

	public void setLdapProviderURL(String string) {
		ldapProviderURL = string;
	}
	public String getLdapProviderURL() {
		return ldapProviderURL;
	}

	public void setManipulationSubject(String string) {
		manipulationSubject = string;
	}
	public String getManipulationSubject() {
		return manipulationSubject;
	}

	public void setAttributesToReturn(String string) {
		attributesToReturn = string;
	}
	public String getAttributesToReturn() {
		return attributesToReturn;
	}

	public void setUsePooling(boolean b) {
		usePooling = b;
	}
	public boolean isUsePooling() {
		return usePooling;
	}

	public void setSearchTimeout(int i) {
		searchTimeout = i;
	}
	public int getSearchTimeout() {
		return searchTimeout;
	}


	public void setErrorSessionKey(String string) {
		errorSessionKey = string;
	}
	public String getErrorSessionKey() {
		return errorSessionKey;
	}

}