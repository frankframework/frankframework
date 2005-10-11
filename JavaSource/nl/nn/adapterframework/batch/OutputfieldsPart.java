/*
 * $Log: OutputfieldsPart.java,v $
 * Revision 1.1  2005-10-11 13:00:21  europe\m00f531
 * New ibis file related elements, such as DirectoryListener, MoveFilePie and 
 * BatchFileTransformerPipe
 *
 */
package nl.nn.adapterframework.batch;

/**
 * @author John Dekker
 */
public class OutputfieldsPart {
	public static final String version = "$RCSfile: OutputfieldsPart.java,v $  $Revision: 1.1 $ $Date: 2005-10-11 13:00:21 $";
	private String value;
	private String description;
	
	public String getValue() {
		return value;
	}

	public void setValue(String string) {
		value = string;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String string) {
		description = string;
	}

}
