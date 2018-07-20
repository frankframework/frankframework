package nl.nn.ibistesttool;

import java.util.ArrayList;
import java.util.List;

/**
 * Description of a pipe. The description contains the XML configuration for the
 * pipe. Optionally a list of the XSLT files used by the pipe can be retrieved.
 *
 * @author Jaco de Groot (jaco@dynasol.nl)
 */
public class PipeDescription {
	private String checkpointName;
	private String description;
	private List<String> resourcesNames = new ArrayList<String>();

	public PipeDescription() {
	}

	public void setCheckpointName(String checkpointName) {
		this.checkpointName = checkpointName;
	}

	public String getCheckpointName() {
		return checkpointName;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public void addResourceName(String resourceName) {
		resourcesNames.add(resourceName);
	}

	public boolean containsResourceName(String resourceName) {
		return resourcesNames.contains(resourceName);
	}

	public List<String> getResourceNames() {
		return resourcesNames;
	}

}
