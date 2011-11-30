/*
 * $Log: Transfer.java,v $
 * Revision 1.3  2011-11-30 13:51:51  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2009/03/04 15:56:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for FXF 2.0
 *
 */
package nl.nn.adapterframework.extensions.fxf;

/**
 * Placeholder for Transfer-information of Trigger message.
 * 
 * @author  Gerrit van Brakel
 * @since   FXF 2.0
 * @version Id
 */
public class Transfer {
	
	private String name;
	private String filename;

	public String getName() {
		return name;
	}
	public void setName(String string) {
		name = string;
	}

	public void setFilename(String string) {
		filename = string;
	}
	public String getFilename() {
		return filename;
	}

}
