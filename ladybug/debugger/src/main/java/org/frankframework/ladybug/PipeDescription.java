/*
   Copyright 2018 Nationale-Nederlanden, 2023 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.ladybug;

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
	private final List<String> resourcesNames = new ArrayList<>();

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

	public boolean doesNotContainResourceName(String resourceName) {
		return !resourcesNames.contains(resourceName);
	}

	public List<String> getResourceNames() {
		return resourcesNames;
	}

}
