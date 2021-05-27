package nl.nn.adapterframework.testutil;

import java.io.IOException;
import java.io.OutputStream;

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
	public void setFileExtension(String extension) {
		//Ignore
	}

	@Override
	public String getFileExtension() {
		return null;
	}

	@Override
	public void generateFlow(String name, String dot, OutputStream outputStream) throws IOException {
		//Ignore
	}

}
