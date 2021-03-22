package nl.nn.adapterframework.configuration;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.util.LogUtil;

public abstract class AdapterProcessor {
	protected final Logger log = LogUtil.getLogger(this);

	public abstract void addAdapter(Adapter adapter);

	public abstract void removeAdapter(Adapter adapter);
}
