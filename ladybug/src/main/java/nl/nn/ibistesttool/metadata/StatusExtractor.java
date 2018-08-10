package nl.nn.ibistesttool.metadata;

import java.util.List;
import java.util.Map;

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
