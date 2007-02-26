/*
 * $Log: LdapChallengePipe.java,v $
 * Revision 1.1  2007-02-26 15:56:37  europe\L190409
 * update of LDAP code, after a snapshot from Ibis4Toegang
 *
 * Revision 1.3  2007/02/21 17:22:55  europe\m719590
 * cosmetic change
 *
 * Revision 1.2  2007/02/19 16:29:01  europe\m719590
 * ldapProviderURL in Ldapsender aangepast
 *
 * Revision 1.1  2006/06/20 09:32:03  Peter Eijgermans <peter.eijgermans@ibissource.org>
 * LdapChalengePipe
 *
 *
 */
package nl.nn.adapterframework.ldap;

import java.util.HashMap;

import javax.naming.directory.DirContext;

import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.pipes.AbstractPipe;

import org.apache.commons.lang.StringUtils;

/**
 * Pipe to check if a username and password are valid in LDAP.
 * 
 * @author  Milan Tomc
 * @version $RCSfile: LdapChallengePipe.java,v $  $Revision: 1.1 $ $Date: 2007-02-26 15:56:37 $"
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.ldap.LdapChalengePipe</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>ldapProviderURL</td><td>URL to the LDAP server. <br/>Example: ldap://su05b9.itc.intranet</td><td>Requered and must be filled</td></tr>
 * <tr><td>credentials</td><td>The LDAP password. <br/> Example: welkom01</td><td>Requered and must be filled</td></tr>
 * <tr><td>principal</td><td>The LDAP DN for the username. <br/>Example: UID=SRP,OU=DI-IUF-EP,OU=SERVICES,O=ING</td><td>Requered and must be filled</td></tr>
 * <tr><td>initialContextFactoryName</td><td>The factory which implements the LDAP DIRContext. <br/>Example: com.sun.jndi.ldap.LdapCtxFactory</td><td>Requered and must be filled</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th><th>remarks</th></tr>
 * <tr><td>success</td><td>Successful login to LDAP</td><td>should be defined in configuration</td></tr>
 * <tr><td>invalid</td><td>Unsuccessful login to LDAP</td><td>should be defined in configuration</td></tr>
 * <tr><td>error</td><td>Exception occured in the pipe</td></tr>
 * </table>
 * </p>
 */
public class LdapChallengePipe extends AbstractPipe {
	public static final String version = "$RCSfile: LdapChallengePipe.java,v $  $Revision: 1.1 $ $Date: 2007-02-26 15:56:37 $";

	/** 
	 * Checks to see if the supplied parameteres of the pipe can login to LDAP 
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(java.lang.Object, nl.nn.adapterframework.core.PipeLineSession)
	 */
	public PipeRunResult doPipe(Object msg, PipeLineSession pls) throws PipeRunException {

		LdapSender ldapSender = new LdapSender();
		
		String ldapProviderURL;
		String credentials;
		String principal;
		String initialContextFactoryName;
					
		try {
			ParameterResolutionContext prc = new ParameterResolutionContext((String)msg, pls);
			HashMap paramMap = prc.getValueMap(getParameterList());
			ldapProviderURL = (String)paramMap.get("ldapProviderURL");
			credentials = (String)paramMap.get("credentials");
			principal = (String)paramMap.get("principal");
			initialContextFactoryName = (String)paramMap.get("initialContextFactoryName");
		} catch (ParameterException e) {
			throw new PipeRunException(this, "Invalid parameter", e);
		}
			
		if (StringUtils.isEmpty(ldapProviderURL) ||
		    StringUtils.isEmpty(credentials) || 
			StringUtils.isEmpty(principal) ||
			StringUtils.isEmpty(ldapProviderURL)
			) {
			String paramfields = "ldapProviderURL: " + ldapProviderURL;
			paramfields = paramfields + " credentials: " + credentials;
			paramfields = paramfields + " principal: " + principal;
			paramfields = paramfields + " ldapProviderURL " + ldapProviderURL;   	
			throw new PipeRunException(this, "One of the following required parameters isEmpty: " + paramfields);			
		}
			
		ldapSender.setLdapProviderURL(ldapProviderURL);
		ldapSender.setInitialContextFactoryName(initialContextFactoryName);
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


}
