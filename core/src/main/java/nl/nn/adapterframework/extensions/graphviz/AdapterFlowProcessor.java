package nl.nn.adapterframework.extensions.graphviz;

import nl.nn.adapterframework.configuration.AdapterProcessor;
import nl.nn.adapterframework.core.Adapter;

public class AdapterFlowProcessor extends AdapterProcessor {

	@Override
	public void addAdapter(Adapter adapter) {
		System.out.println("flow yo");
	}

	@Override
	public void removeAdapter(Adapter adapter) {
		// TODO Auto-generated method stub
		
	}

}
