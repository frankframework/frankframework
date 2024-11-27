/*
   Copyright 2021-2023 WeAreFrank!

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
package org.frankframework.extensions.cmis.mtom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import jakarta.mail.BodyPart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.Logger;
import org.springframework.mock.web.DelegatingServletOutputStream;

import org.frankframework.http.InputStreamDataSource;
import org.frankframework.http.mime.MultipartEntityBuilder;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StringUtil;

public class MtomResponseWrapper extends HttpServletResponseWrapper {
	protected Logger log = LogUtil.getLogger(this);

	private ContentType contentType;

	public MtomResponseWrapper(ServletResponse response) {
		this((HttpServletResponse) response);
	}

	public MtomResponseWrapper(HttpServletResponse response) {
		super(response);
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {

		contentType = ContentType.parse(getContentType());
		log.trace("recieved response with ContentType [{}]", contentType);

		// Als mimeType == text/html dan geen multipart doen :)
		if(!contentType.getMimeType().contains("multipart")) {
			return super.getOutputStream();
		}
		return new DelegatingServletOutputStream(new MtomOutputStream(super.getOutputStream()));
	}

	/**
	 * TODO: Change this to a OutputStreamBuffer which, while being written do, directly changes the headers and bodypart boundaries.
	 */
	private class MtomOutputStream extends FilterOutputStream {
		private ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();

		public MtomOutputStream(OutputStream out) {
			super(out);
		}

		@Override
		public void write(int b) throws IOException {
			bufferStream.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			bufferStream.write(b, off, len);
		}

		@Override
		public synchronized void flush() throws IOException {
			try {
				ByteArrayInputStream is = new ByteArrayInputStream(bufferStream.toByteArray());
				InputStreamDataSource dataSource = new InputStreamDataSource(contentType.toString(), is);
				MimeMultipart mimeMultipart = new MimeMultipart(dataSource);
				int count = mimeMultipart.getCount();

				HttpEntity entity;

				if(count == 1) { //only mtom when there is more then 1 part.
					BodyPart bodyPart = mimeMultipart.getBodyPart(0);

					ContentType parsedContentType = parseContentType(bodyPart.getContentType());
					String charset = parsedContentType.getParameter("charset");

					entity = new InputStreamEntity(bodyPart.getInputStream(), ContentType.TEXT_XML.withCharset(charset));
				} else {
					MultipartEntityBuilder multipart = MultipartEntityBuilder.create();
					multipart.setMtomMultipart();

					for(int i = 0; i < count; i++) {
						BodyPart bodyPart = mimeMultipart.getBodyPart(i);

						ContentType parsedContentType = null;
						if(i == 0) //Apparently with IBM CMIS the first part always returns this header, ala SWA but other parts are MTOM!?
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

						fbpb.setBody(new InputStreamBody(bodyPart.getInputStream(), parsedContentType, bodyPart.getFileName()));

						multipart.addPart(fbpb.build());
					}
					entity = multipart.build();
				}

				Header determinedContentType = entity.getContentType();
				log.trace("writing response with ContentType [{}]", determinedContentType::getValue);

				setContentType(determinedContentType.getValue());
				entity.writeTo(out);
			} catch (Exception e) {
				log.error("unable to parse or convert message", e);
				throw new IOException("unable to parse message", e);
			}

			super.flush();
		}

		private ContentType parseContentType(String contentType) {
			List<NameValuePair> params = new ArrayList<>();
			String mimeType = null;

			List<String> nameValuePairs = StringUtil.split(contentType, "; ");
			for (String nameValuePair : nameValuePairs) {
				if(nameValuePair.contains("=")) {
					String[] pair = nameValuePair.split("=");
					params.add(new BasicNameValuePair(pair[0], pair[1].replace("\"", "")));
				}
				else {
					mimeType = nameValuePair;
				}
			}

			return ContentType.create(mimeType, params.toArray(new NameValuePair[params.size()]));
		}

	}
}
