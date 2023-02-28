/*
   Copyright 2016-2018, 2020 Nationale-Nederlanden, 2021, 2022 WeAreFrank!

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
package nl.nn.adapterframework.pipes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.http.HttpSender;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.StreamUtil;

/**
 * Stream an input stream to an output stream.
 *
 * @ff.parameter inputStream 		the input stream object to use instead of an input stream object taken from pipe input
 * @ff.parameter outputStream		the output stream object to use unless httpResponse parameter is specified
 * @ff.parameter httpResponse		an HttpServletResponse object to stream to (the output stream is retrieved by calling getOutputStream() on the HttpServletResponse object)
 * @ff.parameter httpRequest		an HttpServletRequest object to stream from. Each part is put in a session key and the result of this pipe is a xml with info about these parts and the name of the session key
 * @ff.parameter contentType		the Content-Type header to set in case httpResponse was specified
 * @ff.parameter contentDisposition	the Content-Disposition header to set in case httpResponse was specified
 * @ff.parameter redirectLocation	the redirect location to set in case httpResponse was specified
 *
 * @ff.forward antiVirusFailed the virus checking indicates a problem with the message
 *
 * @author Jaco de Groot
 */
public class StreamPipe extends FixedForwardPipe {
	public static final String ANTIVIRUS_FAILED_FORWARD = "antiVirusFailed";

	private boolean extractFirstStringPart = false;
	private String multipartXmlSessionKey = "multipartXml";
	private boolean checkAntiVirus = false;
	private String antiVirusPartName = "antivirus_rc";
	private String antiVirusMessagePartName = "antivirus_msg";
	private String antiVirusPassedMessage = "Pass";
	private boolean antiVirusFailureAsSoapFault = false;
	private String antiVirusFailureReasonSessionKey;

	private class AntiVirusObject {
		private String fileName;
		private String status;
		private String message;

		public AntiVirusObject(String fileName, String status, String message) {
			this.fileName = fileName;
			this.status = status;
			this.message = message;
		}

		public String getFileName() {
			return fileName;
		}

		public String getStatus() {
			return status;
		}

