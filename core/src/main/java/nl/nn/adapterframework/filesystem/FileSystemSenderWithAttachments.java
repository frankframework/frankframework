/*
   Copyright 2019 Integration Partners

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
package nl.nn.adapterframework.filesystem;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.codec.binary.Base64InputStream;

import microsoft.exchange.webservices.data.property.complex.FileAttachment;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * FileSystem Sender extension to handle Attachments.
 * 
 * 
 * <p><b>Actions:</b></p>
 * <br/>
 */

public class FileSystemSenderWithAttachments<F, A, FS extends IWithAttachments<F,A>> extends FileSystemSender<F,FS> {
	
	public final String[] ACTIONS_FS_WITH_ATTACHMENTS= {"listAttachments"};

	private boolean attachmentsAsSessionKeys=false;

	@Override
	public void configure() throws ConfigurationException {
		addActions(Arrays.asList(ACTIONS_FS_WITH_ATTACHMENTS));
		super.configure();
	}
	
	@Override
	public Object sendMessage(String correlationID, Message message, ParameterResolutionContext prc, MessageOutputStream target) throws SenderException, TimeOutException {
		if (!getAction().equalsIgnoreCase("listAttachments")) {
			return super.sendMessage(correlationID, message, prc, target);
		} else {

			IBasicFileSystem<F> ifs = getFileSystem();
			F file;
			
			try {
				file = ifs.toFile(message.toString());
			} catch (Exception e) {
				throw new SenderException(getLogPrefix() + "unable to get file", e);
			}
			
			XmlBuilder attachments = new XmlBuilder("attachments");
			IWithAttachments<F,A> withAttachments = getFileSystem();
			try {
				Iterator<A> it = withAttachments.listAttachments(file); 
				if (it!=null) {
					while (it.hasNext()) {
						A attachment = it.next();
						XmlBuilder attachmentXml = new XmlBuilder("attachment");
						attachmentXml.addAttribute("name", withAttachments.getAttachmentName(attachment));
						attachmentXml.addAttribute("contentType", withAttachments.getAttachmentContentType(attachment));
						attachmentXml.addAttribute("size", withAttachments.getAttachmentSize(attachment));
						attachmentXml.addAttribute("filename", withAttachments.getAttachmentFileName(attachment));
						
						FileAttachment fileAttachment = (FileAttachment) attachment;
						fileAttachment.load();
						if(!attachmentsAsSessionKeys) {
							InputStream binaryInputStream = new ByteArrayInputStream(fileAttachment.getContent());
							InputStream base64 = new Base64InputStream(binaryInputStream,true);
							attachmentXml.setCdataValue(Misc.streamToString(base64,Misc.DEFAULT_INPUT_STREAM_ENCODING));
						} else {
							attachmentXml.setValue(fileAttachment.getName());
							prc.getSession().put(fileAttachment.getName(), fileAttachment.getContent());
						}
						attachments.addSubElement(attachmentXml);
					}
				}
			} catch (Exception e) {
				log.error(e);
				throw new SenderException(e);
			}
			return attachments.toString();
			
		}
	}


}