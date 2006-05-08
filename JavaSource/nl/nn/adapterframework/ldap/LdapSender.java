/*
 * $Log: LdapSender.java,v $
 * Revision 1.9  2006-05-08 14:58:29  europe\L190409
 * start of 'multiple operation support'
 *
 * Revision 1.8  2005/10/24 10:00:21  John Dekker <john.dekker@ibissource.org>
 * *** empty log message ***
 *
 * Revision 1.7  2005/10/24 09:59:24  John Dekker <john.dekker@ibissource.org>
 * Add support for pattern parameters, and include them into several listeners,
 * senders and pipes that are file related
 *
 * Revision 1.6  2005/04/26 09:31:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * multiple response object support
 *
 * Revision 1.5  2005/04/14 08:00:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * preparations for multi-object result
 *
 * Revision 1.4  2005/03/29 14:47:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added version using parameters
 *
 * Revision 1.3  2005/03/24 12:24:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first working version
 *
 *
 */
package nl.nn.adapterframework.ldap;

import java.util.Properties;

import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchResult;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jms.JNDIBase;
import nl.nn.adapterframework.parameters.IParameterHandler;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Sender to obtain information from an LDAP Directory.
 * Returns the set of attributes in an XML format. An example is shown below:
 * 
 * The message to send should conform to LDAP syntax:<br/><code><pre>
 * 		uid=NI83NZ
 * </pre></code> <br/>
 * 
 * Sample result:<br/><code><pre>
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
 * </pre></code> <br/>
 * 
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.ldap.LdapSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProviderURL(String) providerURL}</td><td>URL to context to search in, e.g. 'ldap://edsnlm01.group.intranet/ou=People, o=ing' to search in te People group of ING CDS</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAuthentication(String) authentication}</td><td>JNDI authentication parameter</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCredentials(String) credentials}</td><td>JNDI credential parameter</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>sets jndi parameters from defined realm</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOperation(String) operation}</td><td>specifies operation to perform. Must be one of 'read', 'create', 'update', 'delete'</td><td>read</td></tr>
 * <tr><td>{@link #setNameAttribute(String) nameAttribute}</td><td>parameter used to specifiy name of LDAP object in context to perform operation on</td><td>uid</td></tr>
 * </table>
 * </p>
 * Instead of via a message, the input to the LDAP search can be specified by parameters, too. In that case the message itself is not interpreted as input.
 * The name of the parameter specifies an attribute-name in LDAP, the value specifies the LDAP value to be matched.
 * @author Gerrit van Brakel
 * @version Id
 */
public class LdapSender extends JNDIBase implements ISenderWithParameters {
	protected Logger log=Logger.getLogger(this.getClass());
	public static final String version="$RCSfile: LdapSender.java,v $  $Revision: 1.9 $ $Date: 2006-05-08 14:58:29 $";
	
	private static final String INITIAL_CONTEXT_FACTORY="com.sun.jndi.ldap.LdapCtxFactory";
	
	public static final String OPERATION_READ="read";
	public static final String OPERATION_CREATE="create";
	public static final String OPERATION_UPDATE="update";
	public static final String OPERATION_DELETE="delete";
	
	protected ParameterList paramList = null;
	
	private String name;
	private String operation=OPERATION_READ;
	private String nameAttribute="uid";
	
	private DirContext dirContext=null;
	
	private Properties namingSyntax=new Properties();
	
	public LdapSender() {
		super();
		setInitialContextFactoryName(INITIAL_CONTEXT_FACTORY);
	}
	
	public void configure() throws ConfigurationException {
		if (paramList!=null) {
			paramList.configure();
		}
		if (getOperation()==null || 
			!(getOperation().equals(OPERATION_READ)  || 
			  getOperation().equals(OPERATION_CREATE)|| 
			  getOperation().equals(OPERATION_UPDATE)|| 
			  getOperation().equals(OPERATION_DELETE))) {
			throw new ConfigurationException("attribute opereration ["+getOperation()+
						"] must be one of ("+OPERATION_READ+","+OPERATION_CREATE+","+OPERATION_UPDATE+","+OPERATION_DELETE+")");
  		}
  		if (getOperation().equals(OPERATION_UPDATE) && StringUtils.isEmpty(getNameAttribute())) {
  			throw new ConfigurationException("for opereration ["+getOperation()+" attriubte 'nameAttribute' must be specified");
  		}
		fillSyntax();
	}

	public void open() throws SenderException {
		getDirContext();
	}

	public void close() throws SenderException {
		dirContext=null;
	}

	public boolean isSynchronous() {
		return true;
	}

	protected void fillSyntax() {
		Properties syntax = getNamingSyntax();
		syntax.clear();
		syntax.put("jndi.syntax.direction","right_to_left");
		syntax.put("jndi.syntax.separator",",");
		syntax.put("jndi.syntax.ignorecase ","true");
		syntax.put("jndi.syntax.trimblanks","true");
	}

	protected Name getNameFromAttrs(ParameterResolutionContext prc) throws ParameterException, InvalidNameException {
		ParameterValueList pvl = prc.getValues(paramList);
		ParameterValue namePv = pvl.getParameterValue(getNameAttribute());
		return new CompositeName(getNameAttribute()+"='"+namePv.getValue()+"'");
//		Parameters2NameHelper helper = new Parameters2NameHelper(new CompositeName());
//		prc.forAllParameters(paramList, helper);
//		Name name = helper.result; 
//
//		log.debug("constructed LDAP Names from parameters ["+name+"]");
//		return name;
	}
	
	private class Parameters2NameHelper implements IParameterHandler {
		private Name result; 

		Parameters2NameHelper(Name base) {
			super();
			result = base;
		}

		public void handleParam(String paramName, Object value) throws ParameterException {
			try {
				result.add(paramName+"='"+value+"'");
			} catch (InvalidNameException e) {
				throw new ParameterException("cannot make name from parameter ["+paramName+"] value ["+value+"]",e);
			}
		}
	}
	
	public String performOperation(Attributes matchAttrs, ParameterResolutionContext prc) throws SenderException, ParameterException {
		String result=null;
		try {
			if (getOperation().equals(OPERATION_READ)) {
				NamingEnumeration searchresults=getDirContext().search("",matchAttrs);
				return SearchResultsToXml(searchresults);
			} else {
				if (getOperation().equals(OPERATION_UPDATE)) {
					Name name=getNameFromAttrs(prc);
					Attributes attrs=applyParameters(null,prc);
					getDirContext().modifyAttributes(name,DirContext.ADD_ATTRIBUTE+DirContext.REPLACE_ATTRIBUTE, attrs);
				} else {
					if (getOperation().equals(OPERATION_CREATE)) {
						Name name=getNameFromAttrs(prc);
						Attributes attrs=applyParameters(null,prc);
						getDirContext().bind(name, null, attrs);
					} else {
						if (getOperation().equals(OPERATION_DELETE)) {
							Name name=getNameFromAttrs(prc);
							getDirContext().unbind(name);
						} else {
							throw new SenderException("unknown operation ["+getOperation()+"]");
						}
					}
				}
			}
		} catch (NamingException e) {
			throw new SenderException("during operation ["+getOperation()+"]", e);
		}
		return result;
	}


	public String sendMessage(String correlationID, String message) throws SenderException {
		try {
			/*
			NamingEnumeration resultSet = getDirContext().list(message);
			log.info("results for ["+message+"]:"+ resultSet.toString());
			return SearchResultsToXml(resultSet);
			*/
			return attributesToXml(getDirContext().getAttributes(message)).toXML();
		} catch (NamingException e) {
			throw new SenderException("cannot obtain resultset for ["+message+"]",e);
		}					
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException {
		if (prc==null || paramList==null) {
			return sendMessage(correlationID, message);
		}
		try {
			Attributes matchAttrs = applyParameters(message, prc);
			NamingEnumeration searchresults=getDirContext().search("",matchAttrs);
			return SearchResultsToXml(searchresults);
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}
	
	protected Attributes applyParameters(String message, ParameterResolutionContext prc) throws ParameterException {
		Parameter2AttributeHelper helper = new Parameter2AttributeHelper();
		prc.forAllParameters(paramList, helper);
		Attributes result = helper.result; 

		log.debug("collected LDAP Attributes from parameters ["+result.toString()+"]");
		return result;
	}
	
	private class Parameter2AttributeHelper implements IParameterHandler {
		private Attributes result = new BasicAttributes(true); // ignore attribute name case

		public void handleParam(String paramName, Object value) throws ParameterException {
			result.put(new BasicAttribute(paramName, value));
		}
	}

	protected synchronized DirContext loopkupDirContext() throws NamingException {
		if (null == dirContext) {
			if (getInitialContextFactoryName() != null) {
				dirContext = (DirContext) new InitialDirContext(getJndiEnv());
			} else {
				dirContext = (DirContext) new InitialDirContext();
			}
			log.debug("obtained InitialDirContext for providerURL ["+getProviderURL()+"]");
		}
		return (DirContext) dirContext.lookup(""); // return copy to be thread-safe
	}
	
	protected DirContext getDirContext() throws SenderException {
		try {
			return loopkupDirContext();
		} catch (NamingException e) {
			throw new SenderException("cannot create InitialDirContext for providerURL ["+getProviderURL()+"]");
		}
	}

	protected String SearchResultsToXml(NamingEnumeration searchresults) {
		// log.debug("SearchResultsToXml for class ["+searchresults.getClass().getName()+"]:"+ToStringBuilder.reflectionToString(searchresults));
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
	}

	protected XmlBuilder attributesToXml(Attributes atts) throws NamingException {
		XmlBuilder attributesElem = new XmlBuilder("attributes");
//		attributesElem.addAttribute("size",atts.size()+"");
		NamingEnumeration all = atts.getAll();
		while (all.hasMore()) {
			Attribute attribute = (Attribute)all.next();
			XmlBuilder attributeElem = new XmlBuilder("attribute");
			attributeElem.addAttribute("name",attribute.getID());
			if (attribute.size()==1) {
				attributeElem.addAttribute("value",(String)attribute.get());
			} else {
//					attributeElem.addAttribute("size",attribute.size()+"");
				NamingEnumeration values = attribute.getAll();
				while (values.hasMore()) {
					Object value = values.next();
					XmlBuilder itemElem = new XmlBuilder("item");
					itemElem.addAttribute("value",value.toString());
					attributeElem.addSubElement(itemElem);
				}
			}
			attributesElem.addSubElement(attributeElem);
		}
		return attributesElem;
	}

	public void addParameter(Parameter p) { 
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}


	public void setName(String name) {
		this.name=name;
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

	public void setNameAttribute(String string) {
		nameAttribute = string;
	}
	public String getNameAttribute() {
		return nameAttribute;
	}

	public void setNamingSyntax(Properties properties) {
		namingSyntax = properties;
	}
	public Properties getNamingSyntax() {
		return namingSyntax;
	}

}