		public String getMessage() {
			return message;
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		Object result = message;
		Map<String,Object> parameters = null;
		ParameterList parameterList = getParameterList();
		if (parameterList != null) {
			try {
				parameters = parameterList.getValues(message, session).getValueMap();
			} catch (ParameterException e) {
				throw new PipeRunException(this, "Could not resolve parameters", e);
			}
		}
		InputStream inputStream = null;
		OutputStream outputStream = null;
		HttpServletRequest httpRequest = null;
		HttpServletResponse httpResponse = null;
		String contentType = null;
		String contentDisposition = null;
		String redirectLocation = null;
		if (parameters != null) {
			if (parameters.get("inputStream") != null) {
				inputStream = (InputStream) parameters.get("inputStream");
			}
			if (parameters.get("outputStream") != null) {
				outputStream = (OutputStream) parameters.get("outputStream");
			}
			if (parameters.get("httpRequest") != null) {
				httpRequest = (HttpServletRequest) parameters.get("httpRequest");
			}
			if (parameters.get("httpResponse") != null) {
				httpResponse = (HttpServletResponse) parameters.get("httpResponse");
			}
			if (parameters.get("contentType") != null) {
				contentType = (String) parameters.get("contentType");
			}
			if (parameters.get("contentDisposition") != null) {
				contentDisposition = (String) parameters.get("contentDisposition");
			}
			if (parameters.get("redirectLocation") != null) {
				redirectLocation = (String) parameters.get("redirectLocation");
			}
		}
		try {
			if (inputStream == null) {
				inputStream = message.asInputStream();
			}
			if (httpResponse != null) {
				HttpSender.streamResponseBody(inputStream, contentType, contentDisposition, httpResponse, log, "", redirectLocation);
			} else if (httpRequest != null) {
				StringBuilder partsString = new StringBuilder("<parts>");
				String firstStringPart = null;
				List<AntiVirusObject> antiVirusObjects = new ArrayList<AntiVirusObject>();
				if (ServletFileUpload.isMultipartContent(httpRequest)) {
					log.debug("request with content type [{}] and length [{}] contains multipart content", httpRequest.getContentType(), httpRequest.getContentLength());
					DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory();
					ServletFileUpload servletFileUpload = new ServletFileUpload(diskFileItemFactory);
					List<FileItem> items = servletFileUpload.parseRequest(httpRequest);
					int fileCounter = 0;
					int stringCounter = 0;
					log.debug("multipart request items size [{}]", items.size());
					String lastFoundFileName = null;
					String lastFoundAVStatus = null;
					String lastFoundAVMessage = null;
					for (FileItem item : items) {
						if (item.isFormField()) {
							// Process regular form field (input
							// type="text|radio|checkbox|etc", select, etc).
							String fieldValue = item.getString();
							String fieldName = item.getFieldName();
							if (isCheckAntiVirus() && fieldName.equalsIgnoreCase(getAntiVirusPartName())) {
								log.debug("found antivirus status part [{}] with value [{}]", fieldName, fieldValue);
								lastFoundAVStatus = fieldValue;
							} else if (isCheckAntiVirus() && fieldName.equalsIgnoreCase(getAntiVirusMessagePartName())) {
								log.debug("found antivirus message part [{}] with value [{}]", fieldName, fieldValue);
								lastFoundAVMessage = fieldValue;
							} else {
								log.debug("found string part [{}] with value [{}]", fieldName, fieldValue);
								if (isExtractFirstStringPart() && firstStringPart == null) {
									firstStringPart = fieldValue;
								} else {
									String sessionKeyName = "part_string" + (++stringCounter > 1 ? stringCounter : "");
									addSessionKey(session, sessionKeyName, fieldValue);
									partsString.append("<part type=\"string\" name=\"" + fieldName + "\" sessionKey=\"" + sessionKeyName + "\" size=\"" + fieldValue.length() + "\"/>");
								}
							}
						} else {
							// Process form file field (input type="file").
							if (lastFoundFileName != null
									&& lastFoundAVStatus != null) {
								antiVirusObjects.add(new AntiVirusObject(
										lastFoundFileName, lastFoundAVStatus,
										lastFoundAVMessage));
								lastFoundFileName = null;
								lastFoundAVStatus = null;
								lastFoundAVMessage = null;
							}
							log.debug("found file part [{}]", item.getName());
							String sessionKeyName = "part_file" + (++fileCounter > 1 ? fileCounter : "");
							String fileName = FilenameUtils.getName(item.getName());
							InputStream is = item.getInputStream();
							int size = is.available();
							String mimeType = item.getContentType();
							if (size > 0) {
								addSessionKey(session, sessionKeyName, is, fileName);
							} else {
								addSessionKey(session, sessionKeyName, null);
							}
							partsString.append("<part type=\"file\" name=\"" + fileName + "\" sessionKey=\"" + sessionKeyName + "\" size=\"" + size + "\" mimeType=\"" + mimeType + "\"/>");
							lastFoundFileName = fileName;
						}
					}
					if (lastFoundFileName != null && lastFoundAVStatus != null) {
						antiVirusObjects.add(new AntiVirusObject(lastFoundFileName, lastFoundAVStatus, lastFoundAVMessage));
					}
				} else {
					log.debug("request with content type [{}] and length [{}] does NOT contain multipart content", httpRequest.getContentType(), httpRequest.getContentLength());
				}
				partsString.append("</parts>");
				if (isExtractFirstStringPart()) {
					result = adjustFirstStringPart(firstStringPart, session);
					session.put(getMultipartXmlSessionKey(), partsString.toString());
				} else {
					result = partsString.toString();
				}
				if (!antiVirusObjects.isEmpty()) {
					for (AntiVirusObject antiVirusObject : antiVirusObjects) {
						if (!antiVirusObject.getStatus().equalsIgnoreCase(getAntiVirusPassedMessage())) {
							String errorMessage = "multipart contains file [" + antiVirusObject.getFileName() + "] with antivirus status [" + antiVirusObject.getStatus() + "] and message [" + StringUtils.defaultString(antiVirusObject.getMessage()) + "]";
							PipeForward antiVirusFailedForward = findForward(ANTIVIRUS_FAILED_FORWARD);
							if (antiVirusFailedForward == null) {
								throw new PipeRunException(this, errorMessage);
							}
							if (antiVirusFailureAsSoapFault) {
								errorMessage = createSoapFaultMessage(errorMessage).asString();
							}
							if (StringUtils.isEmpty(getAntiVirusFailureReasonSessionKey())) {
								return new PipeRunResult(antiVirusFailedForward, errorMessage);
							}
							session.put(getAntiVirusFailureReasonSessionKey(), errorMessage);
							return new PipeRunResult(antiVirusFailedForward, result);
						}
					}
				}
			} else {
				StreamUtil.streamToStream(inputStream, outputStream);
			}
		} catch (IOException e) {
			throw new PipeRunException(this, "IOException streaming input to output", e);
		} catch (FileUploadException e) {
			throw new PipeRunException(this, "FileUploadException getting multiparts from httpServletRequest", e);
		}
		return new PipeRunResult(getSuccessForward(), result);
	}

