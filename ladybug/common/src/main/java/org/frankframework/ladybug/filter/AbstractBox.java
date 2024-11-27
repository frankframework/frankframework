/*
   Copyright 2023-2024 WeAreFrank!, 2018 Nationale-Nederlanden

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
package org.frankframework.ladybug.filter;

import java.util.List;
import java.util.ListIterator;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.CheckpointType;
import nl.nn.testtool.Report;
import nl.nn.testtool.filter.CheckpointMatcher;

public abstract class AbstractBox implements CheckpointMatcher {
	public boolean match(Report report, Checkpoint checkpoint) {
		if (checkpoint.getType() == CheckpointType.INPUTPOINT.toInt()
				|| checkpoint.getType() == CheckpointType.OUTPUTPOINT.toInt()
				|| checkpoint.getType() == CheckpointType.INFOPOINT.toInt()) {
			List<Checkpoint> checkpoints = report.getCheckpoints();
			ListIterator<Checkpoint> iterator = report.getCheckpoints().listIterator(checkpoints.indexOf(checkpoint));
			while (iterator.hasPrevious()) {
				Checkpoint previous = iterator.previous();
				if (previous.getType() == CheckpointType.STARTPOINT.toInt()
						&& previous.getLevel() < checkpoint.getLevel()) {
					return isSender(previous);
				}
			}
			return false;
		} else {
			return isSenderOrPipelineOrFirstOrLastCheckpoint(report, checkpoint);
		}
	}

	protected boolean isSender(Checkpoint checkpoint) {
		return checkpoint.getName() != null && checkpoint.getName().startsWith("Sender ");
	}

	protected boolean isPipeline(Checkpoint checkpoint) {
		return checkpoint.getName() != null && checkpoint.getName().startsWith("Pipeline ");
	}

	protected boolean isSenderOrPipelineOrFirstOrLastCheckpoint(Report report, Checkpoint checkpoint) {
		return isSenderOrPipeline(checkpoint) || isFirstOrLastCheckpoint(report, checkpoint);
	}

	protected boolean isSenderOrPipeline(Checkpoint checkpoint) {
		return isSender(checkpoint) || isPipeline(checkpoint);
	}

	protected boolean isFirstOrLastCheckpoint(Report report, Checkpoint checkpoint) {
		List<Checkpoint> checkpoints = report.getCheckpoints();
		if (!checkpoints.isEmpty()) {
			Checkpoint firstCheckpoint = checkpoints.get(0);
			if (checkpoint.equals(firstCheckpoint)) {
				return true;
			}
			Checkpoint lastCheckpoint = checkpoints.get(checkpoints.size() - 1);
			return checkpoint.equals(lastCheckpoint);
		}
		return false;
	}
}
