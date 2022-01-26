/*
   Copyright 2021 WeAreFrank!

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
import java.net.URL;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * @author	Niels Meijer
 */

@Path("frankdoc/files/")
public final class FrankDoc extends Base {

	public static final String FRANKDOC_JSON = "js/frankdoc.json";
	public static final String FRANKDOC_XSD  = "xml/xsd/FrankConfig-strict.xsd";

	@GET
	@PermitAll
	@Path("frankdoc.json")
	@Produces(MediaType.APPLICATION_JSON)
	public Response fetchFrankDocJSON() throws ApiException, IOException {
		URL frankDoc = ClassUtils.getResourceURL(FRANKDOC_JSON);
		if(frankDoc != null) {
			return Response.status(Response.Status.OK).entity(Message.asInputStream(frankDoc)).build();
		}

		throw new ApiException("Frank!Doc JSON not found", Response.Status.NOT_FOUND);
	}

	@GET
	@PermitAll
	@Path("frankConfig.xsd")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response fetchFrankDocXSD() throws ApiException, IOException {
		URL frankDoc = ClassUtils.getResourceURL(FRANKDOC_XSD);
		if(frankDoc != null) {
			String applicationVersion = AppConstants.getInstance().getProperty("application.version");
			String xsdName = String.format("FrankFramework%s.xsd", applicationVersion!=null ? "-"+applicationVersion : "");
			return Response.status(Response.Status.OK)
					.entity(Message.asInputStream(frankDoc))
					.header("Content-Disposition", "attachment; filename=\"" + xsdName + "\"")
					.header("Content-Transfer-Encoding", "binary")
					.build();
		}

		throw new ApiException("Frank!Config XSD not found", Response.Status.NOT_FOUND);
	}
}