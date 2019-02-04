package nl.nn.adapterframework.filesystem;

import nl.nn.adapterframework.core.ISenderWithParameters;

public interface ISambaSender extends ISenderWithParameters {
	public void setAction(String action);

	public String getAction();
}
