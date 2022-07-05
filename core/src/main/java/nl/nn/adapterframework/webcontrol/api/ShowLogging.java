/*
Copyright 2016-2022 WeAreFrank!

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
import java.util.HashMap;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.Dir2Map;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.webcontrol.FileViewerServlet;

/**
 * Shows directory of logfiles
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public class ShowLogging { // ShowLogging does not extend Base, to enable log analysis of application startup problems
	@Context HttpServletRequest servletRequest;

	boolean showDirectories = AppConstants.getInstance().getBoolean("logging.showdirectories", false);
	int maxItems = AppConstants.getInstance().getInt("logging.items.max", 500);

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/logging")
	@Relation("logging")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLogDirectory(
			@QueryParam("directory") String directory, 
			@QueryParam("sizeFormat") String sizeFormatParam, 
			@QueryParam("wildcard") String wildcard
			) throws ApiException {

		Map<String, Object> returnMap = new HashMap<String, Object>();

		if(directory == null || directory.isEmpty())
			directory = AppConstants.getInstance().getResolvedProperty("logging.path").replace("\\\\", "\\");

		boolean sizeFormat = (sizeFormatParam == null || sizeFormatParam.isEmpty()) ? true : Boolean.parseBoolean(sizeFormatParam);

		if(wildcard == null || wildcard.isEmpty())
			wildcard = AppConstants.getInstance().getProperty("logging.wildcard");

		try {
			if (!FileUtils.readAllowed(FileViewerServlet.permissionRules, servletRequest, directory)) {
				throw new ApiException("Access to path ("+directory+") not allowed!");
			}
			Dir2Map dir = new Dir2Map(directory, sizeFormat, wildcard, showDirectories, maxItems);

			returnMap.put("list", dir.getList());
			returnMap.put("count", dir.size());
			returnMap.put("directory", dir.getDirectory());
			returnMap.put("sizeFormat", sizeFormat);
			returnMap.put("wildcard", wildcard);
		} catch (IOException e) {
			throw new ApiException("Error while trying to retreive directory information", e);
		}

		return Response.status(Response.Status.OK).entity(returnMap).build();
	}
}
