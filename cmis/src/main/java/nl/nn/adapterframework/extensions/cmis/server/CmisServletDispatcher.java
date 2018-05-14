package nl.nn.adapterframework.extensions.cmis.server;

import org.apache.chemistry.opencmis.commons.spi.CmisBinding;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.extensions.cmis.CmisListener;
import nl.nn.adapterframework.extensions.cmis.CmisSender;

public class CmisServletDispatcher {
	private static CmisServletDispatcher self = null;
	private CmisListener listener = null;
	private CmisSender sender = null;

	public static synchronized CmisServletDispatcher getInstance() {
		if(self == null) {
			self = new CmisServletDispatcher();
		}
		return self;
	}

	public void registerServiceClient(CmisListener cmisListener) throws ConfigurationException {
		if(listener != null)
			throw new ConfigurationException("only one cmisListener can be registered per ibis");

		if(sender == null)
			throw new ConfigurationException("no default cmisSender has been specified");

		this.listener = cmisListener;
	}

	public void registerServiceClient(CmisSender cmisSender) throws ConfigurationException {
		if(listener != null)
			throw new ConfigurationException("only one cmisSender can be registered as default");

		this.sender = cmisSender;
	}

	public void unregisterServiceClient(CmisListener cmisListener) {
		listener = null;
	}

	public void unregisterServiceClient(CmisSender cmisSender) {
		sender = null;
	}

	public CmisListener getCmisListener() {
		return listener;
	}

	public CmisSender getCmisSender() {
		return sender;
	}

	public CmisBinding getCmisBinding() {
		return getCmisSender().getSession().getBinding();
	}
}
