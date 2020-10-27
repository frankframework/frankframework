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
package nl.nn.adapterframework.http.cxf;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.xml.soap.MimeHeader;
import javax.xml.soap.SOAPException;

import org.apache.axis.attachments.AttachmentPart;

/**
 * For some reason the getRawContent method was removed from Axis 1.4.
 * This class wraps around an AttachmentPart and makes it possible to retrieve the rawContent as InputStream.
 * 
 * @author Niels Meijer
 *
 */
public class InputStreamAttachmentPart extends AttachmentPart {

	private static final long serialVersionUID = 2L;

	public InputStreamAttachmentPart(javax.xml.soap.AttachmentPart attachment) throws SOAPException {
		//Init a super class with current attachment dataHandler to keep the content
		super(attachment.getDataHandler());

		//For some reason all headers are not copied over so we have to do that manually
		super.removeAllMimeHeaders();
		Iterator<?> headers = attachment.getAllMimeHeaders();
		while (headers.hasNext()) {
			MimeHeader header = (MimeHeader) headers.next();
			setMimeHeader(header.getName(), header.getValue());
		}
	}

	public InputStream getInputStream() throws SOAPException {
		try {
			return super.getDataHandler().getDataSource().getInputStream();
		} catch (IOException e) {
			throw new SOAPException("failed to retreive inputstream from attachment dataHander", e);
		}
	}
}
