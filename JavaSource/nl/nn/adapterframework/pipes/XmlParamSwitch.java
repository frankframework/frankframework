/*
 * $Log: XmlParamSwitch.java,v $
 * Revision 1.6  2004-10-05 10:54:01  L190409
 * moved all functionality to XmlSwitch
 *
 */
package nl.nn.adapterframework.pipes;

/**
 * Extension of the {@link XmlSwitch XmlSwitch}: an xml switch that can use parameters. The parameters
 * will be used to set them on the transformer instance.
 * @author Johan Verrips
 * @version Id
 * @deprecated please use plain XmlSwitch, that now supports parameters too (since 4.2d)
 */
public class XmlParamSwitch extends XmlSwitch {
	public static final String version="$Id: XmlParamSwitch.java,v 1.6 2004-10-05 10:54:01 L190409 Exp $";
	
}
