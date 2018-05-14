package nl.nn.adapterframework.extensions.cmis;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.extensions.cmis.server.CmisServletDispatcher;
import nl.nn.adapterframework.http.PushingListenerAdapter;
import nl.nn.adapterframework.receivers.ReceiverAware;

public class CmisListener extends PushingListenerAdapter implements HasPhysicalDestination, ReceiverAware {

	List<String> bindingTypes = Arrays.asList("atompub", "webservices", "browser", "all");
	private String bindingType = "all";
	private String bindingVersion = "11";
	private IReceiver receiver = null;

	public void configure() throws ConfigurationException {
		super.configure();

		if (!bindingTypes.contains(getBindingType())) {
			throw new ConfigurationException("illegal value for bindingType ["
					+ getBindingType() + "], must be " + bindingTypes.toString());
		}

		if(getBindingVersion().equalsIgnoreCase("10") && getBindingType().equals("browser")) {
			throw new ConfigurationException("bindingVersion can only be set to [atompub] or [webservices]");
		}
	}

	@Override
	public void open() throws ListenerException {
		super.open();
		try {
			CmisServletDispatcher.getInstance().registerServiceClient(this);
		} catch (ConfigurationException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public void close() {
		super.close();
		CmisServletDispatcher.getInstance().unregisterServiceClient(this);
	}

	@Override
	public String processRequest(String correlationId, String message, Map requestContext) throws ListenerException {
		String result = super.processRequest(correlationId, message, requestContext);
		if(result != null && result.isEmpty())
			return null;
		else
			return result;
	}

	@Override
	public String getPhysicalDestinationName() {
		StringBuilder sb = new StringBuilder("url: /cmis/");

		if(!getBindingType().equals("all")) {
			sb.append(getBindingType());

			if(getBindingVersion().equalsIgnoreCase("10"))
				sb.append("10");

			sb.append("/");
		}
		sb.append("*");

		return sb.toString();
	}

	public void setBindingType(String string) {
		bindingType = string;
	}

	public String getBindingType() {
		if(bindingType != null)
			return bindingType.toLowerCase();

		return null;
	}

	public void setBindingVersion(String version) {
		bindingVersion = version;
	}

	public String getBindingVersion() {
		return bindingVersion;
	}

	@Override
	public void setReceiver(IReceiver receiver) {
		this.receiver = receiver;
	}

	@Override
	public IReceiver getReceiver() {
		return receiver;
	}
}
