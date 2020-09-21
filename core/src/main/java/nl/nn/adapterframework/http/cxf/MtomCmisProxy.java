package nl.nn.adapterframework.http.cxf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.http.HttpResponseHandler;
import nl.nn.adapterframework.http.HttpServletBase;
import nl.nn.adapterframework.http.InputStreamDataSource;
import nl.nn.adapterframework.http.MultipartHttpSender;
import nl.nn.adapterframework.http.mime.MultipartEntityBuilder;
import nl.nn.adapterframework.lifecycle.IbisInitializer;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;

@IbisInitializer
public class MtomCmisProxy extends HttpServletBase {

	private static final long serialVersionUID = 1L;
	protected Logger log = LogUtil.getLogger(this);
	private MultipartHttpSender sender = null;

	private final String MULTIPART_XML_SESSIONKEY = "multipartXml";
	private final String URL_PARAMETER = "URL_param";
	private final String URL_PARAMETER_SESSIONKEY = "URL_param_sessionkey";
	private final String AUTHORIZATION_PARAMETER_SESSIONKEY = "AUTHORIZATION_param_sessionkey";

	//Always reply with MTOM
	private final boolean FORCE_MTOM_RESPONSE = AppConstants.getInstance().getBoolean("cmisproxy.FORCE_MTOM_RESPONSE", false);

	//reply with MTOM multipart
	private final boolean MTOM_RESPONSE_WHEN_MULTIPLE_PARTS = AppConstants.getInstance().getBoolean("cmisproxy.MTOM_RESPONSE_WHEN_MULTIPLE_PARTS", false);

	//reply with SOAP multipart
	private final boolean SOAP_WITH_ATTACHMENTS = AppConstants.getInstance().getBoolean("cmisproxy.SOAP_WITH_ATTACHMENTS_RESPONSE", false);

	@Override
	public void init() throws ServletException {
		super.init();

		try {
			createSender();
		} catch (Exception e) {
			log.error("failed to open cmis proxy-sender", e);
		}
	}

	@Override
	public void destroy() {
		try {
			sender.close();
		} catch (SenderException e) {
			log.error("failed to close cmis proxy-sender", e);
		}

		super.destroy();
	}

	private ContentType parseContentType(String contentType) {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		String mimeType = null;

		StringTokenizer nameValuePairs = new StringTokenizer(contentType, "; ");
		while (nameValuePairs.hasMoreTokens()) {
			String nameValuePair = nameValuePairs.nextToken();
			if(nameValuePair.indexOf("=") > -1) {
				String[] pair = nameValuePair.split("=");
				params.add(new BasicNameValuePair(pair[0], pair[1].replace("\"", "")));
			}
			else {
				mimeType = nameValuePair;
			}
		}

		return ContentType.create(mimeType, params.toArray(new NameValuePair[params.size()]));
	}

