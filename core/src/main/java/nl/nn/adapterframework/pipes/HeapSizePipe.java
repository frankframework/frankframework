package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.ProcessMetrics;

public class HeapSizePipe extends FixedForwardPipe {

	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {	
		return new PipeRunResult(getForward(), ProcessMetrics.toXml());
	}

}