/*
 * $Log: Transfer.java,v $
 * Revision 1.1  2009-03-04 15:56:57  L190409
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
