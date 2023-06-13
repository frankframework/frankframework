/*
   Copyright 2023 WeAreFrank!

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
package nl.nn.ibistesttool.filter;

import java.util.List;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.filter.CheckpointMatcher;

/**
 * @author Jaco de Groot
 */
public class GrayBox implements CheckpointMatcher {

	public boolean match(Report report, Checkpoint checkpoint) {
		if (checkpoint.getType() == Checkpoint.TYPE_INPUTPOINT || checkpoint.getType() == Checkpoint.TYPE_OUTPUTPOINT
				 || checkpoint.getType() == Checkpoint.TYPE_INFOPOINT) {
			List<Checkpoint> checkpoints = report.getCheckpoints();
			for (int i = checkpoints.indexOf(checkpoint) - 1; i > 1; i--) {
				if (checkpoints.get(i).getType() == Checkpoint.TYPE_STARTPOINT) {
					return match(report, checkpoints.get(i));
				}
			}
		} else {
			if (match(checkpoint)) {
				return true;
			} else {
				List<Checkpoint> checkpoints = report.getCheckpoints();
				if (!checkpoints.isEmpty()) {
					Checkpoint firstCheckpoint = checkpoints.get(0);
					if (checkpoint.equals(firstCheckpoint)) {
						return true;
					} else {
						Checkpoint lastCheckpoint = (Checkpoint)checkpoints.get(checkpoints.size() - 1);
						if (checkpoint.equals(lastCheckpoint)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	protected boolean match(Checkpoint checkpoint) {
		if (checkpoint.getName() != null &&
				(checkpoint.getName().startsWith("Sender ") || checkpoint.getName().startsWith("Pipeline "))) {
			return true;
		}
		return false;
	}
}
