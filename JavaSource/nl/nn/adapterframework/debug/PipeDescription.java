/*
 * $Log: PipeDescription.java,v $
 * Revision 1.1  2008-07-14 17:07:32  europe\L190409
 * first version of debugger
 *
 */
package nl.nn.adapterframework.debug;

import java.util.ArrayList;
import java.util.List;

/**
 * Description of a pipe. The description contains the XML configuration for the
 * pipe. Optionally a list of the XSLT files used by the pipe can be retrieved.
 *
 * @author  Jaco de Groot (***@dynasol.nl)
 * @since   4.9
 * @version Id
 */
public class PipeDescription {
	
	private String description;
	private List styleSheetsNames = new ArrayList();

	public PipeDescription() {
	}

	public void setDescription(String description) {
		this.description = description;
	}
	public String getDescription() {
		return description;
	}

	public void addStyleSheetName(String styleSheetName) {
		styleSheetsNames.add(styleSheetName);
	}

	public List getStyleSheetNames() {
		return styleSheetsNames;
	}

}
