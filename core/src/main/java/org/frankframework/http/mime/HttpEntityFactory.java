/*
   Copyright 2024-2025 WeAreFrank!

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
package org.frankframework.http.mime;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.MIME;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import org.frankframework.core.PipeLineSession;
import org.frankframework.http.HttpEntityType;
import org.frankframework.http.HttpMessageEntity;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.util.DomBuilderException;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.XmlUtils;

/**
 * A reusable, thread-safe factory for any type of HTTP Entity as specified by {@link HttpEntityType}.
 */
@Log4j2
public class HttpEntityFactory {

	/**
	 * Builder for the HttpEntityFactory.
	 */
	public static class Builder {
		private HttpEntityType entityType;
		private ContentType contentType;
		private Set<String> parametersToUse = Set.of();
		private Set<String> parametersToSkipWhenEmpty = Set.of();
		private boolean rawWithParametersAppendsInputMessage = false;
		private String multipartXmlSessionKey;
		private String charSet = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
		private String firstBodyPartName;
		private String mtomContentTransferEncoding;

		public static Builder create() {
			return new Builder();
		}

		private Builder() {

		}

		public Builder entityType(HttpEntityType entityType) {
			this.entityType = entityType;
			return this;
		}

		public Builder contentType(ContentType contentType) {
			this.contentType = contentType;
			return this;
		}

		public Builder contentType(String contentType) {
			this.contentType = ContentType.create(contentType);
			return this;
		}

		public Builder parametersToUse(Set<String> parametersToUse) {
			this.parametersToUse = parametersToUse;
			return this;
		}

		public Builder parametersToSkipWhenEmpty(Set<String> parametersToSkipWhenEmpty) {
			this.parametersToSkipWhenEmpty = parametersToSkipWhenEmpty;
			return this;
		}

		public Builder multipartXmlSessionKey(String multipartXmlSessionKey) {
			this.multipartXmlSessionKey = multipartXmlSessionKey;
			return this;
		}

		public Builder charSet(String charSet) {
			this.charSet = charSet;
			return this;
		}

		public Builder firstBodyPartName(String firstBodyPartName) {
			this.firstBodyPartName = firstBodyPartName;
			return this;
		}

		public Builder mtomContentTransferEncoding(String mtomContentTransferEncoding) {
			this.mtomContentTransferEncoding = mtomContentTransferEncoding;
			return this;
		}

		public Builder rawWithParametersAppendsInputMessage(boolean rawWithParametersAppendsInputMessage) {
			this.rawWithParametersAppendsInputMessage = rawWithParametersAppendsInputMessage;
			return this;
		}

		public HttpEntityFactory build() {
			return new HttpEntityFactory(entityType, contentType, parametersToUse, parametersToSkipWhenEmpty, rawWithParametersAppendsInputMessage, multipartXmlSessionKey, charSet, firstBodyPartName, mtomContentTransferEncoding);
		}
	}

	private static final MimeType APPLICATION_XOP_XML = MimeType.valueOf("application/xop+xml");

	private final HttpEntityType entityType;
	private final ContentType contentType;
	private final Set<String> parametersToUse;
	private final Set<String> parametersToSkipWhenEmpty;
	private final boolean rawWithParametersAppendsInputMessage;
	private final String multipartXmlSessionKey;
	private final String charSet;
	private final String firstBodyPartName;
	private final String mtomContentTransferEncoding;

	private HttpEntityFactory(HttpEntityType entityType, ContentType contentType, Set<String> parametersToUse, Set<String> parametersToSkipWhenEmpty, boolean rawWithParametersAppendsInputMessage, String multipartXmlSessionKey, String charSet, String firstBodyPartName, String mtomContentTransferEncoding) {
		this.entityType = entityType;
		this.contentType = contentType;
		this.parametersToUse = Collections.unmodifiableSet(parametersToUse);
		this.parametersToSkipWhenEmpty = Collections.unmodifiableSet(parametersToSkipWhenEmpty);
		this.rawWithParametersAppendsInputMessage = rawWithParametersAppendsInputMessage;
		this.multipartXmlSessionKey = multipartXmlSessionKey;
		this.charSet = charSet;
		this.firstBodyPartName = firstBodyPartName;
		this.mtomContentTransferEncoding = mtomContentTransferEncoding;
	}


