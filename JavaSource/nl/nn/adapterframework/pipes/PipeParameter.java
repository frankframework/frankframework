package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeLineSession;

/**
 * Generic parameter for Pipes.
 * @author Richard Punt
 * @version Id
 */
public class PipeParameter {

	public PipeParameter() {
		super();
	}

	private String name = null;
	private String defaultValue = null;
	private String sessionKey = null;

	public void setName(String rhs) {
		name = rhs;
	}

	public String getName() {
		return name;
	}

	public void setDefaultValue(String rhs) {
		defaultValue = rhs;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setSessionKey(String rhs) {
		sessionKey = rhs;
	}

	public String getSessionKey() {
		return sessionKey;
	}

	/**
	 * returns the value of the parameter. If the session key can be resolved, the value of the session
	 * key will be returned. Otherwise the DefaultValue will be returned. 
	 * @param session
	 * @return the value
	 */
	public String getValue(PipeLineSession session) {
		String result=null;
		if (getSessionKey() != null) {
				result= (String) session.get(getSessionKey());
			}
		if (result==null) result=getDefaultValue();
		
		return result;
				
	}



	public String toString() {
		return "name=["+name+"] defaultValue=["+defaultValue+"] sessionKey=["+sessionKey+"]";

	}

}