	private void createSender() throws ConfigurationException, SenderException {
		MultipartHttpSender sender = new MultipartHttpSender() {
			@Override
			public boolean isParamsInUrl() {
				return !getMethodType().equalsIgnoreCase("post");
			}
			@Override
			protected HttpRequestBase getMethod(URI uri, Message message, ParameterValueList parameters, IPipeLineSession session) throws SenderException {
				HttpRequestBase request = super.getMethod(uri, message, parameters, session);
				request.setHeader(HttpHeaders.AUTHORIZATION, (String) session.get(AUTHORIZATION_PARAMETER_SESSIONKEY));
				return request;
			}

			@Override
			protected Message extractResult(HttpResponseHandler responseHandler, IPipeLineSession session) throws SenderException, IOException {
				HttpServletResponse response = (HttpServletResponse) session.get(IPipeLineSession.HTTP_RESPONSE_KEY);
				OutputStream outputStream = response.getOutputStream();

				try {
//no parts			//text/xml;charset=UTF-8
//mtom soap			//multipart/related; type="application/xop+xml"; boundary="uuid:44797796-ab12-4b62-991c-738aff2d05a7"; start="<root.message@cxf.apache.org>"; start-info="text/xml"
//normal soap		//multipart/related; type="text/xml"; start="<rootpart@soapui.org>"; boundary="----=_Part_24_8773358.1567155468022"

//					if(responseHandler.getStatusLine().getStatusCode() > 300) {
//						outputStream.write(responseHandler.getStatusLine().toString().getBytes());
//						return "";
//					}
					ContentType contentType = responseHandler.getContentType();
					if(!contentType.getMimeType().contains("multipart") && !contentType.getMimeType().contains("xml")) {
						response.reset();
						Header[] headers = responseHandler.getAllHeaders();
						for (Header header : headers) { //copy all headers
							if(header.getName().equalsIgnoreCase("transfer-encoding")) continue; //Ignore this as it's set by the servlet
							response.setHeader(header.getName(), header.getValue());
						}
						Misc.streamToStream(responseHandler.getResponse(), outputStream);
						return Message.nullMessage();
					}
					//Als mimeType == text/html dan geen multipart doen :)

					InputStreamDataSource dataSource = new InputStreamDataSource(contentType.toString(), responseHandler.getResponse());
					MimeMultipart mimeMultipart = new MimeMultipart(dataSource);
					int count = mimeMultipart.getCount();

					//force cmis mtom response
					if(FORCE_MTOM_RESPONSE || (!SOAP_WITH_ATTACHMENTS && MTOM_RESPONSE_WHEN_MULTIPLE_PARTS && count > 1)) {
						response.reset();
						Header[] headers = responseHandler.getAllHeaders();
						for (Header header : headers) { //copy all headers
							if(header.getName().equalsIgnoreCase("transfer-encoding")) continue; //Ignore this as it's set by the servlet
							response.setHeader(header.getName(), header.getValue());
						}
						mimeMultipart.writeTo(outputStream);
					}
					else {
						HttpEntity entity = null;

						if(count == 1) { //reply with normal soap message
							BodyPart bodyPart = mimeMultipart.getBodyPart(0);

							ContentType parsedContentType = parseContentType(bodyPart.getContentType());
							String charset = parsedContentType.getParameter("charset");

							entity = new InputStreamEntity(bodyPart.getInputStream(), ContentType.TEXT_XML.withCharset(charset));
						}
						else { //multiple parts, check if SWA or MTOM

							MultipartEntityBuilder multipart = MultipartEntityBuilder.create();
							multipart.setMimeSubtype("related");

							if(!SOAP_WITH_ATTACHMENTS)
								multipart.setMtomMultipart();

							for (int i = 0; i < count; i++) {
								BodyPart bodyPart = mimeMultipart.getBodyPart(i);
								InputStream is = bodyPart.getInputStream();

								ContentType parsedContentType = null;
								if(i == 0)//Apparently IBM always returns this header, ala SWA but other parts are MTOM!?
									parsedContentType = ContentType.create("text/xml");
								else
									parsedContentType = parseContentType(bodyPart.getContentType());

								FormBodyPartBuilder fbpb = FormBodyPartBuilder.create();

								String[] partName = bodyPart.getHeader("content-id");
								if(partName != null && partName.length > 0) {
									String contentId = partName[0];
									contentId = contentId.substring(1, contentId.length()-1); //Remove pre and post-fix: < & >
									fbpb.setName(contentId);
								}
								else
									fbpb.setName("part"+i);

								fbpb.setBody(new InputStreamBody(is, parsedContentType, bodyPart.getFileName()));

								multipart.addPart(fbpb.build());
							}

							entity = multipart.build();
						}

						//man-o-man this is so ugly.. why are we doing this again?
						response.setHeader("content-type", entity.getContentType().getValue().replace("application/xop+", "text/"));
						entity.writeTo(outputStream);
					}
				}
				catch (Exception e) {
					log.error("failed to read parts", e);
				}
				finally {
					responseHandler.close();
				}

				return Message.nullMessage();
			}
		};

		sender.setMtomEnabled(true);
		sender.setMultipartXmlSessionKey(MULTIPART_XML_SESSIONKEY);
		sender.setUrlParam(URL_PARAMETER);

		Parameter url = new Parameter();
		url.setName(URL_PARAMETER);
		url.setSessionKey(URL_PARAMETER_SESSIONKEY);
		sender.addParameter(url);

		sender.setVerifyHostname(false);
		sender.setAllowSelfSignedCertificates(true);
		sender.configure();
		sender.open();

		this.sender = sender;
	}

	@Override
	public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;

