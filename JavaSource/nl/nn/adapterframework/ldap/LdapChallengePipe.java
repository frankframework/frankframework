/*
 * $Log: LdapChallengePipe.java,v $
 * Revision 1.4  2007-08-03 08:45:28  europe\L190409
 * fixed bug, added internal entryName parameter
 *
 * Revision 1.3  2007/05/16 11:42:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cleanup code, remove threading problems, improve javadoc
 *
 * Revision 1.2  2007/02/27 12:48:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * set pooling off
 *
 * Revision 1.1  2007/02/26 15:56:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * update of LDAP code, after a snapshot from Ibis4Toegang
 */
package nl.nn.adapterframework.ldap;

import java.util.HashMap;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.pipes.AbstractPipe;

import org.apache.commons.lang.StringUtils;

/**
 * Pipe to check if a username and password are valid in LDAP.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.ldap.LdapChallengePipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setLdapProviderURL(String) ldapProviderURL}</td><td>URL to the LDAP server. <br/>Example: ldap://su05b9.itc.intranet</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInitialContextFactoryName(String) initialContextFactoryName}</td><td>class to use as initial context factory</td><td>com.sun.jndi.ldap.LdapCtxFactory</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>ldapProviderURL</td><td>URL to the LDAP server. <br/>Example: ldap://su05b9.itc.intranet</td><td>Required only if attribute ldapProviderURL is not set</td></tr>
 * <tr><td>principal</td><td>The LDAP DN for the username. <br/>Example: UID=SRP,OU=DI-IUF-EP,OU=SERVICES,O=ING</td><td>Required and must be filled</td></tr>
 * <tr><td>credentials</td><td>The LDAP password. <br/> Example: welkom01</td><td>Required and must be filled</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th><th>remarks</th></tr>
 * <tr><td>success</td><td>Successful login to LDAP</td><td>should be defined in configuration</td></tr>
 * <tr><td>invalid</td><td>Unsuccessful login to LDAP</td><td>should be defined in configuration</td></tr>
 * </table>
 * </p>
 * 
 * @author  Milan Tomc
 * @version Id
 */
public class LdapChallengePipe extends AbstractPipe {
	public static String version = "$RCSfile: LdapChallengePipe.java,v $  $Revision: 1.4 $ $Date: 2007-08-03 08:45:28 $";

	private String ldapProviderURL=null;
	private String initialContextFactoryName=null;

	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(ldapProviderURL) && getParameterList().findParameter("ldapProviderURL")==null) {
			throw new ConfigurationException("ldapProviderURL must be specified, either as attribute or as parameter");
		}
		if (StringUtils.isNotEmpty(ldapProviderURL) && getParameterList().findParameter("ldapProviderURL")!=null) {
			throw new ConfigurationException("ldapProviderURL can only be specified once, either as attribute or as parameter");
		}
		if (getParameterList().findParameter("principal")==null) {
			throw new ConfigurationException("Parameter 'principal' must be specified");
		}
		if (getParameterList().findParameter("credentials")==null) {
			throw new ConfigurationException("Parameter 'credentials' must be specified");
		}
	}

	/** 
	 * Checks to see if the supplied parameteres of the pipe can login to LDAP 
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(java.lang.Object, nl.nn.adapterframework.core.PipeLineSession)
	 */
	public PipeRunResult doPipe(Object msg, PipeLineSession pls) throws PipeRunException {

		LdapSender ldapSender = new LdapSender();
		
		String ldapProviderURL;
		String credentials;
		String principal;
					
		try {
			ParameterResolutionContext prc = new ParameterResolutionContext((String)msg, pls);
			HashMap paramMap = prc.getValueMap(getParameterList());
			if (StringUtils.isNotEmpty(getLdapProviderURL())) {
				ldapProviderURL = getLdapProviderURL();
			} else {
				ldapProviderURL = (String)paramMap.get("ldapProviderURL");
			}
			credentials = (String)paramMap.get("credentials");
			principal = (String)paramMap.get("principal");
		} catch (ParameterException e) {
			throw new PipeRunException(this, "Invalid parameter", e);
		}
			
		if (StringUtils.isEmpty(ldapProviderURL)) {
			throw new PipeRunException(this, "ldapProviderURL is empty");			
		}
		if (StringUtils.isEmpty(principal)) {
			throw new PipeRunException(this, "principal is empty");			
		}
		if (StringUtils.isEmpty(credentials)) {
			throw new PipeRunException(this, "credentials are empty");			
		}
			
		Parameter dummyEntryName =  new Parameter();
		dummyEntryName.setName("entryName");
		dummyEntryName.setValue(principal);
		ldapSender.addParameter(dummyEntryName);
			
		ldapSender.setUsePooling(false);
		ldapSender.setLdapProviderURL(ldapProviderURL);
		if (StringUtils.isNotEmpty(getInitialContextFactoryName())) {
			ldapSender.setInitialContextFactoryName(getInitialContextFactoryName());
		}
		ldapSender.setPrincipal(principal);
		ldapSender.setCredentials(credentials);
		ldapSender.setOperation(LdapSender.OPERATION_READ);
		try {
			log.debug("Looking up context for principal ["+principal+"]");
			ldapSender.configure();
			log.debug("Succesfully looked up context for principal ["+principal+"]");
		} catch (Exception e) {
			log.warn("LDAP error looking up context for principal ["+principal+"]", e);
			return new PipeRunResult(findForward("invalid"), msg);
		}
						
		return new PipeRunResult(findForward("success"), msg);
	}

	public void setLdapProviderURL(String string) {
		ldapProviderURL = string;
	}
	public String getLdapProviderURL() {
		return ldapProviderURL;
	}

	public void setInitialContextFactoryName(String value) {
		initialContextFactoryName = value;
	}
	public String getInitialContextFactoryName() {
		return initialContextFactoryName;
	}

}