	@Nonnull
	public HttpEntity create(Message message, ParameterValueList parameters, PipeLineSession session) throws IOException {
		return switch (entityType) {
			case RAW -> createEntityForRawMessage(message, parameters);
			case BINARY -> new HttpMessageEntity(message, computeContentType(message));
			case URLENCODED -> createUrlEncodedFormEntity(message, parameters);
			case FORMDATA, MTOM -> createMultiPartEntity(message, parameters, session);
		};
	}

	@Nonnull
	private HttpEntity createEntityForRawMessage(Message message, ParameterValueList parameters) throws IOException {
		if (parameters == null || parameters.stream().noneMatch(p -> parametersToUse.contains(p.getDefinition().getName()))) {
			return new HttpMessageEntity(message, computeContentType(message));
		}

		String initialMessage = rawWithParametersAppendsInputMessage && message != null ? message.asString() : null;

		List<String> params = parameters.stream()
				.sequential()
				.filter(p -> parametersToUse.contains(p.getName()))
				.map(pv -> Map.entry(pv.getName(), pv.asStringValue("")))
				.filter(e -> !(StringUtils.isBlank(e.getValue()) && parametersToSkipWhenEmpty.contains(e.getKey())))
				.map(this::encodeAsUrlParameter)
				.collect(Collectors.toList());
		if (StringUtils.isNotBlank(initialMessage)) {
			params.add(0, initialMessage);
		}
		String msg = String.join("&", params);
		return new ByteArrayEntity(msg.getBytes(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING), computeContentType(message));
	}

	private ContentType computeContentType(Message message) {
		if (contentType != null) {
			return contentType;
		}
		MimeType mimeType = message != null ? message.getContext().getMimeType() : null;
		if (mimeType != null) {
			return ContentType.create(mimeType.getType() + "/" + mimeType.getSubtype(), mimeType.getCharset());
		}
		return null;
	}

