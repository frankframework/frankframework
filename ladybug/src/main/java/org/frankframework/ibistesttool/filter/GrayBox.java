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
package org.frankframework.ibistesttool.filter;

import java.util.List;
import java.util.ListIterator;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;

/**
 * Only show senders and pipelines (within these senders) (show only the pipeline checkpoints, not it's children)
 *
 * @author Jaco de Groot
 */
public class GrayBox extends AbstractBox {
	@Override
	public boolean match(Report report, Checkpoint checkpoint) {
		if (checkpoint.getType() == Checkpoint.TYPE_INPUTPOINT || checkpoint.getType() == Checkpoint.TYPE_OUTPUTPOINT
				|| checkpoint.getType() == Checkpoint.TYPE_INFOPOINT) {
			if (hasStartPointOnLevel(report, checkpoint)) {
				return false;
			}
			List<Checkpoint> checkpoints = report.getCheckpoints();
			ListIterator<Checkpoint> iterator = report.getCheckpoints().listIterator(checkpoints.indexOf(checkpoint));
			while (iterator.hasPrevious()) {
				Checkpoint previous = iterator.previous();
				if (previous.getType() == Checkpoint.TYPE_STARTPOINT && previous.getLevel() < checkpoint.getLevel()) {
					return isSender(previous);
				}
			}
			return false;
		} else {
			return isSenderOrPipelineOrFirstOrLastCheckpoint(report, checkpoint);
		}
	}

	public boolean hasStartPointOnLevel(Report report, Checkpoint checkpoint) {
		List<Checkpoint> checkpoints = report.getCheckpoints();
		ListIterator<Checkpoint> iterator = report.getCheckpoints().listIterator(checkpoints.indexOf(checkpoint));
		int currentLevel = checkpoint.getLevel();
		while (iterator.hasNext()) {
			Checkpoint nextCheckpoint = iterator.next();
			if (nextCheckpoint.getLevel() < currentLevel || nextCheckpoint.getType() == Checkpoint.TYPE_ENDPOINT) {
				break;
			}
			if (nextCheckpoint.getType() == Checkpoint.TYPE_STARTPOINT) {
				return true;
			}
		}
		while (iterator.hasPrevious()) {
			Checkpoint previousCheckpoint = iterator.previous();
			if (previousCheckpoint.getLevel() < currentLevel) {
				break;
			}
			if (previousCheckpoint.getType() == Checkpoint.TYPE_STARTPOINT) {
				return true;
			}
		}
		return false;
	}
}
