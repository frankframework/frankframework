package nl.nn.adapterframework.management.bus;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.messaging.Message;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.util.flow.FlowDiagramManager;

@BusAware("frank-management-bus")
public class RenameMe implements InitializingBean {
	private @Getter @Setter IbisManager ibisManager;
	private @Setter FlowDiagramManager flowDiagramManager;

	@HeaderSelector(BusTopic.CONFIGURATION)
	public Message getXMLConfiguration(Message<?> message) {
		boolean loadedConfiguration = false;//Boolean.parseBoolean(message.getHeaders().get("loaded"));
		String result = "";
		for (Configuration configuration : getIbisManager().getConfigurations()) {
			if (loadedConfiguration) {
				result = result + configuration.getLoadedConfiguration();
			} else {
				result = result + configuration.getOriginalConfiguration();
			}
		}
		return ResponseMessage.create(result);
//		return Response.status(Response.Status.OK).entity(result).build();
	}

	@HeaderSelector(BusTopic.CONFIGURATION)
	public Message getApplicationFlow() throws IOException {
		ResponseBuilder response;
		InputStream configFlow = flowDiagramManager.get(getIbisManager().getConfigurations());
		if(configFlow != null) {
			response = Response.ok(configFlow, flowDiagramManager.getMediaType());
		} else {
			response = Response.noContent();
		}
		return ResponseMessage.create(response.build());
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		System.out.println("halli hallo dit werkt!");
	}
}
