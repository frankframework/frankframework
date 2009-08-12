/*
 * $Log: CredentialCheckingPipe.java,v $
 * Revision 1.1  2009-08-12 14:15:56  L190409
 * added test for CredentialFactory
 *
 */
package nl.nn.adapterframework.pipes;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.util.CredentialFactory;

/**
 * Pipe to check the the CredentialFactory (for testing only).
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class CredentialCheckingPipe extends FixedForwardPipe {

	private String targetUserid;
	private String targetPassword;
	private String defaultUserid;
	private String defaultPassword;
	private String authAlias;

	public void configure() throws ConfigurationException {
		super.configure();
		if (getTargetUserid()==null) {
			throw new ConfigurationException("targetUserid must be specified");
		} 
		if (getTargetPassword()==null) {
			throw new ConfigurationException("targetPassword must be specified");
		} 
	}


	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		CredentialFactory cf=new CredentialFactory(getAuthAlias(),getDefaultUserid(),getDefaultPassword());
		String result="";
		if (!getTargetUserid().equals(cf.getUsername())) {
			result+="username ["+cf.getUsername()+"] does not match target ["+getTargetUserid()+"]";
		}
		if (!getTargetPassword().equals(cf.getPassword())) {
			result+="password ["+cf.getPassword()+"] does not match target ["+getTargetPassword()+"]";
		}
 		if (StringUtils.isEmpty(result)) {
 			result="OK";
 		}
 		return new PipeRunResult(getForward(),result);
	}

	public void setAuthAlias(String string) {
		authAlias = string;
	}
	public String getAuthAlias() {
		return authAlias;
	}

	public void setTargetPassword(String string) {
		targetPassword = string;
	}
	public String getTargetPassword() {
		return targetPassword;
	}

	public void setTargetUserid(String string) {
		targetUserid = string;
	}
	public String getTargetUserid() {
		return targetUserid;
	}

	public void setDefaultPassword(String string) {
		defaultPassword = string;
	}
	public String getDefaultPassword() {
		return defaultPassword;
	}

	public void setDefaultUserid(String string) {
		defaultUserid = string;
	}
	public String getDefaultUserid() {
		return defaultUserid;
	}


}
