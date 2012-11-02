/*
 * $Log: FxfPropertyPlaceholderConfigurer.java,v $
 * Revision 1.3  2012-11-02 09:28:33  m00f069
 * Changed log level from warn to debug when FxF directory not found (valid in case FxF isn't used)
 *
 * Revision 1.2  2012/09/07 13:15:17  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Messaging related changes:
 * - Use CACHE_CONSUMER by default for ESB RR
 * - Don't use JMSXDeliveryCount to determine whether message has already been processed
 * - Added maxDeliveries
 * - Delay wasn't increased when unable to write to error store (it was reset on every new try)
 * - Don't call session.rollback() when isTransacted() (it was also called in afterMessageProcessed when message was moved to error store)
 * - Some cleaning along the way like making some synchronized statements unnecessary
 * - Made BTM and ActiveMQ work for testing purposes
 *
 */
package nl.nn.adapterframework.extensions.fxf;

import java.io.File;
import java.util.Properties;

import nl.nn.adapterframework.configuration.AppConstantsPropertyPlaceholderConfigurer;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * Initialise the fxf.dir property when not already available from the
 * AppConstants and make it available to the Ibis configuration and the Spring
 * configuration.
 * 
 * @author Jaco de Groot
 */
public class FxfPropertyPlaceholderConfigurer
		extends AppConstantsPropertyPlaceholderConfigurer {
	protected Logger log = LogUtil.getLogger(this);

	protected void convertProperties(Properties props) {
		String fxfDir = appConstants.getResolvedProperty("fxf.dir");
		if (fxfDir == null) {
			// Use default location, see was.policy too
			fxfDir = System.getProperty("APPSERVER_ROOT_DIR");
			if (fxfDir != null) {
				fxfDir = fxfDir + File.separator + "fxf-work";
			}
		}
		if (fxfDir != null) {
			if (!new File(fxfDir).isDirectory()) {
				log.debug("Could not find FxF directory: " + fxfDir);
				fxfDir = null;
			} else {
				log.debug("FxF directory: " + fxfDir);
				appConstants.setProperty("fxf.dir", fxfDir);
				props.put("fxf.dir", fxfDir);
			}
		}
	}

}
