/*
 * $Log: LdapSender.java,v $
 * Revision 1.4  2005-03-29 14:47:15  L190409
 * added version using parameters
 *
 * Revision 1.3  2005/03/24 12:24:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first working version
 *
 *
 */
package nl.nn.adapterframework.ldap;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jms.JNDIBase;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.XmlBuilder;

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
 * </table>
 * </p>
 * Instead of via a message, the input to the LDAP search can be specified by parameters, too. In that case the message itself is not interpreted as input.
 * @author Gerrit van Brakel
 * @version Id
 */
public class LdapSender extends JNDIBase implements ISenderWithParameters {
	protected Logger log=Logger.getLogger(this.getClass());
	public static final String version="$Id: LdapSender.java,v 1.4 2005-03-29 14:47:15 L190409 Exp $";
	
	private static final String INITIAL_CONTEXT_FACTORY="com.sun.jndi.ldap.LdapCtxFactory";
	protected ParameterList paramList = null;
	
	private String name;
	
	private DirContext dirContext=null;
	
	public LdapSender() {
		super();
		setInitialContextFactoryName(INITIAL_CONTEXT_FACTORY);
	}
	
	public void configure() throws ConfigurationException {
		if (paramList!=null) {
			paramList.configure();
		}
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

	public String sendMessage(String correlationID, String message) throws SenderException {
		try {
			Attributes resultSet = getDirContext().getAttributes(message);
			return attributesToXml(resultSet);
		} catch (NamingException e) {
			throw new SenderException("cannot obtain attributes for ["+message+"]",e);
		}					
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException {
		if (prc==null || paramList==null) {
			return sendMessage(correlationID, message);
		}
		try {
			String queryString = applyParameters(message,prc);
			return sendMessage(correlationID, queryString);
		} catch (ParameterException e) {
			throw new SenderException(e);
		}
	}
	
	protected String applyParameters(String message, ParameterResolutionContext prc) throws ParameterException {
		// message is not used in default implementation
		String result=null;
		for (int i=0; i<paramList.size(); i++) {
			Parameter p = paramList.getParameter(i);
			if (StringUtils.isNotEmpty(p.getName())) {
				String clause=p.getName()+"="+ p.getValue(prc);
				if (result==null) {
					result = clause;
				} else {
					result = clause +", "+ result;
				}
			}
		}
		log.debug("collected LDAP query-clause from parameters ["+result+"]");
		return result;
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
			return loopkupDirContext(); // return copy to be thread-safe
		} catch (NamingException e) {
			throw new SenderException("cannot create InitialDirContext for providerURL ["+getProviderURL()+"]");
		}
	}

	protected String attributesToXml(Attributes atts) throws NamingException {
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
		return attributesElem.toXML();
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

}
