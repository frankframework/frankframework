/*
 * $Log: XmlParamSwitch.java,v $
 * Revision 1.7  2008-12-30 17:01:12  m168309
 * added configuration warnings facility (in Show configurationStatus)
 *
 * Revision 1.6  2004/10/05 10:54:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved all functionality to XmlSwitch
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;

/**
 * Extension of the {@link XmlSwitch XmlSwitch}: an xml switch that can use parameters. The parameters
 * will be used to set them on the transformer instance.
 * @author Johan Verrips
 * @version Id
 * @deprecated please use plain XmlSwitch, that now supports parameters too (since 4.2d)
 */
public class XmlParamSwitch extends XmlSwitch {
	public static final String version="$Id: XmlParamSwitch.java,v 1.7 2008-12-30 17:01:12 m168309 Exp $";
	
	public void configure() throws ConfigurationException {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = getLogPrefix(null)+"The class ["+getClass().getName()+"] has been deprecated. Please change to ["+getClass().getSuperclass().getName()+"]";
		configWarnings.add(log, msg);
		super.configure();
	}
}
