package nl.nn.adapterframework.management.bus;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.messaging.Message;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.util.flow.FlowDiagramManager;

@BusAware("frank-management-bus")
public class RenameMe {
	private @Getter @Setter IbisManager ibisManager;
	private @Setter FlowDiagramManager flowDiagramManager;

	@TopicSelector(BusTopic.CONFIGURATION)
	public Message getXMLConfiguration(Message<?> message) {
		boolean loadedConfiguration = getHeader(message, "loaded", false);
		String result = "";
		for (Configuration configuration : getIbisManager().getConfigurations()) {
			if (loadedConfiguration) {
				result = result + configuration.getLoadedConfiguration();
			} else {
				result = result + configuration.getOriginalConfiguration();
			}
		}
		return ResponseMessage.create(result);
	}

	@TopicSelector(BusTopic.FLOW)
	public Message getApplicationFlow(Message<?> message) throws IOException {
		InputStream configFlow = flowDiagramManager.get(getIbisManager().getConfigurations());
		if(configFlow != null) {
			return ResponseMessage.ok(configFlow, flowDiagramManager.getMediaType());
		} else {
			return ResponseMessage.noContent();
		}
	}

	private boolean containsHeader(Message<?> message, String headerName) {
		return message.getHeaders().get(headerName) != null;
	}

	private boolean getHeader(Message<?> message, String headerName, boolean defaultValue) {
		Object header = message.getHeaders().get(headerName);
		if(header == null) {
			return defaultValue;
		}

		return Boolean.parseBoolean(""+header);
	}
}
