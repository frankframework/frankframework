/*
 * $Log: IsUserInRolePipe.java,v $
 * Revision 1.1  2005-07-05 13:20:06  europe\L190409
 * introduction of SecurityHandlers
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

import org.apache.commons.lang.StringUtils;

/**
 * Pipe that checks if the calling user has a specified role. 
 * Uses the PipeLineSessions methods.
 * 
 * @author  Gerrit van Brakel
 * @since   4.3
 * @version Id
 */
public class IsUserInRolePipe extends FixedForwardPipe {
	public static final String version = "$RCSfile: IsUserInRolePipe.java,v $ $Revision: 1.1 $ $Date: 2005-07-05 13:20:06 $";

	private String role=null;
	private String notInRoleForwardName=null;
	protected PipeForward notInRoleForward;
	
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getRole())) {
			throw new ConfigurationException("must specify attribute role");
		}
		if (StringUtils.isNotEmpty(getNotInRoleForwardName())) {
			notInRoleForward = findForward(getNotInRoleForwardName());
			if  (notInRoleForward==null) {
				throw new ConfigurationException("notInRoleForwardName ["+getNotInRoleForwardName()+"] not found");
			}
		}
	}
	
	protected void assertUserIsInRole(PipeLineSession session) throws SecurityException {
		if (!session.isUserInRole(getRole())) {
			throw new SecurityException(getLogPrefix(session)+"user is not in role ["+getRole()+"]");
		}
	}
	
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		try {
			assertUserIsInRole(session);
		} catch (SecurityException e) {
			if (notInRoleForward!=null) {
				return new PipeRunResult(notInRoleForward, input);
			} else {
				throw new PipeRunException(this,"",e);
			}
		}
		return super.doPipe(input, session);
	}
	
	public String getRole() {
		return role;
	}
	public void setRole(String string) {
		role = string;
	}

	public String getNotInRoleForwardName() {
		return notInRoleForwardName;
	}
	public void setNotInRoleForwardName(String string) {
		notInRoleForwardName = string;
	}

}
