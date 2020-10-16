/*
Copyright 2016-2020 Integration Partners B.V.

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
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.IThreadCountControllable;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.extensions.esb.EsbJmsListener;
import nl.nn.adapterframework.extensions.esb.EsbUtils;
import nl.nn.adapterframework.ftp.FtpSender;
import nl.nn.adapterframework.http.HttpSender;
import nl.nn.adapterframework.http.RestListener;
import nl.nn.adapterframework.http.WebServiceSender;
import nl.nn.adapterframework.jdbc.JdbcSenderBase;
import nl.nn.adapterframework.jms.JmsBrowser;
import nl.nn.adapterframework.jms.JmsListenerBase;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.MessageKeeperMessage;
import nl.nn.adapterframework.util.RunStateEnum;
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

	private boolean showCountMessageLog = AppConstants.getInstance().getBoolean("messageLog.count.show", true);
	private boolean showCountErrorStore = AppConstants.getInstance().getBoolean("errorStore.count.show", true);

	private Adapter getAdapter(String adapterName) {
		Adapter adapter = (Adapter) getIbisManager().getRegisteredAdapter(adapterName);

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

		TreeMap<String, Object> adapterList = new TreeMap<String, Object>();
		List<IAdapter> registeredAdapters = getIbisManager().getRegisteredAdapters();

		for(Iterator<IAdapter> adapterIt=registeredAdapters.iterator(); adapterIt.hasNext();) {
			Adapter adapter = (Adapter)adapterIt.next();

			Map<String, Object> adapterInfo = mapAdapter(adapter);
			if(expanded != null && !expanded.isEmpty()) {
				if(expanded.equalsIgnoreCase("all")) {
					adapterInfo.put("receivers", mapAdapterReceivers(adapter, showPendingMsgCount));
					adapterInfo.put("pipes", mapAdapterPipes(adapter));
					adapterInfo.put("messages", mapAdapterMessages(adapter));
				}
				else if(expanded.equalsIgnoreCase("receivers")) {
					adapterInfo.put("receivers", mapAdapterReceivers(adapter, showPendingMsgCount));
				}
				else if(expanded.equalsIgnoreCase("pipes")) {
					adapterInfo.put("pipes", mapAdapterPipes(adapter));
				}
				else if(expanded.equalsIgnoreCase("messages")) {
					adapterInfo.put("messages", mapAdapterMessages(adapter));
				}
				else {
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

		//If ETag matches the response will be non-null; 
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
				adapterInfo.put("receivers", mapAdapterReceivers(adapter, showPendingMsgCount));
				adapterInfo.put("pipes", mapAdapterPipes(adapter));
				adapterInfo.put("messages", mapAdapterMessages(adapter));
			}
			else if(expanded.equalsIgnoreCase("receivers")) {
				adapterInfo.put("receivers", mapAdapterReceivers(adapter, showPendingMsgCount));
			}
			else if(expanded.equalsIgnoreCase("pipes")) {
				adapterInfo.put("pipes", mapAdapterPipes(adapter));
			}
			else if(expanded.equalsIgnoreCase("messages")) {
				adapterInfo.put("messages", mapAdapterMessages(adapter));
			}
			else {
				throw new ApiException("Invalid value ["+expanded+"] for parameter expanded supplied!");
			}
		}

		Response.ResponseBuilder response = null;

		//Calculate the ETag on last modified date of user resource 
		EntityTag etag = new EntityTag(adapterInfo.hashCode() + "");

		//Verify if it matched with etag available in http request
		response = request.evaluatePreconditions(etag);

		//If ETag matches the response will be non-null; 
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
		Map<String, Object> response = new HashMap<String, Object>();
		List<String> errors = new ArrayList<String>();

		RunStateEnum state = adapter.getRunState(); //Let's not make it difficult for ourselves and only use STARTED/ERROR enums

		if(state.equals(RunStateEnum.STARTED)) {
			Iterator<IReceiver> receiverIterator = adapter.getReceiverIterator();
			while (receiverIterator.hasNext()) {
				IReceiver receiver = receiverIterator.next();
				RunStateEnum rState = receiver.getRunState();

				if(!rState.equals(RunStateEnum.STARTED)) {
					errors.add("receiver["+receiver.getName()+"] of adapter["+adapter.getName()+"] is in state["+rState.toString()+"]");
					state = RunStateEnum.ERROR;
				}
			}
		}
		else {
			errors.add("adapter["+adapter.getName()+"] is in state["+state.toString()+"]");
			state = RunStateEnum.ERROR;
		}

		Status status = Response.Status.OK;
		if(state.equals(RunStateEnum.ERROR))
			status = Response.Status.SERVICE_UNAVAILABLE;

		if(errors.size() > 0)
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
	public Response updateAdapters(LinkedHashMap<String, Object> json) throws ApiException {

		Response.ResponseBuilder response = Response.status(Response.Status.NO_CONTENT); //PUT defaults to no content
		String action = null;
		ArrayList<String> adapters = new ArrayList<String>();

		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if(key.equalsIgnoreCase("action")) {//Start or stop an adapter!
				if(value.equals("stop")) { action = "stopadapter"; }
				if(value.equals("start")) { action = "startadapter"; }
			}
			if(key.equalsIgnoreCase("adapters")) {
				try {
					adapters.addAll((ArrayList<String>) value);
				}
				catch(Exception e) {
					return response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
				}
			}
		}

		if(action != null) {
			response.status(Response.Status.ACCEPTED);
			if(adapters.size() == 0) {
				getIbisManager().handleAdapter(action, "*ALL*", "*ALL*", null, null, false);
			}
			else {
				for (Iterator<String> iterator = adapters.iterator(); iterator.hasNext();) {
					String adapterName = iterator.next();
					getIbisManager().handleAdapter(action, "", adapterName, null, null, false);
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
	public Response updateAdapter(@PathParam("adapterName") String adapterName, LinkedHashMap<String, Object> json) throws ApiException {

		getAdapter(adapterName); //Check if the adapter exists!
		Response.ResponseBuilder response = Response.status(Response.Status.NO_CONTENT); //PUT defaults to no content

		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if(key.equalsIgnoreCase("action")) {//Start or stop an adapter!
				String action = null;

				if(value.equals("stop")) { action = "stopadapter"; }
				if(value.equals("start")) { action = "startadapter"; }

				getIbisManager().handleAdapter(action, "", adapterName, null, null, false);

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
	public Response updateReceiver(@PathParam("adapterName") String adapterName, @PathParam("receiverName") String receiverName, LinkedHashMap<String, Object> json) throws ApiException {

		Adapter adapter = getAdapter(adapterName);

		IReceiver receiver = adapter.getReceiverByName(receiverName);
		if(receiver == null) {
			throw new ApiException("Receiver ["+receiverName+"] not found!");
		}

		Response.ResponseBuilder response = Response.status(Response.Status.NO_CONTENT); //PUT defaults to no content

		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if(key.equalsIgnoreCase("action")) {//Start or stop an adapter!
				String action = null;

				if(value.equals("stop")) { action = "stopreceiver"; }
				else if(value.equals("start")) { action = "startreceiver"; }
				else if(value.equals("incthread")) { action = "incthreads"; }
				else if(value.equals("decthread")) { action = "decthreads"; }

				if(StringUtils.isEmpty(action))
					throw new ApiException("unknown or empty action ["+action+"]");

				getIbisManager().handleAdapter(action, "", adapterName, receiverName, null, false);
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
	public Response getAdapterFlow(@PathParam("name") String adapterName, @QueryParam("dot") boolean dot) throws ApiException {
		Adapter adapter = getAdapter(adapterName);

		FlowDiagramManager flowDiagramManager = getFlowDiagramManager();

		try {
			ResponseBuilder response = Response.status(Response.Status.OK);
			if(dot) {
				response.entity(flowDiagramManager.generateDot(adapter)).type(MediaType.TEXT_PLAIN);
			} else {
				response.entity(flowDiagramManager.get(adapter)).type("image/svg+xml");
			}
			return response.build();
		} catch (SAXException | TransformerException | IOException e) {
			throw new ApiException(e);
		}
	}

	private Map<String, Object> addCertificateInfo(WebServiceSender s) {
		String certificate = s.getCertificate();
		if (certificate == null || StringUtils.isEmpty(certificate))
			return null;

		Map<String, Object> certElem = new HashMap<String, Object>(4);
		certElem.put("name", certificate);
		String certificateAuthAlias = s.getCertificateAuthAlias();
		certElem.put("authAlias", certificateAuthAlias);
		URL certificateUrl = ClassUtils.getResourceURL(s.getConfigurationClassLoader(), certificate);
		if (certificateUrl == null) {
			certElem.put("url", null);
			certElem.put("info", "*** ERROR ***");
		} else {
			certElem.put("url", certificateUrl.toString());
			String certificatePassword = s.getCertificatePassword();
			CredentialFactory certificateCf = new CredentialFactory(certificateAuthAlias, null, certificatePassword);
			String keystoreType = s.getKeystoreType();
			certElem.put("info", getCertificateInfo(certificateUrl, certificateCf.getPassword(), keystoreType, "Certificate chain"));
		}
		return certElem;
	}

	private Map<String, Object> addCertificateInfo(HttpSender s) {
		String certificate = s.getCertificate();
		if (certificate == null || StringUtils.isEmpty(certificate))
			return null;

		Map<String, Object> certElem = new HashMap<String, Object>(4);
		certElem.put("name", certificate);
		String certificateAuthAlias = s.getCertificateAuthAlias();
		certElem.put("authAlias", certificateAuthAlias);
		URL certificateUrl = ClassUtils.getResourceURL(s.getConfigurationClassLoader(), certificate);
		if (certificateUrl == null) {
			certElem.put("url", "");
			certElem.put("info", "*** ERROR ***");
		} else {
			certElem.put("url", certificateUrl.toString());
			String certificatePassword = s.getCertificatePassword();
			CredentialFactory certificateCf = new CredentialFactory(certificateAuthAlias, null, certificatePassword);
			String keystoreType = s.getKeystoreType();
			certElem.put("info", getCertificateInfo(certificateUrl, certificateCf.getPassword(), keystoreType, "Certificate chain"));
		}
		return certElem;
	}

	private Map<String, Object> addCertificateInfo(FtpSender s) {
		String certificate = s.getCertificate();
		if (certificate == null || StringUtils.isEmpty(certificate))
			return null;

		Map<String, Object> certElem = new HashMap<String, Object>(4);
		certElem.put("name", certificate);
		String certificateAuthAlias = s.getCertificateAuthAlias();
		certElem.put("authAlias", certificateAuthAlias);
		URL certificateUrl = ClassUtils.getResourceURL(s.getConfigurationClassLoader(), certificate);
		if (certificateUrl == null) {
			certElem.put("url", "");
			certElem.put("info", "*** ERROR ***");
		} else {
			certElem.put("url", certificateUrl.toString());
			String certificatePassword = s.getCertificatePassword();
			CredentialFactory certificateCf = new CredentialFactory(certificateAuthAlias, null, certificatePassword);
			String keystoreType = s.getCertificateType();
			certElem.put("info", getCertificateInfo(certificateUrl, certificateCf.getPassword(), keystoreType, "Certificate chain"));
		}
		return certElem;
	}

	private ArrayList<Object> getCertificateInfo(final URL url, final String password, String keyStoreType, String prefix) {
		ArrayList<Object> certificateList = new ArrayList<Object>();
		try {
			KeyStore keystore = KeyStore.getInstance(keyStoreType);
			keystore.load(url.openStream(), password != null ? password.toCharArray() : null);
			if (log.isInfoEnabled()) {
				Enumeration<String> aliases = keystore.aliases();
				while (aliases.hasMoreElements()) {
					String alias = (String) aliases.nextElement();
					ArrayList<Object> infoElem = new ArrayList<Object>();
					infoElem.add(prefix + " '" + alias + "':");
					Certificate trustedcert = keystore.getCertificate(alias);
					if (trustedcert != null && trustedcert instanceof X509Certificate) {
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
		ArrayList<Object> pipes = new ArrayList<Object>(totalPipes);

		for (int i=0; i<totalPipes; i++) {
			Map<String, Object> pipesInfo = new HashMap<String, Object>();
			IPipe pipe = pipeline.getPipe(i);
			Map<String, PipeForward> pipeForwards = pipe.getForwards();

			String pipename = pipe.getName();

			Map<String, String> forwards = new HashMap<String, String>();
			for (PipeForward fwrd : pipeForwards.values()) {
				forwards.put(fwrd.getName(), fwrd.getPath());
			}

			pipesInfo.put("name", pipename);
			pipesInfo.put("forwards", forwards);
			if (pipe instanceof MessageSendingPipe) {
				MessageSendingPipe msp=(MessageSendingPipe)pipe;
				ISender sender = msp.getSender();
				pipesInfo.put("sender", ClassUtils.nameOf(sender));
				if (sender instanceof WebServiceSender) {
					WebServiceSender s = (WebServiceSender) sender;
					Map<String, Object> certInfo = addCertificateInfo(s);
					if(certInfo != null)
						pipesInfo.put("certificate", certInfo);
				}
				if (sender instanceof HttpSender) {
					HttpSender s = (HttpSender) sender;
					Map<String, Object> certInfo = addCertificateInfo(s);
					if(certInfo != null)
						pipesInfo.put("certificate", certInfo);
				}
				if (sender instanceof FtpSender) {
					FtpSender s = (FtpSender) sender;
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
						log.warn("Cannot determine number of messages in messageLog ["+messageLog.getName()+"]", e);
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

	private ArrayList<Object> mapAdapterReceivers(Adapter adapter, boolean showPendingMsgCount) {
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
					Map<String, Object> listenerInfo = new HashMap<String, Object>();
					IListener<?> listener=rb.getListener();
					listenerInfo.put("name", listener.getName());
					listenerInfo.put("class", ClassUtils.nameOf(listener));
					if (listener instanceof HasPhysicalDestination) {
						String pd = ((HasPhysicalDestination)rb.getListener()).getPhysicalDestinationName();
						listenerInfo.put("destination", pd);
					}
					if (listener instanceof HasSender) {
						sender = ((HasSender)listener).getSender();
					}
					//receiverInfo.put("hasInprocessStorage", ""+(rb.getInProcessStorage()!=null));
					IMessageBrowser ts = rb.getErrorStorageBrowser();
					receiverInfo.put("hasErrorStorage", (ts!=null));
					if (ts!=null) {
						try {
							if (showCountErrorStore) {
								receiverInfo.put("errorStorageCount", ts.getMessageCount());
							} else {
								receiverInfo.put("errorStorageCount", "?");
							}
						} catch (Exception e) {
							log.warn("Cannot determine number of messages in errorstore", e);
							receiverInfo.put("errorStorageCount", "error");
						}
					}
					ts=rb.getMessageLogBrowser();
					receiverInfo.put("hasMessageLog", (ts!=null));
					if (ts!=null) {
						try {
							if (showCountMessageLog) {
								receiverInfo.put("messageLogCount", ts.getMessageCount());
							} else {
								receiverInfo.put("messageLogCount", "?");
							}
						} catch (Exception e) {
							log.warn("Cannot determine number of messages in messageLog", e);
							receiverInfo.put("messageLogCount", "error");
						}
					}
					ts=rb.getInProcessBrowser();
					receiverInfo.put("hasInProcessLog", (ts!=null));
					if (ts!=null) {
						try {
							if (showCountMessageLog) {
								receiverInfo.put("inProcessLogCount", ts.getMessageCount());
							} else {
								receiverInfo.put("inProcessLogCount", "?");
							}
						} catch (Exception e) {
							log.warn("Cannot determine number of messages in inProcessLog", e);
							receiverInfo.put("inProcessLogCount", "error");
						}
					}
					boolean isRestListener = (listener instanceof RestListener);
					listenerInfo.put("isRestListener", isRestListener);
					if (isRestListener) {
						RestListener rl = (RestListener) listener;
						listenerInfo.put("restUriPattern", rl.getRestUriPattern());
						listenerInfo.put("isView", (rl.isView()==null?false:rl.isView()));
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

					receiverInfo.put("listener", listenerInfo);
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
		adapterInfo.put("started", adapterRunState.equals(RunStateEnum.STARTED));
		String state = adapterRunState.toString().toLowerCase().replace("*", "");
		adapterInfo.put("state", state);

		adapterInfo.put("configured", adapter.configurationSucceeded());
		adapterInfo.put("upSince", adapter.getStatsUpSinceDate().getTime());
		Date lastMessage = adapter.getLastMessageDateDate();
		adapterInfo.put("lastMessage", (lastMessage == null) ? null : lastMessage.getTime());
		adapterInfo.put("messagesInProcess", adapter.getNumOfMessagesInProcess());
		adapterInfo.put("messagesProcessed", adapter.getNumOfMessagesProcessed());
		adapterInfo.put("messagesInError", adapter.getNumOfMessagesInError());

		return adapterInfo;
	}
}
