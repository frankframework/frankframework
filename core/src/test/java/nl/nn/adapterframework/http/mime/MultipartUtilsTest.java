package nl.nn.adapterframework.http.mime;

import java.io.IOException;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

import org.junit.Test;

import nl.nn.adapterframework.http.PartMessage;

public class MultipartUtilsTest {

	@Test
	public void asdf() throws Exception {

		//TODO fix HttpSender#handleMultipartResponse(..)
		if(MultipartUtils.isMultipart(request)) {
			String multipartBodyName = listener.getMultipartBodyName();
			try {
				MimeMultipart mimeMultipart = MultipartUtils.parse(request);
				for (int i = 0; i < mimeMultipart.getCount(); i++) {
					BodyPart bodyPart = mimeMultipart.getBodyPart(i);
					String fieldName = bodyPart.getFieldName();
					if((i == 0 && multipartBodyName == null) || fieldName.equalsIgnoreCase(multipartBodyName)) {
						body = new PartMessage(bodyPart);
					} else {
						session.put("multipart" + i, new PartMessage(bodyPart));
					}
				}
			} catch(MessagingException e) {
				throw new IOException("Could not read mime multipart response", e);
			}
		}
	}
}
