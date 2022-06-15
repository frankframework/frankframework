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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import nl.nn.adapterframework.lifecycle.Gateway;

@Path("/")
public class Producer extends Base {

	@GET
	@Path("/send")
	@Produces("text/plain")
	public Response test() throws ApiException {
		Gateway<String> gateway = getApplicationContext().getBean("gateway", Gateway.class);
		Message<String> input = MessageBuilder.withPayload("test").build();
		Message<String> response = gateway.execute(input);
		return Response.status(Response.Status.OK).entity(response.getPayload()).build();
	}
}
