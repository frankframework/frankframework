/*
 * $Log: LdapSender.java,v $
 * Revision 1.1  2005-01-11 08:00:24  L190409
 * first preliminary version
 *
 */
package nl.nn.adapterframework.ldap;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.jms.JNDIBase;

/**
 * 
 * 
 * @author Gerrit van Brakel
 * @version Id
 */
public class LdapSender extends JNDIBase implements ISender {
	protected Logger log=Logger.getLogger(this.getClass());
	
	private static final String DEFAULT_INITIAL_CONTEXT_FACTORY="com.sun.jndi.ldap.LdapCtxFactory";
	
	
	private String name;
	
	private DirContext dirContext=null;
	
	public LdapSender() {
		super();
		setInitialContextFactoryName(DEFAULT_INITIAL_CONTEXT_FACTORY);
	}
	
	public DirContext getDirContext() throws SenderException {

		try {
			if (null == dirContext) {
				if (getInitialContextFactoryName() != null) {
					dirContext = (DirContext) new InitialDirContext(getJndiEnv());
				} else {
					dirContext = (DirContext) new InitialDirContext();
				}
			}
			log.debug("obtained InitialDirContext ["+ToStringBuilder.reflectionToString(dirContext)+"]");
			return dirContext;
		} catch (NamingException e) {
			throw new SenderException("cannot create InitialDirContext");
		}
	}
	
	public void configure() throws ConfigurationException {
	}

	public void open() throws SenderException {
			getDirContext();
	}

	public void close() throws SenderException {
		// TODO Auto-generated method stub

	}

	public boolean isSynchronous() {
		return true;
	}

	public String sendMessage(String correlationID, String message)
		throws SenderException, TimeOutException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setName(String name) {
		this.name=name;
	}
	public String getName() {
		return name;
	}


}
