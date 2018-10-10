/*
   Copyright 2018 Nationale-Nederlanden

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
package nl.nn.ibistesttool.metadata;

import java.util.List;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.metadata.DefaultValueMetadataFieldExtractor;

/**
 * @author Jaco de Groot
 */
public class StatusExtractor extends DefaultValueMetadataFieldExtractor {
	
	public StatusExtractor() {
		name = "status";
		label = "Status";
	}

	public Object extractMetadata(Report report) {
		String status = "Success";
		List checkpoints = report.getCheckpoints();
		if (checkpoints.size() > 0) {
			Checkpoint lastCheckpoint = (Checkpoint)checkpoints.get(checkpoints.size() - 1);
			if (lastCheckpoint.getType() == Checkpoint.TYPE_ABORTPOINT) {
				status = "Error";
			}
		}
		return status;
	}

}
