/*
Copyright 2016 Nationale-Nederlanden

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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.IThreadCountControllable;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.extensions.esb.EsbJmsListener;
import nl.nn.adapterframework.extensions.esb.EsbUtils;
import nl.nn.adapterframework.http.RestListener;
import nl.nn.adapterframework.jdbc.JdbcSenderBase;
import nl.nn.adapterframework.jms.JmsListenerBase;
import nl.nn.adapterframework.jms.JmsMessageBrowser;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.MessageKeeperMessage;
import nl.nn.adapterframework.util.RunStateEnum;

import org.apache.commons.lang.StringUtils;

/**
* Get adapter information from either all or a specified adapter
* 
* @author	Niels Meijer
*/

@Path("/")
public final class ShowConfigurationStatus extends Base {

	private boolean showCountMessageLog = AppConstants.getInstance().getBoolean("messageLog.count.show", true);
	private boolean showCountErrorStore = AppConstants.getInstance().getBoolean("errorStore.count.show", true);
	
	@GET
	@RolesAllowed({"ObserverAccess", "IbisTester"})
	@Path("/adapters")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAdapters(@Context ServletConfig servletConfig) throws ApiException {
		initBase(servletConfig);
		
		if (ibisManager == null) {
			throw new ApiException("Config not found!");
		}
		
		Map<String, Object> adapterList = new HashMap<String, Object>();
		List<IAdapter> registeredAdapters = ibisManager.getRegisteredAdapters();
		
		for(Iterator<IAdapter> adapterIt=registeredAdapters.iterator(); adapterIt.hasNext();) {
			Adapter adapter = (Adapter)adapterIt.next();
			
			Map<String, Object> adapterInfo = mapAdapter(adapter);
			
			adapterList.put((String) adapterInfo.get("name"), adapterInfo);
		}
		
		return Response.status(Response.Status.CREATED).entity(adapterList).build();
	}

	@GET
	@RolesAllowed({"ObserverAccess", "IbisTester"})
	@Path("/adapters/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAdapter(@PathParam("name") String name, @Context ServletConfig servletConfig, @Context Request request) throws ApiException {
		initBase(servletConfig);

		if (ibisManager == null) {
			throw new ApiException("Config not found!");
		}
		
		Adapter adapter = (Adapter) ibisManager.getRegisteredAdapter(name);
		
		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}
		
		Map<String, Object> adapterInfo = mapAdapter(adapter);
		adapterInfo.put("receivers", mapAdapterReceivers(adapter));
		adapterInfo.put("pipes", mapAdapterPipes(adapter));
		adapterInfo.put("messages", mapAdapterMessages(adapter));

		Response.ResponseBuilder response = null;

		//Calculate the ETag on last modified date of user resource 
		EntityTag etag = new EntityTag(adapterInfo.hashCode() + "");

		//Verify if it matched with etag available in http request
		response = request.evaluatePreconditions(etag);

		//If ETag matches the response will be non-null; 
		if (response != null) {
			return response.tag(etag).build();
		}
		
