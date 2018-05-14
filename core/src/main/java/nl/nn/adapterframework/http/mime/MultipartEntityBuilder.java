/*
   Copyright 2018 Nationale-Nederlanden

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
package nl.nn.adapterframework.http.mime;

import java.io.File;
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
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.Args;

/**
 * Builder for (mtom-)multipart {@link HttpEntity}s.
 *
 * See: https://www.w3.org/Protocols/rfc1341/7_2_Multipart.html
 */
public class MultipartEntityBuilder {

	/**
	 * The pool of ASCII chars to be used for generating a multipart boundary.
	 */
	private final static char[] MULTIPART_CHARS =
			"-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
					.toCharArray();

	private final static String DEFAULT_SUBTYPE = "form-data";
	private final static String MTOM_SUBTYPE = "related";

	private ContentType contentType;
	private String boundary = null;
	private Charset charset = null;
	private List<FormBodyPart> bodyParts = null;
	private boolean mtom = false;
	private String firstPart = null;

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
			this.bodyParts = new ArrayList<FormBodyPart>();
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
				bodyPart.addField("Content-Disposition", "attachment; name=\""+fileName+"\"; filename=\""+fileName+"\"");
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

	public MultipartEntityBuilder addTextBody(String name, String text, ContentType contentType) {
		return addPart(name, new StringBody(text, contentType));
	}

	public MultipartEntityBuilder addTextBody(String name, final String text) {
		return addTextBody(name, text, ContentType.DEFAULT_TEXT);
	}

	public MultipartEntityBuilder addBinaryBody(String name, byte[] b, ContentType contentType, String filename) {
		return addPart(name, new ByteArrayBody(b, contentType, filename));
	}

	public MultipartEntityBuilder addBinaryBody(String name, byte[] b) {
		return addBinaryBody(name, b, ContentType.DEFAULT_BINARY, null);
	}

	public MultipartEntityBuilder addBinaryBody(String name, File file, ContentType contentType, String filename) {
		return addPart(name, new FileBody(file, contentType, filename));
	}

	public MultipartEntityBuilder addBinaryBody(String name, File file) {
		return addBinaryBody(name, file, ContentType.DEFAULT_BINARY, file != null ? file.getName() : null);
	}

	public MultipartEntityBuilder addBinaryBody(String name, InputStream stream, ContentType contentType, String filename) {
		return addPart(name, new InputStreamBody(stream, contentType, filename));
	}

	public MultipartEntityBuilder addBinaryBody(String name, InputStream stream) {
		return addBinaryBody(name, stream, ContentType.DEFAULT_BINARY, null);
	}

	private String generateBoundary() {
		//See: https://tools.ietf.org/html/rfc2046#section-5.1.1
		StringBuilder buffer = new StringBuilder();
		if(mtom)
			buffer.append("----=_Part_");

		Random rand = new Random();
		int count = rand.nextInt(11) + 30; // a random size from 30 to 40
		for (int i = 0; i < count; i++) {
			buffer.append(MULTIPART_CHARS[rand.nextInt(MULTIPART_CHARS.length)]);
		}
		return buffer.toString();
	}

	private MultipartEntity buildEntity() {
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

		List<NameValuePair> paramsList = new ArrayList<NameValuePair>(5);
		paramsList.add(new BasicNameValuePair("boundary", boundaryCopy));
		if (charsetCopy != null) {
			paramsList.add(new BasicNameValuePair("charset", charsetCopy.name()));
		}

		String subtypeCopy = DEFAULT_SUBTYPE;
		if(mtom) {
			paramsList.add(new BasicNameValuePair("type", "application/xop+xml"));
			paramsList.add(new BasicNameValuePair("start", firstPart));
			paramsList.add(new BasicNameValuePair("start-info", firstPart));
			subtypeCopy = MTOM_SUBTYPE;
		}

		NameValuePair[] params = paramsList.toArray(new NameValuePair[paramsList.size()]);
		ContentType contentTypeCopy = contentType != null ?
				contentType.withParameters(params) :
				ContentType.create("multipart/" + subtypeCopy, params);

		List<FormBodyPart> bodyPartsCopy = bodyParts != null ? new ArrayList<FormBodyPart>(bodyParts) :
				Collections.<FormBodyPart>emptyList();
		MultipartForm form = new MultipartForm(charsetCopy, boundaryCopy, bodyPartsCopy);
		return new MultipartEntity(form, contentTypeCopy, form.getTotalLength());
	}

	public HttpEntity build() {
		return buildEntity();
	}
}