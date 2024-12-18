/*
   Copyright 2018 Nationale-Nederlanden, 2022, 2024 WeAreFrank!

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

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.Header;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.Args;

import org.frankframework.stream.Message;

/**
 * Builder for (mtom-)multipart {@link HttpEntity}s.
 * See <a href="https://www.w3.org/Protocols/rfc1341/7_2_Multipart.html">link</a>.
 */
public class MultipartEntityBuilder {

	/**
	 * The pool of ASCII chars to be used for generating a multipart boundary.
	 */
	private static final char[] MULTIPART_CHARS = "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

	private static final String DEFAULT_SUBTYPE = "form-data";
	private static final String MTOM_SUBTYPE = "related";

	private ContentType contentType;
	private String boundary = null;
	private Charset charset = null;
	private List<FormBodyPart> bodyParts = null;
	private boolean mtom = false;
	private String firstPart = null;

	@SuppressWarnings("java:S2245") // Random is good enough for this use case
	private static final Random RANDOM = new Random();

	public static MultipartEntityBuilder create() {
		return new MultipartEntityBuilder();
	}

	private MultipartEntityBuilder() {
	}

	public MultipartEntityBuilder setBoundary(String boundary) {
		this.boundary = boundary;
		return this;
	}

	public MultipartEntityBuilder setMtomMultipart() {
		mtom = true;
		return this;
	}

	public MultipartEntityBuilder setMtomMultipart(boolean mtom) {
		this.mtom = mtom;
		return this;
	}

	public MultipartEntityBuilder setMimeSubtype(String subType) {
		Args.notBlank(subType, "MIME subtype");
		this.contentType = ContentType.create("multipart/" + subType);
		return this;
	}

	public MultipartEntityBuilder setContentType(ContentType contentType) {
		Args.notNull(contentType, "Content type");
		this.contentType = contentType;
		return this;
	}

	public MultipartEntityBuilder setCharset(Charset charset) {
		this.charset = charset;
		return this;
	}

	public MultipartEntityBuilder addPart(FormBodyPart bodyPart) {
		if (bodyPart == null) {
			return this;
		}
		if (this.bodyParts == null) {
			this.bodyParts = new ArrayList<>();
		}

		if(mtom) {
			Header header = bodyPart.getHeader();
			String contentID;
			String fileName = bodyPart.getBody().getFilename();
			header.removeFields("Content-Disposition");
			if(fileName == null) {
				contentID = "<"+bodyPart.getName()+">";
			}
			else {
				bodyPart.addField("Content-Disposition", "attachment; name=\""+bodyPart.getName()+"\"; filename=\""+fileName+"\"");
				contentID = "<"+fileName+">";
			}
			bodyPart.addField("Content-ID", contentID);

			if(firstPart == null)
				firstPart = contentID;
		}

		this.bodyParts.add(bodyPart);
		return this;
	}

	public MultipartEntityBuilder addPart(String name, ContentBody contentBody) {
		Args.notNull(name, "Name");
		Args.notNull(contentBody, "Content body");
		return addPart(FormBodyPartBuilder.create(name, contentBody).build());
	}

	public MultipartEntityBuilder addPart(String name, Message message) {
		return addPart(name, new MessageContentBody(message));
	}

	/* utility methods */
	public MultipartEntityBuilder addTextBody(String name, final String text) {
		return addTextBody(name, text, ContentType.DEFAULT_TEXT); //ISO-8859-1
	}
	public MultipartEntityBuilder addTextBody(String name, String text, ContentType contentType) {
		return addPart(name, new StringBody(text, contentType));
	}

	public MultipartEntityBuilder addBinaryBody(String name, InputStream stream) {
		return addBinaryBody(name, stream, ContentType.DEFAULT_BINARY, null);
	}
	public MultipartEntityBuilder addBinaryBody(String name, InputStream stream, ContentType contentType, String filename) {
		return addPart(name, new InputStreamBody(stream, contentType, filename));
	}
	/* end utility methods */

	private String generateBoundary() {
		//See: https://tools.ietf.org/html/rfc2046#section-5.1.1
		StringBuilder buffer = new StringBuilder();
		if(mtom)
			buffer.append("----=_Part_");

		int count = RANDOM.nextInt(11) + 30; // a random size from 30 to 40
		for (int i = 0; i < count; i++) {
			buffer.append(MULTIPART_CHARS[RANDOM.nextInt(MULTIPART_CHARS.length)]);
		}
		return buffer.toString();
	}

	public MultipartEntity build() {
		String boundaryCopy = boundary;
		if (boundaryCopy == null && contentType != null) {
			boundaryCopy = contentType.getParameter("boundary");
		}
		if (boundaryCopy == null) {
			boundaryCopy = generateBoundary();
		}
		Charset charsetCopy = charset;
		if (charsetCopy == null && contentType != null) {
			charsetCopy = contentType.getCharset();
		}

		List<NameValuePair> paramsList = new ArrayList<>(5);
		paramsList.add(new BasicNameValuePair("boundary", boundaryCopy));
		if (charsetCopy != null) {
			paramsList.add(new BasicNameValuePair("charset", charsetCopy.name()));
		}

		String subtypeCopy = DEFAULT_SUBTYPE;
		if(mtom) {
			paramsList.add(new BasicNameValuePair("type", "application/xop+xml"));
			paramsList.add(new BasicNameValuePair("start", firstPart));
			paramsList.add(new BasicNameValuePair("start-info", "text/xml"));
			subtypeCopy = MTOM_SUBTYPE;
		}

		NameValuePair[] params = paramsList.toArray(new NameValuePair[paramsList.size()]);
		ContentType contentTypeCopy = contentType != null ?
				contentType.withParameters(params) :
				ContentType.create("multipart/" + subtypeCopy, params);

		List<FormBodyPart> bodyPartsCopy = bodyParts != null ? new ArrayList<>(bodyParts) : Collections.emptyList();
		MultipartForm form = new MultipartForm(charsetCopy, boundaryCopy, bodyPartsCopy);
		return new MultipartEntity(form, contentTypeCopy);
	}
}
