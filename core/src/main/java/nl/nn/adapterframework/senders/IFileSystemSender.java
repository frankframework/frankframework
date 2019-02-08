package nl.nn.adapterframework.senders;

import nl.nn.adapterframework.core.ISenderWithParameters;

public interface IFileSystemSender extends ISenderWithParameters {

	public String getAction();

	public void setAction(String action);
}
