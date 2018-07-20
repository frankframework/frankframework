package nl.nn.ibistesttool.filter;

import java.util.List;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.filter.CheckpointMatcher;
import nl.nn.testtool.util.SearchUtil;

/**
 * The FtpFileRetrieverPipe should be converted to a Sender in the Ibis
 * AdapterFramework. Untill then this class can be used as an additional
 * checkpoint matcher in views that need to display ftp senders. 
 * 
 * @author Jaco de Groot
 */
public class FtpFileRetrieverPipe implements CheckpointMatcher {
	
	public boolean match(Report report, Checkpoint checkpoint) {
		if (
				checkpoint.getName() != null
				&&
				(
					checkpoint.getName().startsWith("Pipe ")
					// Also in stub4testtool.xsl
					&& "nl.nn.adapterframework.ftp.FtpFileRetrieverPipe".equals(checkpoint.getSourceClassName())
					&&
					(
						checkpoint.getType() == Checkpoint.TYPE_STARTPOINT
						|| checkpoint.getType() == Checkpoint.TYPE_ENDPOINT
						|| checkpoint.getType() == Checkpoint.TYPE_ABORTPOINT
					)
				)
			) {
			return true;
		}
		return false;
	}

}
