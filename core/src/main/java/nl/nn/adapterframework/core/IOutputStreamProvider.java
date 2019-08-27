package nl.nn.adapterframework.core;

import java.io.OutputStream;

import nl.nn.adapterframework.doc.IbisDoc;

public interface IOutputStreamProvider {

	/**
	 * When set, the pipe will provide an {@link OutputStream} in this session variable, that the next pipe can use to write it's output to.
	 */
	@IbisDoc({"When set, an {@link OutputStream} will be provided in this session variable, that the next pipe can use to write it's output to.", ""})
	public void setCreateStreamSessionKey(String createStreamSessionKey);
	public String getCreateStreamSessionKey();

	/*
	 * When this returns true AND createStreamSessionKey is set 
	 * then doPipe must return an OutputStream in this sessionKey.
	 */
	public boolean canProvideOutputStream();  

}