	protected String adjustFirstStringPart(String firstStringPart, PipeLineSession session) throws PipeRunException {
		if (firstStringPart == null) {
			return "";
		} else {
			return firstStringPart;
		}
	}

	private Message createSoapFaultMessage(String errorMessage) throws PipeRunException {
		try {
			return SoapWrapper.getInstance().createSoapFaultMessage(errorMessage);
		} catch (ConfigurationException e) {
			throw new PipeRunException(this, "Could not get soapWrapper instance", e);
		}
	}

	private void addSessionKey(PipeLineSession session, String key, Object value) {
		addSessionKey(session, key, value, null);
	}

	private void addSessionKey(PipeLineSession session, String key, Object value, String name) {
		if (log.isDebugEnabled()) {
			String message = "setting sessionKey [" + key + "] to ";
			if (value instanceof InputStream) {
				message = message + "input stream of file [" + name + "]";
			} else {
				message = message + "[" + value + "]";
			}
			log.debug(message);
		}
		session.put(key, value);
	}

	/**
	 * (only used for parameter <code>httprequest</code>) when true the first part is not put in a session key but returned to the pipeline (as the result of this pipe)
	 * @ff.default false
	 */
	public void setExtractFirstStringPart(boolean b) {
		extractFirstStringPart = b;
	}

	public boolean isExtractFirstStringPart() {
		return extractFirstStringPart;
	}

	public String getMultipartXmlSessionKey() {
		return multipartXmlSessionKey;
	}

	/**
	 * (only used when <code>extractfirststringpart=true</code>) the session key to put the xml in with info about the stored parts
	 * @ff.default <code>multipartxml</code>
	 */
	public void setMultipartXmlSessionKey(String multipartXmlSessionKey) {
		this.multipartXmlSessionKey = multipartXmlSessionKey;
	}

	/**
	 * (only used for parameter <code>httprequest</code>) when true parts are checked for antivirus scan returncode. these antivirus scan parts have been added by another application (so the antivirus scan is not performed in this pipe). for each file part an antivirus scan part have been added by this other application (directly after this file part)
	 * @ff.default false
	 */
	public void setCheckAntiVirus(boolean b) {
		checkAntiVirus = b;
	}

	public boolean isCheckAntiVirus() {
		return checkAntiVirus;
	}

	public String getAntiVirusPartName() {
		return antiVirusPartName;
	}

	/**
	 * (only used for parameter <code>httprequest</code> and when <code>checkantivirus=true</code>) name of antivirus scan status parts
	 * @ff.default <code>antivirus_rc</code>
	 */
	public void setAntiVirusPartName(String antiVirusPartName) {
		this.antiVirusPartName = antiVirusPartName;
	}

	public String getAntiVirusMessagePartName() {
		return antiVirusMessagePartName;
	}

	/**
	 * (only used for parameter <code>httprequest</code> and when <code>checkantivirus=true</code>) name of antivirus scan message parts
	 * @ff.default <code>antivirus_msg</code>
	 */
	public void setAntiVirusMessagePartName(String antiVirusMessagePartName) {
		this.antiVirusMessagePartName = antiVirusMessagePartName;
	}

	public String getAntiVirusPassedMessage() {
		return antiVirusPassedMessage;
	}

	/**
	 * (only used for parameter <code>httprequest</code> and when <code>checkantivirus=true</code>) message of antivirus scan parts which indicates the antivirus scan passed
	 * @ff.default <code>pass</code>
	 */
	public void setAntiVirusPassedMessage(String antiVirusPassedMessage) {
		this.antiVirusPassedMessage = antiVirusPassedMessage;
	}

	/**
	 * (only used for parameter <code>httprequest</code> and when <code>checkantivirus=true</code>) when true and the antivirusfailed forward is specified and the antivirus scan did not pass, a soap fault is returned instead of only a plain error message
	 * @ff.default false
	 */
	public void setAntiVirusFailureAsSoapFault(boolean b) {
		antiVirusFailureAsSoapFault = b;
	}

	public boolean getAntiVirusFailureAsSoapFault() {
		return antiVirusFailureAsSoapFault;
	}

	/** (only used for parameter <code>httprequest</code> and when <code>checkantivirus=true</code>) if not empty and the antivirusfailed forward is specified and the antivirus scan did not pass, the error message (or soap fault) is stored in this session key and the first string part is returned to the pipeline */
	public void setAntiVirusFailureReasonSessionKey(String antiVirusFailureReasonSessionKey) {
		this.antiVirusFailureReasonSessionKey = antiVirusFailureReasonSessionKey;
	}

	public String getAntiVirusFailureReasonSessionKey() {
		return antiVirusFailureReasonSessionKey;
	}
}
