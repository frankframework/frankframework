/*
   Copyright 2023 WeAreFrank!

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
package nl.nn.adapterframework.management.web;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;

/**
 * This class exists to provide backwards compatibility for endpoints without a configuration path prefix.
 */
@Path("/")
public class CompatiblityTransactionalStorage extends TransactionalStorage {

	@GET
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/{storageSource}/{storageSourceName}/stores/{processState}/messages/{messageId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Deprecated
	public Response browseMessageOld(
				@PathParam("adapterName") String adapterName,
				@PathParam("storageSource") StorageSource storageSource,
				@PathParam("storageSourceName") String storageSourceName,
				@PathParam("processState") String processState,
				@PathParam("messageId") String messageId,
				@QueryParam("configuration") String configuration
			) {
		final String config = (StringUtils.isNotEmpty(configuration)) ? configuration : null;
		return browseMessage(config, adapterName, storageSource, storageSourceName, processState, messageId);
	}

	@GET
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/{storageSource}/{storageSourceName}/stores/{processState}/messages/{messageId}/download")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Deprecated
	public Response downloadMessageOld(
			@PathParam("adapterName") String adapterName,
			@PathParam("storageSource") StorageSource storageSource,
			@PathParam("storageSourceName") String storageSourceName,
			@PathParam("processState") String processState,
			@PathParam("messageId") String messageId,
			@QueryParam("configuration") String configuration
		) {
		final String config = (StringUtils.isNotEmpty(configuration)) ? configuration : null;
		return downloadMessage(config, adapterName, storageSource, storageSourceName, processState, messageId);
	}

	@POST
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/{storageSource}/{storageSourceName}/stores/{processState}/messages/download")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Deprecated
	public Response downloadMessagesOld(
			@PathParam("adapterName") String adapterName,
			@PathParam("storageSource") StorageSource storageSource,
			@PathParam("storageSourceName") String storageSourceName,
			@PathParam("processState") String processState,
			@QueryParam("configuration") String configuration,
			MultipartBody input
		) {
		final String config = (StringUtils.isNotEmpty(configuration)) ? configuration : null;
		return downloadMessages(config, adapterName, storageSource, storageSourceName, processState, input);
	}

	@GET
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/{storageSource}/{storageSourceName}/stores/{processState}")
	@Produces(MediaType.APPLICATION_JSON)
	@Deprecated
	public Response browseMessagesOld(
				@PathParam("adapterName") String adapterName,
				@PathParam("storageSource") StorageSource storageSource,
				@PathParam("storageSourceName") String storageSourceName,
				@PathParam("processState") String processState,
				@QueryParam("configuration") String configuration,

				@QueryParam("type") String type,
				@QueryParam("host") String host,
				@QueryParam("id") String id,
				@QueryParam("messageId") String messageId,
				@QueryParam("correlationId") String correlationId,
				@QueryParam("comment") String comment,
				@QueryParam("message") String message,
				@QueryParam("label") String label,
				@QueryParam("startDate") String startDateStr,
				@QueryParam("endDate") String endDateStr,
				@QueryParam("sort") String sort,
				@QueryParam("skip") int skipMessages,
				@QueryParam("max") int maxMessages
			) {
		final String config = (StringUtils.isNotEmpty(configuration)) ? configuration : null;
		return browseMessages(config, adapterName, storageSource, storageSourceName, processState, type, host, id, messageId, correlationId, comment, message, label, startDateStr, endDateStr, sort, skipMessages, maxMessages);
	}




	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/receivers/{receiverName}/stores/Error/messages/{messageId}")
	@Relation("pipeline")
	@Produces(MediaType.APPLICATION_JSON)
	@Deprecated
	public Response resendReceiverMessageOld(
			@PathParam("adapterName") String adapter,
			@PathParam("receiverName") String receiver,
			@PathParam("messageId") String messageId,
			@QueryParam("configuration") String configuration
		) {
		final String config = (StringUtils.isNotEmpty(configuration)) ? configuration : null;
		return resendReceiverMessage(config, adapter, receiver, messageId);
	}

	@POST
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/receivers/{receiverName}/stores/Error")
	@Relation("pipeline")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Deprecated
	public Response resendReceiverMessagesOld(@PathParam("adapterName") String adapter, @PathParam("receiverName") String receiver, @QueryParam("configuration") String configuration, MultipartBody input) {
		final String config = (StringUtils.isNotEmpty(configuration)) ? configuration : null;
		return resendReceiverMessages(config, adapter, receiver, input);
	}

	@POST
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/receivers/{receiverName}/stores/{processState}/move/{targetState}")
	@Relation("pipeline")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Deprecated
	public Response changeProcessStateOld(@PathParam("adapterName") String adapter, @PathParam("receiverName") String receiver,
			@PathParam("processState") String processState, @PathParam("targetState") String targetState, MultipartBody input,
			@QueryParam("configuration") String configuration) {
		final String config = (StringUtils.isNotEmpty(configuration)) ? configuration : null;
		return changeProcessState(config, adapter, receiver, processState, targetState, input);
	}

	@DELETE
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/receivers/{receiverName}/stores/Error/messages/{messageId}")
	@Relation("pipeline")
	@Produces(MediaType.APPLICATION_JSON)
	@Deprecated
	public Response deleteReceiverMessageOld(
			@PathParam("adapterName") String adapter,
			@PathParam("receiverName") String receiver,
			@PathParam("messageId") String messageId,
			@QueryParam("configuration") String configuration) {
		final String config = (StringUtils.isNotEmpty(configuration)) ? configuration : null;
		return deleteReceiverMessage(config, adapter, receiver, messageId);
	}

	@DELETE
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/receivers/{receiverName}/stores/Error")
	@Relation("pipeline")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Deprecated
	public Response deleteReceiverMessagesOld(@PathParam("adapterName") String adapter, @PathParam("receiverName") String receiver, @QueryParam("configuration") String configuration, MultipartBody input) {
		final String config = (StringUtils.isNotEmpty(configuration)) ? configuration : null;
		return deleteReceiverMessages(config, adapter, receiver, input);
	}
}