		response = Response.status(Response.Status.CREATED).entity(adapterInfo).tag(etag);
		return response.build();
	}
	
	@PUT
	@RolesAllowed({"ObserverAccess", "IbisTester", "AdminAccess"})
	@Path("/adapters/{adapterName}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateAdapter(@PathParam("adapterName") String adapterName, LinkedHashMap<String, Object> json, @Context ServletConfig servletConfig) throws ApiException {
		initBase(servletConfig);

		if (ibisManager == null) {
			throw new ApiException("Config not found!");
		}
		
		Adapter adapter = (Adapter) ibisManager.getRegisteredAdapter(adapterName);
		
		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}
		
		Response.ResponseBuilder response = Response.status(Response.Status.NO_CONTENT); //PUT defaults to no content
		
		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if(key.equalsIgnoreCase("action")) {//Start or stop an adapter!
				String action = null;
				
				if(value.equals("stop")) { action = "stopadapter"; }
				if(value.equals("start")) { action = "startadapter"; }
				
			    ibisManager.handleAdapter(action, "", adapterName, null, null, false);
				
			    response.entity("{\"status\":\"ok\"}");
			}
		}
		
		return response.build();
	}
	
	@PUT
	@RolesAllowed({"ObserverAccess", "IbisTester", "AdminAccess"})
	@Path("/adapters/{adapterName}/receivers/{receiverName}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateReceiver(@PathParam("adapterName") String adapterName, @PathParam("receiverName") String receiverName, LinkedHashMap<String, Object> json, @Context ServletConfig servletConfig) throws ApiException {
		initBase(servletConfig);

		if (ibisManager == null) {
			throw new ApiException("Config not found!");
		}
		
		Adapter adapter = (Adapter) ibisManager.getRegisteredAdapter(adapterName);
		
		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}
		
		Response.ResponseBuilder response = Response.status(Response.Status.NO_CONTENT); //PUT defaults to no content
		
		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if(key.equalsIgnoreCase("action")) {//Start or stop an adapter!
				String action = null;
				
				if(value.equals("stop")) { action = "stopreceiver"; }
				if(value.equals("start")) { action = "startreceiver"; }

			    ibisManager.handleAdapter(action, "", adapterName, receiverName, null, false);
			    response.entity("{\"status\":\"ok\"}");
			}
		}
		
		return response.build();
	}
	
	@GET
	@RolesAllowed({"ObserverAccess", "IbisTester"})
	@Path("/adapters/{name}/pipes")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAdapterPipes(@PathParam("name") String adapterName, @Context ServletConfig servletConfig) throws ApiException {
		initBase(servletConfig);

		if (ibisManager == null) {
			throw new ApiException("Config not found!");
		}
		
		Adapter adapter = (Adapter) ibisManager.getRegisteredAdapter(adapterName);
		
		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}
		
		ArrayList<Object> adapterInfo = mapAdapterPipes(adapter);
		
		return Response.status(Response.Status.CREATED).entity(adapterInfo).build();
	}
	
	@GET
	@RolesAllowed({"ObserverAccess", "IbisTester"})
	@Path("/adapters/{name}/messages")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAdapterMessages(@PathParam("name") String adapterName, @Context ServletConfig servletConfig) throws ApiException {
		initBase(servletConfig);

		if (ibisManager == null) {
			throw new ApiException("Config not found!");
		}
		
		Adapter adapter = (Adapter) ibisManager.getRegisteredAdapter(adapterName);
		
		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}
		
		ArrayList<Object> adapterInfo = mapAdapterMessages(adapter);
		
		return Response.status(Response.Status.CREATED).entity(adapterInfo).build();
	}
	
	@GET
	@RolesAllowed({"ObserverAccess", "IbisTester"})
	@Path("/adapters/{name}/receivers")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAdapterReceivers(@PathParam("name") String adapterName, @Context ServletConfig servletConfig) throws ApiException {
		initBase(servletConfig);

		if (ibisManager == null) {
			throw new ApiException("Config not found!");
		}
		
		Adapter adapter = (Adapter) ibisManager.getRegisteredAdapter(adapterName);
		
		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}
		
		ArrayList<Object> receiverInfo = mapAdapterReceivers(adapter);
		
		return Response.status(Response.Status.CREATED).entity(receiverInfo).build();
	}
	
	private ArrayList<Object> mapAdapterPipes(Adapter adapter) {
		PipeLine pipeline = adapter.getPipeLine();
		int totalPipes = pipeline.getPipes().size();
		ArrayList<Object> pipes = new ArrayList<Object>(totalPipes);

		for (int i=0; i<totalPipes; i++) {
			Map<String, Object> pipesInfo = new HashMap<String, Object>();
			IPipe pipe = pipeline.getPipe(i);
			String pipename = pipe.getName();
			pipesInfo.put("name", pipename);
			if (pipe instanceof MessageSendingPipe) {
				MessageSendingPipe msp=(MessageSendingPipe)pipe;
				ISender sender = msp.getSender();
				pipesInfo.put("sender", ClassUtils.nameOf(sender));
				if (sender instanceof HasPhysicalDestination) {
					pipesInfo.put("destination",((HasPhysicalDestination)sender).getPhysicalDestinationName());
				}
				if (sender instanceof JdbcSenderBase) {
					pipesInfo.put("isJdbcSender", true);
				}
				IListener listener = msp.getListener();
				if (listener!=null) {
					pipesInfo.put("listenerName", listener.getName());
					pipesInfo.put("listenerClass", ClassUtils.nameOf(listener));
					if (listener instanceof HasPhysicalDestination) {
						String pd = ((HasPhysicalDestination)listener).getPhysicalDestinationName();
						pipesInfo.put("listenerDestination", pd);
					}
				}
				ITransactionalStorage messageLog = msp.getMessageLog();
				if (messageLog!=null) {
					pipesInfo.put("hasMessageLog", true);
					String messageLogCount;
					try {
						if (showCountMessageLog) {
							messageLogCount=""+messageLog.getMessageCount();
						} else {
							messageLogCount="?";
						}
					} catch (Exception e) {
						log.warn(e);
						messageLogCount="error";
					}
					pipesInfo.put("messageLogCount", messageLogCount);

					Map<String, Object> message = new HashMap<String, Object>();
					message.put("name", messageLog.getName());
					message.put("type", "log");
					message.put("slotId", messageLog.getSlotId());
					message.put("count", messageLogCount);
					pipesInfo.put("message", message);
				}
			}
			pipes.add(pipesInfo);
		}
		return pipes;
	}
	
	private ArrayList<Object> mapAdapterReceivers(Adapter adapter) {
		ArrayList<Object> receivers = new ArrayList<Object>();
		
		Iterator<?> recIt=adapter.getReceiverIterator();
		if (recIt.hasNext()){
			while (recIt.hasNext()){
				IReceiver receiver=(IReceiver) recIt.next();
				Map<String, Object> receiverInfo = new HashMap<String, Object>();

				RunStateEnum receiverRunState = receiver.getRunState();

				receiverInfo.put("started", receiverRunState.equals(RunStateEnum.STARTED));
				receiverInfo.put("state", receiverRunState.toString().toLowerCase().replace("*", ""));
				
				receiverInfo.put("name", receiver.getName());
				receiverInfo.put("class", ClassUtils.nameOf(receiver));
				Map<String, Object> messages = new HashMap<String, Object>(3);
				messages.put("received", receiver.getMessagesReceived());
				messages.put("retried", receiver.getMessagesRetried());
				messages.put("rejected", receiver.getMessagesRejected());
				receiverInfo.put("messages", messages);
				ISender sender=null;
				if (receiver instanceof ReceiverBase ) {
					ReceiverBase rb = (ReceiverBase) receiver;
					IListener listener=rb.getListener();
					receiverInfo.put("listenerClass", ClassUtils.nameOf(listener));
					if (listener instanceof HasPhysicalDestination) {
						String pd = ((HasPhysicalDestination)rb.getListener()).getPhysicalDestinationName();
						receiverInfo.put("listenerDestination", pd);
					}
					if (listener instanceof HasSender) {
						sender = ((HasSender)listener).getSender();
					}
					//receiverInfo.put("hasInprocessStorage", ""+(rb.getInProcessStorage()!=null));
					ITransactionalStorage ts;
					ts=rb.getErrorStorage();
					receiverInfo.put("hasErrorStorage", (ts!=null));
					if (ts!=null) {
						try {
							if (showCountErrorStore) {
								receiverInfo.put("errorStorageCount", ts.getMessageCount());
							} else {
								receiverInfo.put("errorStorageCount", "?");
							}
						} catch (Exception e) {
							log.warn(e);
							receiverInfo.put("errorStorageCount", "error");
						}
					}
					ts=rb.getMessageLog();
					receiverInfo.put("hasMessageLog", (ts!=null));
					if (ts!=null) {
						try {
							if (showCountMessageLog) {
								receiverInfo.put("messageLogCount", ts.getMessageCount());
							} else {
								receiverInfo.put("messageLogCount", "?");
							}
						} catch (Exception e) {
							log.warn(e);
							receiverInfo.put("messageLogCount", "error");
						}
					}
					boolean isRestListener = (listener instanceof RestListener);
					receiverInfo.put("isRestListener", isRestListener);
					if (isRestListener) {
						RestListener rl = (RestListener) listener;
						receiverInfo.put("restUriPattern", rl.getRestUriPattern());
						receiverInfo.put("isView", (rl.isView()==null?false:rl.isView()));
					}
					if (listener instanceof JmsListenerBase) {
						JmsListenerBase jlb = (JmsListenerBase) listener;
						JmsMessageBrowser jmsBrowser;
						if (StringUtils.isEmpty(jlb
								.getMessageSelector())) {
							jmsBrowser = new JmsMessageBrowser();
						} else {
							jmsBrowser = new JmsMessageBrowser(jlb.getMessageSelector());
						}
						jmsBrowser.setName("MessageBrowser_" + jlb.getName());
						jmsBrowser.setJmsRealm(jlb.getJmsRealName());
						jmsBrowser.setDestinationName(jlb.getDestinationName());
						jmsBrowser.setDestinationType(jlb.getDestinationType());
						String numMsgs;
						try {
							int messageCount = jmsBrowser
									.getMessageCount();
							numMsgs = String.valueOf(messageCount);
						} catch (Throwable t) {
							log.warn(t);
							numMsgs = "?";
						}
						receiverInfo.put("pendingMessagesCount", numMsgs);
					}
					boolean isEsbJmsFFListener = false;
					if (listener instanceof EsbJmsListener) {
						EsbJmsListener ejl = (EsbJmsListener) listener;
						if (ejl.getMessageProtocol().equalsIgnoreCase("FF")) {
							isEsbJmsFFListener = true;
						}
						String esbNumMsgs = EsbUtils.getQueueMessageCount(ejl);
						if (esbNumMsgs == null) {
							esbNumMsgs = "?";
						}
						receiverInfo.put("esbPendingMessagesCount", esbNumMsgs);
					}
					receiverInfo.put("isEsbJmsFFListener", isEsbJmsFFListener);
				}

				if (receiver instanceof HasSender) {
					ISender rsender = ((HasSender) receiver).getSender();
					if (rsender!=null) { // this sender has preference, but avoid overwriting listeners sender with null
						sender=rsender; 
					}
				}
				if (sender != null) { 
					receiverInfo.put("senderName", sender.getName());
					receiverInfo.put("senderClass", ClassUtils.nameOf(sender));
					if (sender instanceof HasPhysicalDestination) {
						String pd = ((HasPhysicalDestination)sender).getPhysicalDestinationName();
						receiverInfo.put("senderDestination", pd);
					}
				}
				if (receiver instanceof IThreadCountControllable) {
					IThreadCountControllable tcc = (IThreadCountControllable)receiver;
					if (tcc.isThreadCountReadable()) {
						receiverInfo.put("threadCount", tcc.getCurrentThreadCount());
						receiverInfo.put("maxThreadCount", tcc.getMaxThreadCount());
					}
					if (tcc.isThreadCountControllable()) {
						receiverInfo.put("threadCountControllable", "true");
					}
				}
				receivers.add(receiverInfo);
			}
		}
		return receivers;
	}
	
	private ArrayList<Object> mapAdapterMessages(Adapter adapter) {
		int totalMessages = adapter.getMessageKeeper().size();
		//adapter.getMessageKeeper().get
		ArrayList<Object> messages = new ArrayList<Object>(totalMessages);
		for (int t=0; t<totalMessages; t++) {
			Map<String, Object> message = new HashMap<String, Object>();
			MessageKeeperMessage msg = adapter.getMessageKeeper().getMessage(t);
		
			message.put("message", msg.getMessageText());
			message.put("date", msg.getMessageDate());
			message.put("level", msg.getMessageLevel());
			message.put("capacity", adapter.getMessageKeeper().capacity());
			
			messages.add(message);
		}
		return messages;
	}

	private Map<String, Object> mapAdapter(Adapter adapter) {
		Map<String, Object> adapterInfo = new HashMap<String, Object>();
		Configuration config = adapter.getConfiguration();
		
		String adapterName = adapter.getName();
		adapterInfo.put("name", adapterName);
		adapterInfo.put("description", adapter.getDescription());
		adapterInfo.put("configuration", config.getName() );
		// replace low line (x'5f') by asterisk (x'2a) so it's sorted before any digit and letter 
		String nameUC = StringUtils.upperCase(StringUtils.replace(adapterName,"_", "*"));
		adapterInfo.put("nameUC", nameUC);
		RunStateEnum adapterRunState = adapter.getRunState();
		boolean started = adapterRunState.equals(RunStateEnum.STARTED);
		adapterInfo.put("started", started);
		String state = adapterRunState.toString().toLowerCase().replace("*", "");
		adapterInfo.put("state", state);
		
		boolean configured =  adapter.configurationSucceeded();
		adapterInfo.put("configured", configured);
		adapterInfo.put("upSince", adapter.getStatsUpSinceDate().getTime());
		Date lastMessage = adapter.getLastMessageDateDate();
		adapterInfo.put("lastMessage", (lastMessage == null) ? 0 : lastMessage.getTime());
		int messagesInProcess = adapter.getNumOfMessagesInProcess();
		adapterInfo.put("messagesInProcess", messagesInProcess);
		long messagesProcessed = adapter.getNumOfMessagesProcessed();
		adapterInfo.put("messagesProcessed", messagesProcessed);
		long messagesInError = adapter.getNumOfMessagesInError();
		adapterInfo.put("messagesInError", messagesInError);
		
		return adapterInfo;
	}
}