	@SneakyThrows
	private String encodeAsUrlParameter(Map.Entry<String, String> entry) {
		return entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), charSet);
	}

	/**
	 * There is no definition for parameters with multiple values so both {@code ?id=1,2} and {@code ?id=1&id=2} are valid.
	 * For simplicity we use the latter.
	 *
	 * @see <a href="https://www.rfc-editor.org/rfc/rfc3986#section-3.4">RFC 3986 section 3.4 Query</a>
	 */
	@Nonnull
	private HttpEntity createUrlEncodedFormEntity(Message message, ParameterValueList parameters) throws IOException {
		List<NameValuePair> requestFormElements = new ArrayList<>();

		if (StringUtils.isNotEmpty(firstBodyPartName)) {
			requestFormElements.add(new BasicNameValuePair(firstBodyPartName, message.asString()));
			log.debug("appended parameter [{}] with value [{}]", firstBodyPartName, message);
		}
		if (parameters !=null) {
			for(ParameterValue pv : parameters) {
				String name = pv.getDefinition().getName();
				String value = pv.asStringValue("");

				if (parametersToUse.contains(name) && (StringUtils.isNotEmpty(value) || !parametersToSkipWhenEmpty.contains(name))) {
					requestFormElements.add(new BasicNameValuePair(name,value));
					log.debug("appended parameter [{}] with value [{}]", name, value);
				}
			}
		}
		try {
			return new UrlEncodedFormEntity(requestFormElements, charSet);
		} catch (UnsupportedEncodingException e) {
			throw new IOException("unsupported encoding for one or more POST parameters", e);
		}
	}

	private FormBodyPart createStringBodyPart(Message message) {
		MimeType mimeType = entityType == HttpEntityType.MTOM ? APPLICATION_XOP_XML : MediaType.TEXT_PLAIN; // only the first part is XOP+XML, other parts should use their own content-type
		FormBodyPartBuilder bodyPart = FormBodyPartBuilder.create(firstBodyPartName, new MessageContentBody(message, mimeType));

		// Should only be set when request is MTOM and it's the first BodyPart
		if (entityType == HttpEntityType.MTOM && StringUtils.isNotEmpty(mtomContentTransferEncoding)) {
			bodyPart.setField(MIME.CONTENT_TRANSFER_ENC, mtomContentTransferEncoding);
		}

		return bodyPart.build();
	}

	private HttpEntity createMultiPartEntity(Message message, ParameterValueList parameters, PipeLineSession session) throws IOException {
		MultipartEntityBuilder entity = MultipartEntityBuilder.create();

		entity.setCharset(Charset.forName(charSet));
		entity.setMtomMultipart(entityType == HttpEntityType.MTOM);

		if (StringUtils.isNotEmpty(firstBodyPartName)) {
			entity.addPart(createStringBodyPart(message));
			if (log.isDebugEnabled()) log.debug("appended stringpart [{}] with value [{}]", firstBodyPartName, message);
		}
		if (parameters!=null) {
			addParameters(entity, parameters, session);
		}

		if (StringUtils.isNotEmpty(multipartXmlSessionKey)) {
			addMultiPart(entity, session);
		}
		return entity.build();
	}

	private void addMultiPart(MultipartEntityBuilder entity, PipeLineSession session) throws IOException {
		String multipartXml = session.getString(multipartXmlSessionKey);
		log.debug("building multipart message with MultipartXmlSessionKey [{}]", multipartXml);
		if (StringUtils.isEmpty(multipartXml)) {
			log.warn("sessionKey [{}] is empty", multipartXmlSessionKey);
			return;
		}
		Element partsElement;
		try {
			partsElement = XmlUtils.buildElement(multipartXml);
		} catch (DomBuilderException e) {
			throw new IOException("error building multipart xml", e);
		}
		Collection<Node> parts = XmlUtils.getChildTags(partsElement, "part");
		if (parts.isEmpty()) {
			log.warn("no part(s) in multipart xml [{}]", multipartXml);
			return;
		}
		for (final Node part : parts) {
			Element partElement = (Element) part;
			entity.addPart(elementToFormBodyPart(partElement, session));
		}
	}

	private void addParameters(MultipartEntityBuilder entity, ParameterValueList parameters, PipeLineSession session) {
		parameters.stream()
				.sequential()
				.filter(parameter -> parametersToUse.contains(parameter.getName()))
				.forEach(parameter -> addParameter(entity, parameter, session));
	}

	private void addParameter(MultipartEntityBuilder entity, ParameterValue pv, PipeLineSession session) {
		String name = pv.getName();
		String fileName = null;
		String sessionKey = pv.getDefinition().getSessionKey();
		if (sessionKey != null) {
			fileName = session.getString(sessionKey + "Name");
		}
		if(fileName != null) {
			log.warn("setting filename using [{}Name] for bodypart [{}]. Consider using a MultipartXml with the attribute \"name\"=[{}] instead.", sessionKey, fileName, name);
		}

		Message msg = pv.asMessage();
		if (msg.isEmpty() && fileName == null && parametersToSkipWhenEmpty.contains(name)) {
			return;
		}

		entity.addPart(name, new MessageContentBody(msg, null, fileName));
		log.debug("appended bodypart [{}] with message [{}]", name, msg);
	}

	protected FormBodyPart elementToFormBodyPart(Element element, PipeLineSession session) {
		String part = element.getAttribute("name"); // Name of the part
		boolean isFile = "file".equals(element.getAttribute("type")); // text of file, empty == text
		String filename = element.getAttribute("filename"); // If type == file, the filename
		String partSessionKey = element.getAttribute("sessionKey"); // SessionKey to retrieve data from
		String partMimeType = element.getAttribute("mimeType"); // MimeType of the part
		Message partObject = session.getMessage(partSessionKey);
		if (Message.isEmpty(partObject) && element.hasAttribute("value")) {
			partObject = Message.asMessage(element.getAttribute("value"));
		}

		MimeType mimeType = null;
		if(StringUtils.isNotEmpty(partMimeType)) {
			partObject.getContext().withMimeType(partMimeType);
			mimeType = MimeType.valueOf(partMimeType);
		}

		final String filenameToUse;
		if(isFile || StringUtils.isNotBlank(filename)) {
			String filenameBackup = StringUtils.isBlank(part) ? partSessionKey : part;
			filenameToUse = StringUtils.isNotBlank(filename) ? filename : filenameBackup;
		} else {
			filenameToUse = null;
		}

		String partName = isFile || StringUtils.isBlank(part) ? partSessionKey : part;
		return FormBodyPartBuilder.create(partName, new MessageContentBody(partObject, mimeType, filenameToUse)).build();
	}
}
