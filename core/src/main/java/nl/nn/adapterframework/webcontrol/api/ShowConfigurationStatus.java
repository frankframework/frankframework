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
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisManager.IbisAction;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.encryption.HasKeystore;
import nl.nn.adapterframework.encryption.KeystoreType;
import nl.nn.adapterframework.extensions.esb.EsbJmsListener;
import nl.nn.adapterframework.extensions.esb.EsbUtils;
import nl.nn.adapterframework.http.RestListener;
import nl.nn.adapterframework.jdbc.JdbcSenderBase;
import nl.nn.adapterframework.jms.JmsBrowser;
import nl.nn.adapterframework.jms.JmsListenerBase;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.MessageKeeperMessage;
import nl.nn.adapterframework.util.RunState;
import nl.nn.adapterframework.util.flow.FlowDiagramManager;

/**
 * Get adapter information from either all or a specified adapter
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public final class ShowConfigurationStatus extends Base {
	@Context Request request;
	private static final String RECEIVERS="receivers";
	private static final String PIPES="pipes";
	private static final String MESSAGES="messages";

	private boolean showCountMessageLog = AppConstants.getInstance().getBoolean("messageLog.count.show", true);

	private Adapter getAdapter(String adapterName) {
		Adapter adapter = getIbisManager().getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		return adapter;
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAdapters(@QueryParam("expanded") String expanded, @QueryParam("showPendingMsgCount") boolean showPendingMsgCount) throws ApiException {
		TreeMap<String, Object> adapterList = new TreeMap<>();
		for(Adapter adapter: getIbisManager().getRegisteredAdapters()) {
			Map<String, Object> adapterInfo = mapAdapter(adapter);
			if(expanded != null && !expanded.isEmpty()) {
				if(expanded.equalsIgnoreCase("all")) {
					adapterInfo.put(RECEIVERS, mapAdapterReceivers(adapter, showPendingMsgCount));
					adapterInfo.put(PIPES, mapAdapterPipes(adapter));
					adapterInfo.put(MESSAGES, mapAdapterMessages(adapter));
				} else if(expanded.equalsIgnoreCase(RECEIVERS)) {
					adapterInfo.put(RECEIVERS, mapAdapterReceivers(adapter, showPendingMsgCount));
				} else if(expanded.equalsIgnoreCase(PIPES)) {
					adapterInfo.put(PIPES, mapAdapterPipes(adapter));
				} else if(expanded.equalsIgnoreCase(MESSAGES)) {
					adapterInfo.put(MESSAGES, mapAdapterMessages(adapter));
				} else {
					throw new ApiException("Invalid value ["+expanded+"] for parameter expanded supplied!");
				}
			}
			adapterList.put((String) adapterInfo.get("name"), adapterInfo);
		}

		Response.ResponseBuilder response = null;

		//Calculate the ETag on last modified date of user resource
		EntityTag etag = new EntityTag(adapterList.hashCode() + "");

		//Verify if it matched with etag available in http request
		response = request.evaluatePreconditions(etag);

		//If ETag matches the response will be non-null
		if (response != null) {
			return response.tag(etag).build();
		}

		response = Response.status(Response.Status.OK).entity(adapterList).tag(etag);
		return response.build();
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAdapter(@PathParam("name") String name, @QueryParam("expanded") String expanded, @QueryParam("showPendingMsgCount") boolean showPendingMsgCount) throws ApiException {
		Adapter adapter = getAdapter(name);
		Map<String, Object> adapterInfo = mapAdapter(adapter);
		if(expanded != null && !expanded.isEmpty()) {
			if(expanded.equalsIgnoreCase("all")) {
				adapterInfo.put(RECEIVERS, mapAdapterReceivers(adapter, showPendingMsgCount));
				adapterInfo.put(PIPES, mapAdapterPipes(adapter));
				adapterInfo.put(MESSAGES, mapAdapterMessages(adapter));
			} else if(expanded.equalsIgnoreCase(RECEIVERS)) {
				adapterInfo.put(RECEIVERS, mapAdapterReceivers(adapter, showPendingMsgCount));
			} else if(expanded.equalsIgnoreCase(PIPES)) {
				adapterInfo.put(PIPES, mapAdapterPipes(adapter));
			} else if(expanded.equalsIgnoreCase(MESSAGES)) {
				adapterInfo.put(MESSAGES, mapAdapterMessages(adapter));
			} else {
				throw new ApiException("Invalid value ["+expanded+"] for parameter expanded supplied!");
			}
		}

		Response.ResponseBuilder response = null;

		//Calculate the ETag on last modified date of user resource
		EntityTag etag = new EntityTag(adapterInfo.hashCode() + "");

		//Verify if it matched with etag available in http request
		response = request.evaluatePreconditions(etag);

		//If ETag matches the response will be non-null
		if (response != null) {
			return response.tag(etag).build();
		}

		response = Response.status(Response.Status.OK).entity(adapterInfo).tag(etag);
		return response.build();
	}

	@GET
	@PermitAll
	@Path("/adapters/{name}/health")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getIbisHealth(@PathParam("name") String name) throws ApiException {

		Adapter adapter = getAdapter(name);
		Map<String, Object> response = new HashMap<>();
		List<String> errors = new ArrayList<>();

		RunState state = adapter.getRunState(); //Let's not make it difficult for ourselves and only use STARTED/ERROR enums

		if(state==RunState.STARTED) {
			for (Receiver<?> receiver: adapter.getReceivers()) {
				RunState rState = receiver.getRunState();

				if(rState!=RunState.STARTED) {
					errors.add("receiver["+receiver.getName()+"] of adapter["+adapter.getName()+"] is in state["+rState.toString()+"]");
					state = RunState.ERROR;
				}
			}
		} else {
			errors.add("adapter["+adapter.getName()+"] is in state["+state.toString()+"]");
			state = RunState.ERROR;
		}

		Status status = Response.Status.OK;
		if(state==RunState.ERROR) {
			status = Response.Status.SERVICE_UNAVAILABLE;
		}
		if(!errors.isEmpty())
			response.put("errors", errors);
		response.put("status", status);

		return Response.status(status).entity(response).build();
	}

	@SuppressWarnings("unchecked")
	@PUT //Normally you don't use the PUT method on a collection...
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateAdapters(Map<String, Object> json) throws ApiException {

		Response.ResponseBuilder response = Response.status(Response.Status.NO_CONTENT); //PUT defaults to no content
		IbisAction action = null;
		ArrayList<String> adapters = new ArrayList<>();

		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if(key.equalsIgnoreCase("action")) {//Start or stop an adapter!
				if(value.equals("stop")) { action = IbisAction.STOPADAPTER; }
				if(value.equals("start")) { action = IbisAction.STARTADAPTER; }
			}
			if(key.equalsIgnoreCase("adapters")) {
				try {
					adapters.addAll((ArrayList<String>) value);
				} catch(Exception e) {
					return response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
				}
			}
		}

		if(action != null) {
			response.status(Response.Status.ACCEPTED);
			if(adapters.isEmpty()) {
				getIbisManager().handleAction(action, "*ALL*", "*ALL*", null, getUserPrincipalName(), false);
			} else {
				for (Iterator<String> iterator = adapters.iterator(); iterator.hasNext();) {
					String adapterName = iterator.next();
					getIbisManager().handleAction(action, "", adapterName, null, getUserPrincipalName(), false);
				}
			}
		}

		return response.build();
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateAdapter(@PathParam("adapterName") String adapterName, Map<String, Object> json) throws ApiException {

		getAdapter(adapterName); //Check if the adapter exists!
		Response.ResponseBuilder response = Response.status(Response.Status.NO_CONTENT); //PUT defaults to no content

		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if(key.equalsIgnoreCase("action")) {//Start or stop an adapter!
				IbisAction action = null;

				if(value.equals("stop")) { action = IbisAction.STOPADAPTER; }
				if(value.equals("start")) { action = IbisAction.STARTADAPTER; }

				getIbisManager().handleAction(action, "", adapterName, null, getUserPrincipalName(), false);

				response.entity("{\"status\":\"ok\"}");
			}
		}

		return response.build();
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/receivers/{receiverName}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateReceiver(@PathParam("adapterName") String adapterName, @PathParam("receiverName") String receiverName, Map<String, Object> json) throws ApiException {

		Adapter adapter = getAdapter(adapterName);

		Receiver<?> receiver = adapter.getReceiverByName(receiverName);
		if(receiver == null) {
			throw new ApiException("Receiver ["+receiverName+"] not found!");
		}

		Response.ResponseBuilder response = Response.status(Response.Status.NO_CONTENT); //PUT defaults to no content

		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if(key.equalsIgnoreCase("action")) {//Start or stop an adapter!
				IbisAction action = null;

				if(value.equals("stop")) { action = IbisAction.STOPRECEIVER; }
				else if(value.equals("start")) { action = IbisAction.STARTRECEIVER; }
				else if(value.equals("incthread")) { action = IbisAction.INCTHREADS; }
				else if(value.equals("decthread")) { action = IbisAction.DECTHREADS; }

				if(action == null)
					throw new ApiException("no or unknown action provided");

				getIbisManager().handleAction(action, "", adapterName, receiverName, getUserPrincipalName(), false);
				response.entity("{\"status\":\"ok\"}");
			}
		}

		return response.build();
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{name}/pipes")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAdapterPipes(@PathParam("name") String adapterName) throws ApiException {

		Adapter adapter = getAdapter(adapterName);
		ArrayList<Object> adapterInfo = mapAdapterPipes(adapter);

		if(adapterInfo == null)
			throw new ApiException("Adapter not configured!");

		return Response.status(Response.Status.OK).entity(adapterInfo).build();
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{name}/messages")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAdapterMessages(@PathParam("name") String adapterName) throws ApiException {

		Adapter adapter = getAdapter(adapterName);
		ArrayList<Object> adapterInfo = mapAdapterMessages(adapter);

		return Response.status(Response.Status.OK).entity(adapterInfo).build();
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{name}/receivers")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAdapterReceivers(@PathParam("name") String adapterName, @QueryParam("showPendingMsgCount") boolean showPendingMsgCount) throws ApiException {

		Adapter adapter = getAdapter(adapterName);
		ArrayList<Object> receiverInfo = mapAdapterReceivers(adapter, showPendingMsgCount);

		return Response.status(Response.Status.OK).entity(receiverInfo).build();
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{name}/flow")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getAdapterFlow(@PathParam("name") String adapterName) throws ApiException {
		Adapter adapter = getAdapter(adapterName);

		FlowDiagramManager flowDiagramManager = getFlowDiagramManager();

		try {
			ResponseBuilder response;
			InputStream flow = flowDiagramManager.get(adapter);
			if(flow != null) {
				response = Response.ok(flow, flowDiagramManager.getMediaType());
			} else {
				response = Response.noContent();
			}
			return response.build();
		} catch (IOException e) {
			throw new ApiException(e);
		}
	}

	private Map<String, Object> addCertificateInfo(HasKeystore s) {
		String certificate = s.getKeystore();
		if (certificate == null || StringUtils.isEmpty(certificate))
			return null;

		Map<String, Object> certElem = new HashMap<>(4);
		certElem.put("name", certificate);
		String certificateAuthAlias = s.getKeystoreAuthAlias();
		certElem.put("authAlias", certificateAuthAlias);
		URL certificateUrl = ClassUtils.getResourceURL(s, s.getKeystore());
		if (certificateUrl == null) {
			certElem.put("url", "");
			certElem.put("info", "*** ERROR ***");
		} else {
			certElem.put("url", certificateUrl.toString());
			String certificatePassword = s.getKeystorePassword();
			CredentialFactory certificateCf = new CredentialFactory(certificateAuthAlias, null, certificatePassword);
			KeystoreType keystoreType = s.getKeystoreType();
			certElem.put("info", getCertificateInfo(certificateUrl, certificateCf.getPassword(), keystoreType, "Certificate chain"));
		}
		return certElem;
	}

	private ArrayList<Object> getCertificateInfo(final URL url, final String password, KeystoreType keystoreType, String prefix) {
		ArrayList<Object> certificateList = new ArrayList<>();
		try (InputStream stream = url.openStream()) {
			KeyStore keystore = KeyStore.getInstance(keystoreType.name());
			keystore.load(stream, password != null ? password.toCharArray() : null);
			if (log.isInfoEnabled()) {
				Enumeration<String> aliases = keystore.aliases();
				while (aliases.hasMoreElements()) {
					String alias =  aliases.nextElement();
					ArrayList<Object> infoElem = new ArrayList<>();
					infoElem.add(prefix + " '" + alias + "':");
					Certificate trustedcert = keystore.getCertificate(alias);
					if (trustedcert instanceof X509Certificate) {
						X509Certificate cert = (X509Certificate) trustedcert;
						infoElem.add("Subject DN: " + cert.getSubjectDN());
						infoElem.add("Signature Algorithm: " + cert.getSigAlgName());
						infoElem.add("Valid from: " + cert.getNotBefore());
						infoElem.add("Valid until: " + cert.getNotAfter());
						infoElem.add("Issuer: " + cert.getIssuerDN());
					}
					certificateList.add(infoElem);
				}
			}
		} catch (Exception e) {
			certificateList.add("*** ERROR ***");
		}
		return certificateList;
	}

	private ArrayList<Object> mapAdapterPipes(Adapter adapter) {
		if(!adapter.configurationSucceeded())
			return null;
		PipeLine pipeline = adapter.getPipeLine();
		int totalPipes = pipeline.getPipes().size();
		ArrayList<Object> pipes = new ArrayList<>(totalPipes);

		for (int i=0; i<totalPipes; i++) {
			Map<String, Object> pipesInfo = new HashMap<>();
			IPipe pipe = pipeline.getPipe(i);
			Map<String, PipeForward> pipeForwards = pipe.getForwards();

			String pipename = pipe.getName();

			Map<String, String> forwards = new HashMap<>();
			for (PipeForward fwrd : pipeForwards.values()) {
				forwards.put(fwrd.getName(), fwrd.getPath());
			}

			pipesInfo.put("name", pipename);
			pipesInfo.put("forwards", forwards);
			if (pipe instanceof HasKeystore) {
				HasKeystore s = (HasKeystore) pipe;
				Map<String, Object> certInfo = addCertificateInfo(s);
				if(certInfo != null)
					pipesInfo.put("certificate", certInfo);
			}
			if (pipe instanceof MessageSendingPipe) {
				MessageSendingPipe msp=(MessageSendingPipe)pipe;
				ISender sender = msp.getSender();
				pipesInfo.put("sender", ClassUtils.nameOf(sender));
				if (sender instanceof HasKeystore) {
					HasKeystore s = (HasKeystore) sender;
					Map<String, Object> certInfo = addCertificateInfo(s);
					if(certInfo != null)
						pipesInfo.put("certificate", certInfo);
				}
				if (sender instanceof HasPhysicalDestination) {
					pipesInfo.put("destination",((HasPhysicalDestination)sender).getPhysicalDestinationName());
				}
				if (sender instanceof JdbcSenderBase) {
					pipesInfo.put("isJdbcSender", true);
				}
				IListener<?> listener = msp.getListener();
				if (listener!=null) {
					pipesInfo.put("listenerName", listener.getName());
					pipesInfo.put("listenerClass", ClassUtils.nameOf(listener));
					if (listener instanceof HasPhysicalDestination) {
						String pd = ((HasPhysicalDestination)listener).getPhysicalDestinationName();
						pipesInfo.put("listenerDestination", pd);
					}
				}
				ITransactionalStorage<?> messageLog = msp.getMessageLog();
				if (messageLog!=null) {
					mapPipeMessageLog(messageLog, pipesInfo);
				} else if(sender instanceof ITransactionalStorage) { // in case no message log specified
					ITransactionalStorage<?> store = (ITransactionalStorage<?>) sender;
					mapPipeMessageLog(store, pipesInfo);
					pipesInfo.put("isSenderTransactionalStorage", true);
				}
			}
			pipes.add(pipesInfo);
		}
		return pipes;
	}

	private void mapPipeMessageLog(ITransactionalStorage<?> store, Map<String, Object> data) {
		data.put("hasMessageLog", true);
		String messageLogCount;
		try {
			if (showCountMessageLog) {
				messageLogCount=""+store.getMessageCount();
			} else {
				messageLogCount="?";
			}
		} catch (Exception e) {
			log.warn("Cannot determine number of messages in messageLog ["+store.getName()+"]", e);
			messageLogCount="error";
		}
		data.put("messageLogCount", messageLogCount);

		Map<String, Object> message = new HashMap<>();
		message.put("name", store.getName());
		message.put("type", "log");
		message.put("slotId", store.getSlotId());
		message.put("count", messageLogCount);
		data.put("message", message);
	}

	private ArrayList<Object> mapAdapterReceivers(Adapter adapter, boolean showPendingMsgCount) {
		ArrayList<Object> receivers = new ArrayList<>();

		for (Receiver<?> receiver: adapter.getReceivers()) {
			Map<String, Object> receiverInfo = new HashMap<>();

			RunState receiverRunState = receiver.getRunState();

			receiverInfo.put("name", receiver.getName());
			receiverInfo.put("state", receiverRunState.name().toLowerCase());

			Map<String, Object> messages = new HashMap<>(3);
			messages.put("received", receiver.getMessagesReceived());
			messages.put("retried", receiver.getMessagesRetried());
			messages.put("rejected", receiver.getMessagesRejected());
			receiverInfo.put(MESSAGES, messages);

			Set<ProcessState> knownStates = receiver.knownProcessStates();
			Map<ProcessState, Object> tsInfo = new LinkedHashMap<>();
			for (ProcessState state : knownStates) {
				IMessageBrowser<?> ts = receiver.getMessageBrowser(state);
				if(ts != null) {
					Map<String, Object> info = new HashMap<>();
					try {
						info.put("numberOfMessages", ts.getMessageCount());
					} catch (Exception e) {
						log.warn("Cannot determine number of messages in process state ["+state+"]", e);
						info.put("numberOfMessages", "error");
					}
					info.put("name", state.getName());
					tsInfo.put(state, info);
				}
			}
			receiverInfo.put("transactionalStores", tsInfo);

			ISender sender=null;
			IListener<?> listener=receiver.getListener();
			if(listener != null) {
				Map<String, Object> listenerInfo = new HashMap<>();
				listenerInfo.put("name", listener.getName());
				listenerInfo.put("class", ClassUtils.nameOf(listener));
				if (listener instanceof HasPhysicalDestination) {
					String pd = ((HasPhysicalDestination)receiver.getListener()).getPhysicalDestinationName();
					listenerInfo.put("destination", pd);
				}
				if (listener instanceof HasSender) {
					sender = ((HasSender)listener).getSender();
				}

				boolean isRestListener = (listener instanceof RestListener);
				listenerInfo.put("isRestListener", isRestListener);
				if (isRestListener) {
					RestListener rl = (RestListener) listener;
					listenerInfo.put("restUriPattern", rl.getRestUriPattern());
					listenerInfo.put("isView", rl.isView());
				}

				receiverInfo.put("listener", listenerInfo);
			}

			if ((listener instanceof JmsListenerBase) && showPendingMsgCount) {
				JmsListenerBase jlb = (JmsListenerBase) listener;
				JmsBrowser<javax.jms.Message> jmsBrowser;
				if (StringUtils.isEmpty(jlb.getMessageSelector())) {
					jmsBrowser = new JmsBrowser<>();
				} else {
					jmsBrowser = new JmsBrowser<>(jlb.getMessageSelector());
				}
				jmsBrowser.setName("MessageBrowser_" + jlb.getName());
				jmsBrowser.setJmsRealm(jlb.getJmsRealmName());
				jmsBrowser.setDestinationName(jlb.getDestinationName());
				jmsBrowser.setDestinationType(jlb.getDestinationType());
				String numMsgs;
				try {
					int messageCount = jmsBrowser.getMessageCount();
					numMsgs = String.valueOf(messageCount);
				} catch (Throwable t) {
					log.warn("Cannot determine number of messages in errorstore ["+jmsBrowser.getName()+"]", t);
					numMsgs = "?";
				}
				receiverInfo.put("pendingMessagesCount", numMsgs);
			}
			boolean isEsbJmsFFListener = false;
			if (listener instanceof EsbJmsListener) {
				EsbJmsListener ejl = (EsbJmsListener) listener;
				if(ejl.getMessageProtocol() != null) {
					if (ejl.getMessageProtocol().equalsIgnoreCase("FF")) {
						isEsbJmsFFListener = true;
					}
					if(showPendingMsgCount) {
						String esbNumMsgs = EsbUtils.getQueueMessageCount(ejl);
						if (esbNumMsgs == null) {
							esbNumMsgs = "?";
						}
						receiverInfo.put("esbPendingMessagesCount", esbNumMsgs);
					}
				}
			}
			receiverInfo.put("isEsbJmsFFListener", isEsbJmsFFListener);

			ISender rsender = receiver.getSender();
			if (rsender!=null) { // this sender has preference, but avoid overwriting listeners sender with null
				sender=rsender;
			}
			if (sender != null) {
				receiverInfo.put("senderName", sender.getName());
				receiverInfo.put("senderClass", ClassUtils.nameOf(sender));
				if (sender instanceof HasPhysicalDestination) {
					String pd = ((HasPhysicalDestination)sender).getPhysicalDestinationName();
					receiverInfo.put("senderDestination", pd);
				}
			}
			if (receiver.isThreadCountReadable()) {
				receiverInfo.put("threadCount", receiver.getCurrentThreadCount());
				receiverInfo.put("maxThreadCount", receiver.getMaxThreadCount());
			}
			if (receiver.isThreadCountControllable()) {
				receiverInfo.put("threadCountControllable", "true");
			}
			receivers.add(receiverInfo);
		}
		return receivers;
	}

	private ArrayList<Object> mapAdapterMessages(Adapter adapter) {
		int totalMessages = adapter.getMessageKeeper().size();
		ArrayList<Object> messages = new ArrayList<>(totalMessages);
		for (int t=0; t<totalMessages; t++) {
			Map<String, Object> message = new HashMap<>();
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
		Map<String, Object> adapterInfo = new HashMap<>();
		Configuration config = adapter.getConfiguration();

		String adapterName = adapter.getName();
		adapterInfo.put("name", adapterName);
		adapterInfo.put("description", adapter.getDescription());
		adapterInfo.put("configuration", config.getName() );
		RunState adapterRunState = adapter.getRunState();
		adapterInfo.put("started", adapterRunState==RunState.STARTED);
		String state = adapterRunState.toString().toLowerCase().replace("*", "");
		adapterInfo.put("state", state);

		adapterInfo.put("configured", adapter.configurationSucceeded());
		adapterInfo.put("upSince", adapter.getStatsUpSinceDate().getTime());
		Date lastMessage = adapter.getLastMessageDateDate();
		if(lastMessage != null) {
			adapterInfo.put("lastMessage", lastMessage.getTime());
			adapterInfo.put("messagesInProcess", adapter.getNumOfMessagesInProcess());
			adapterInfo.put("messagesProcessed", adapter.getNumOfMessagesProcessed());
			adapterInfo.put("messagesInError", adapter.getNumOfMessagesInError());
		}

		Iterator<Receiver<?>> it = adapter.getReceivers().iterator();
		int errorStoreMessageCount = 0;
		int messageLogMessageCount = 0;
		while(it.hasNext()) {
			Receiver<?> rcv = it.next();
			if(rcv.isNumberOfExceptionsCaughtWithoutMessageBeingReceivedThresholdReached()) {
				adapterInfo.put("receiverReachedMaxExceptions", "true");
			}

			IMessageBrowser<?> esmb = rcv.getMessageBrowser(ProcessState.ERROR);
			if(esmb != null) {
				try {
					errorStoreMessageCount += esmb.getMessageCount();
				} catch (ListenerException e) {
					if(log.isInfoEnabled()) log.warn("Cannot determine number of messages in errorstore of [{}]", rcv.getName(), e);
					else log.warn("Cannot determine number of messages in errorstore of [{}]: {}", rcv::getName, e::getMessage);
				}
			}
			IMessageBrowser<?> mlmb = rcv.getMessageBrowser(ProcessState.DONE);
			if(mlmb != null) {
				try {
					messageLogMessageCount += mlmb.getMessageCount();
				} catch (ListenerException e) {
					if(log.isInfoEnabled()) log.warn("Cannot determine number of messages in errorstore of [{}]", rcv.getName(), e);
					else log.warn("Cannot determine number of messages in errorstore of [{}]: {}", rcv::getName, e::getMessage);
				}
			}
		}
		if(errorStoreMessageCount != 0) {
			adapterInfo.put("errorStoreMessageCount", errorStoreMessageCount);
		}
		if(messageLogMessageCount != 0) {
			adapterInfo.put("messageLogMessageCount", messageLogMessageCount);
		}

		return adapterInfo;
	}
}
