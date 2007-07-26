/*
 * $Log: InputfieldsPart.java,v $
 * Revision 1.4  2007-07-26 16:08:48  europe\L190409
 * cosmetic changes
 *
 * Revision 1.3  2006/05/19 09:28:36  Peter Eijgermans <peter.eijgermans@ibissource.org>
 * Restore java files from batch package after unwanted deletion.
 *
 * Revision 1.1  2005/10/11 13:00:20  John Dekker <john.dekker@ibissource.org>
 * New ibis file related elements, such as DirectoryListener, MoveFilePie and 
 * BatchFileTransformerPipe
 *
 */
package nl.nn.adapterframework.batch;

/**
 * @author  John Dekker
 * @version Id
 */
public class InputfieldsPart {
	public static final String version = "$RCSfile: InputfieldsPart.java,v $  $Revision: 1.4 $ $Date: 2007-07-26 16:08:48 $";

	private String value;
	private String description;
	

	public void setValue(String string) {
		value = string;
	}
	public String getValue() {
		return value;
	}


	public void setDescription(String string) {
		description = string;
	}
	public String getDescription() {
		return description;
	}

}
