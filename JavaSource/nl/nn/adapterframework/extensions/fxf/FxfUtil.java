/*
 * $Log: FxfUtil.java,v $
 * Revision 1.1  2008-09-04 12:05:25  europe\L190409
 * test for version of fxf
 *
 */
package nl.nn.adapterframework.extensions.fxf;

import org.apache.log4j.Logger;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.ProcessUtil;

/**
 * FXF utility functions.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class FxfUtil {
	protected static Logger log = LogUtil.getLogger(FxfUtil.class);
	
	public static String getVersion(String script) {
		AppConstants ac = AppConstants.getInstance();
		
		String configured=ac.getResolvedProperty("fxf.version");
		String result="unknown";
		
		if ("auto".equalsIgnoreCase(configured)) {
			try {
				String command = script+" version";
				log.debug("checking FXF version by executing command ["+command+"]");
				String execResult=ProcessUtil.executeCommand(command);
				log.debug("output of command ["+execResult+"]");
				result=execResult;
			} catch (SenderException e) {
				log.debug("caught SenderException determining version of FXF: "+e.getMessage());
			} catch (Throwable t) {
				log.debug("caught ["+ClassUtils.nameOf(t)+"] determining version of FXF: "+t.getMessage());
			}
		} else {
			result=configured;
		}
		return result;
	}

}
