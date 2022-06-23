/*
   Copyright 2022 WeAreFrank!

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

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.util.ClassUtils;

/**
 * API to get class information
 */
@Path("/")
public class ClassInfo {

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/class/{className}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getClassInfo(
			@PathParam("className") String className, 
			@QueryParam("base") String baseClassName
			) throws ApiException {
		try {
			Class baseClass;
			if (StringUtils.isNotEmpty(baseClassName)) {
				baseClass = Class.forName(baseClassName, false, this.getClass().getClassLoader());
			} else {
				baseClass = this.getClass();
			}
			ClassLoader classLoader = baseClass.getClassLoader();
			
			Class clazz = classLoader.loadClass(className);
			
			List<?> result = ClassUtils.getClassInfoList(clazz);
			
			return Response.status(Response.Status.OK).entity(result).build();
		} catch (Exception e) {
			throw new ApiException("Could not determine classInfo for class ["+className+"]", e);
		}
	}

}