		//New request, new session :)
		IPipeLineSession session = new PipeLineSessionBase();
		session.put(IPipeLineSession.HTTP_REQUEST_KEY, request);
		session.put(IPipeLineSession.HTTP_RESPONSE_KEY, response);

		String message = "";
		if(request.getMethod().equalsIgnoreCase("POST")) {
			InputStream body = null;
			String contentType = request.getHeader("content-type");
			if(!contentType.contains("multipart/related")) { //Not a multipart message but normal soap message
				body = request.getInputStream();
			}
			else { // handle as multipart (mtom or not)
				try {
					body = handleMultipartContent(request, session);
				} catch (MessagingException e) {
					log.error("unable to read/open multipart request message", e);
				}
			}
			message = Misc.streamToString(body);
		}

		AppConstants appConstants = AppConstants.getInstance();
		boolean secure = appConstants.getBoolean("cmisproxy.SECURE", request.isSecure());
		String protocol = (secure ? "https" : "http" )+"://";
		String host = appConstants.getString("cmisproxy.HOSTNAME", appConstants.getString("hostname", "localhost"));

		String defaultUrl = protocol + host + (request.getRequestURI().replace("/proxy/", "/"));
		String url = AppConstants.getInstance().getString("cmisproxy.URL", defaultUrl);
		session.put(URL_PARAMETER_SESSIONKEY, url);

		//authorization 
		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		if(StringUtils.isNotEmpty(authorization)) {
			session.put(AUTHORIZATION_PARAMETER_SESSIONKEY, authorization);
		}

		sender.setMethodType(request.getMethod());
		try {
			sender.sendMessage(new Message(message), session);
		} catch (Exception e) {
			log.error("error sending message to url ["+url+"]", e);
		}
	}

	private InputStream handleMultipartContent(HttpServletRequest request, IPipeLineSession session) throws MessagingException, IOException {
		InputStreamDataSource dataSource = new InputStreamDataSource(request.getHeader("content-type"), request.getInputStream());
		MimeMultipart mp = new MimeMultipart(dataSource);
		int count = mp.getCount();

		XmlBuilder attachments = new XmlBuilder("parts");
		InputStream body = null;
		for (int i = 0; i < count; i++) {
			BodyPart bp = mp.getBodyPart(i);

			if(i == 0) { //Skip the first part as this is the sender's input!
				body = bp.getInputStream();
			}
			else {
				XmlBuilder attachment = new XmlBuilder("part");

				attachment.addAttribute("type", "file");
				String sessionKeyName = "attachment"+i;

				String fileName = bp.getFileName();
				if(StringUtils.isEmpty(fileName))
					fileName = sessionKeyName;

				String[] partName = bp.getHeader("content-id");
				if(partName != null && partName.length > 0) {
					String contentId = partName[0];
					contentId = contentId.substring(1, contentId.length()-1); //Remove pre and post-fix: < & >

					attachment.addAttribute("name", contentId);
					attachment.addAttribute("filename", contentId);
				}
				else {
					attachment.addAttribute("name", fileName);
					attachment.addAttribute("filename", fileName);
				}

				attachment.addAttribute("size", bp.getSize());
				attachment.addAttribute("sessionKey", sessionKeyName);
				session.put(sessionKeyName, bp.getInputStream());

				String contentType = bp.getContentType();
				if(contentType != null) {
					String mimeType = contentType;
					int semicolon = contentType.indexOf(";");
					if(semicolon >= 0) {
						mimeType = contentType.substring(0, semicolon);
						String mightContainCharSet = contentType.substring(semicolon+1).trim();
						if(mightContainCharSet.contains("charset=")) {
							String charSet = mightContainCharSet.substring(mightContainCharSet.indexOf("charset=")+8);
							attachment.addAttribute("charSet", charSet);
						}
					}
					else {
						mimeType = contentType;
					}
					attachment.addAttribute("mimeType", mimeType);
				}

				attachments.addSubElement(attachment);
			}
		}
		session.put(MULTIPART_XML_SESSIONKEY, attachments.toXML());

		return body;
	}

	@Override
	public int loadOnStartUp() {
		return 0;
	}

	@Override
	public String getUrlMapping() {
		return "/cmis/proxy/*";
	}

	@Override
	public String[] getRoles() {
		return new String[] {"IbisWebService", "IbisTester"};
	}
}