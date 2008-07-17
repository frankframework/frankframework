/*
 * $Log: PipeDescriptionImpl.java,v $
 * Revision 1.1  2008-07-17 16:16:26  europe\L190409
 * made PipeDescription an interface
 *
 * Revision 1.1  2008/07/14 17:07:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
public class PipeDescriptionImpl implements PipeDescription {
	
	private String description;
	private List styleSheetsNames = new ArrayList();

	public PipeDescriptionImpl() {
	}

	public void setDescription(String description) {
		this.description = description;
	}
	public String getDescription() {
		return description;
	}

	public void setStyleSheetNames(List styleSheetNames) {
		this.styleSheetsNames=styleSheetNames;
	}

	public List getStyleSheetNames() {
		return styleSheetsNames;
	}

}
