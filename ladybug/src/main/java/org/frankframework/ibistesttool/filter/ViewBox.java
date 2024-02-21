package org.frankframework.ibistesttool.filter;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.filter.CheckpointMatcher;

import java.util.List;
import java.util.ListIterator;

public abstract class ViewBox implements CheckpointMatcher {
	public boolean match(Report report, Checkpoint checkpoint) {
		if (checkpoint.getType() == Checkpoint.TYPE_INPUTPOINT || checkpoint.getType() == Checkpoint.TYPE_OUTPUTPOINT
				|| checkpoint.getType() == Checkpoint.TYPE_INFOPOINT) {
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
