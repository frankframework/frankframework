package nl.nn.adapterframework.extensions.cmis.mtom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.mail.BodyPart;
import javax.mail.internet.MimeMultipart;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

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

import nl.nn.adapterframework.http.InputStreamDataSource;
import nl.nn.adapterframework.http.mime.MultipartEntityBuilder;
import nl.nn.adapterframework.util.LogUtil;

public class MtomResponseWrapper extends HttpServletResponseWrapper {
	protected Logger log = LogUtil.getLogger(this);

	private ContentType contentType;

	public MtomResponseWrapper(ServletResponse response) throws IOException {
		this((HttpServletResponse) response);
	}

	public MtomResponseWrapper(HttpServletResponse response) throws IOException {
		super(response);
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {

		contentType = ContentType.parse(getContentType());
		if(log.isTraceEnabled()) log.trace("recieved response with ContentType ["+contentType+"]");

		// Als mimeType == text/html dan geen multipart doen :)
		if(!contentType.getMimeType().contains("multipart")) {
			return super.getOutputStream();
		} else {
			return new DelegatingServletOutputStream(new MtomOutputStream(super.getOutputStream()));
		}
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
				if(log.isTraceEnabled()) log.trace("writing response with ContentType ["+determinedContentType.getValue()+"]");

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

	}
}
