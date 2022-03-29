package nl.nn.adapterframework.http.cxf;

import java.io.IOException;

import javax.activation.DataHandler;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.SOAPMessage;

import org.apache.soap.util.mime.ByteArrayDataSource;
import org.springframework.util.MimeType;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageContext;

public class AttachmentPartMessage extends Message {

	public AttachmentPartMessage(AttachmentPart part) {
//		part.getDataHandler().getInputStream()
		part.getRawContentBytes();
	}

	public static AttachmentPart toAttachmentPart(SOAPMessage soapMessage, Message message) throws IOException {
		String name = (String) message.getContext().get(MessageContext.METADATA_NAME);
		MimeType mimeType = (MimeType) message.getContext().get(MessageContext.METADATA_MIMETYPE);

		AttachmentPart part = soapMessage.createAttachmentPart();
		DataHandler dataHander;
		if (message.isBinary()) {
			dataHander = new DataHandler(new ByteArrayDataSource(message.asByteArray(), mimeType.toString()));
		} else {
			dataHander = new DataHandler(new ByteArrayDataSource(message.asString(), mimeType.toString()));
		}
		part.setDataHandler(dataHander);
		part.setContentId(name);

		return part;
	}
}
