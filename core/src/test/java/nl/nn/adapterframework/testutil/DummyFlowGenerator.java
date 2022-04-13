package nl.nn.adapterframework.testutil;

import java.io.OutputStream;

import org.springframework.http.MediaType;

import nl.nn.adapterframework.util.flow.FlowGenerationException;
import nl.nn.adapterframework.util.flow.IFlowGenerator;

/**
 * Placeholder class to help initialize the FlowDiagramManager.
 */
public class DummyFlowGenerator implements IFlowGenerator {

	@Override
	public void afterPropertiesSet() throws Exception {
		//Ignore
	}

	@Override
	public void destroy() throws Exception {
		//Ignore
	}

	@Override
	public String getFileExtension() {
		return null;
	}

	@Override
	public void generateFlow(String dot, OutputStream outputStream) throws FlowGenerationException {
		//Ignore
	}

	@Override
	public MediaType getMediaType() {
		return null;
	}

}
