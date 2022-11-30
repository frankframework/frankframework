/*
Copyright 2016-2017, 2019-2021 WeAreFrank!

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package nl.nn.adapterframework.webcontrol.api;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.http.WebServiceListener;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.RequestMessageBuilder;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.soap.WsdlGenerator;

/**
 * Shows all monitors.
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public final class Webservices extends Base {

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/webservices")
	@Relation("webservices")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getWebServices() throws ApiException {
		return callSyncGateway(RequestMessageBuilder.create(this, BusTopic.WEBSERVICES));
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/webservices/{resourceName}")
	@Relation("webservices")
	@Produces(MediaType.APPLICATION_XML)
	public Response getWsdl(
		@PathParam("resourceName") String resourceName,
		@DefaultValue("true") @QueryParam("indent") boolean indent,
		@DefaultValue("false") @QueryParam("useIncludes") boolean useIncludes) throws ApiException {

		String adapterName;
		boolean zip;
		int dotPos=resourceName.lastIndexOf('.');
		if (dotPos>=0) {
			adapterName=resourceName.substring(0,dotPos);
			zip=resourceName.substring(dotPos).equals(".zip");
		} else {
			adapterName=resourceName;
			zip=false;
		}

		if (StringUtils.isEmpty(adapterName)) {
			return Response.status(Response.Status.BAD_REQUEST).entity("<error>no adapter specified</error>").build();
		}
		IAdapter adapter = getIbisManager().getRegisteredAdapter(adapterName);
		if (adapter == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity("<error>adapter not found</error>").build();
		}
		try {
			String servletName = getServiceEndpoint(adapter);
			String generationInfo = "by FrankConsole";
			WsdlGenerator wsdl = new WsdlGenerator(adapter.getPipeLine(), generationInfo);
			wsdl.setIndent(indent);
			wsdl.setUseIncludes(useIncludes||zip);
			wsdl.init();
			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream out) throws IOException, WebApplicationException {
					try {
						if (zip) {
							wsdl.zip(out, servletName);
						} else {
							wsdl.wsdl(out, servletName);
						}
					} catch (ConfigurationException | XMLStreamException e) {
						throw new WebApplicationException(e);
					}
				}
			};
			ResponseBuilder responseBuilder = Response.ok(stream);
			if (zip) {
				responseBuilder.type(MediaType.APPLICATION_OCTET_STREAM);
				responseBuilder.header("Content-Disposition", "attachment; filename=\""+adapterName+".zip\"");
			}
			return responseBuilder.build();

		} catch (Exception e) {
			throw new ApiException("exception on retrieving wsdl", e);
		}
	}

	private String getServiceEndpoint(IAdapter adapter) {
		String endpoint = "external address of ibis";
		Iterator it = adapter.getReceivers().iterator();
		while(it.hasNext()) {
			IListener listener = ((Receiver) it.next()).getListener();
			if(listener instanceof WebServiceListener) {
				String address = ((WebServiceListener) listener).getAddress();
				if(StringUtils.isNotEmpty(address)) {
					endpoint = address;
				} else {
					endpoint = "rpcrouter";
				}
				String protocol = servletRequest.isSecure() ? "https://" : "http://";
				int port = servletRequest.getServerPort();
				String restBaseUrl = protocol + servletRequest.getServerName() + (port != 0 ? ":" + port : "") + servletRequest.getContextPath() + "/services/";
				endpoint = restBaseUrl + endpoint;
				break;	//what if there are more than 1 WebServiceListener
			}
		}
		return endpoint;
	}

	private String getWsdlExtension() {
		return ".wsdl";
	}
}
