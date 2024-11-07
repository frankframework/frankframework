/*
   Copyright 2019, 2021, 2024 WeAreFrank!

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
package org.frankframework.filesystem;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;

import jakarta.annotation.Nonnull;

import org.apache.commons.codec.binary.Base64InputStream;

import microsoft.exchange.webservices.data.property.complex.FileAttachment;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.stream.Message;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.XmlBuilder;

/**
 * FileSystem Sender extension to handle Attachments.
 */
public class FileSystemSenderWithAttachments<F, A, FS extends IMailFileSystem<F,A>> extends AbstractFileSystemSender<F,FS> {

	public final FileSystemActor.FileSystemAction[] ACTIONS_FS_WITH_ATTACHMENTS = {FileSystemActor.FileSystemAction.LISTATTACHMENTS};

	private static final boolean ATTACHMENTS_AS_SESSION_KEYS = false;

	@Override
	public void configure() throws ConfigurationException {
		addActions(Arrays.asList(ACTIONS_FS_WITH_ATTACHMENTS));
		super.configure();
	}

	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		if (getAction() != FileSystemActor.FileSystemAction.LISTATTACHMENTS) {
			return super.sendMessage(message, session);
		} else {

			IBasicFileSystem<F> ifs = getFileSystem();
			F file;

			try {
				file = ifs.toFile(message.asString());
			} catch (Exception e) {
				throw new SenderException("unable to get file", e);
			}

			XmlBuilder attachments = new XmlBuilder("attachments");
			IMailFileSystem<F,A> withAttachments = getFileSystem();
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
						if(!ATTACHMENTS_AS_SESSION_KEYS) {
							InputStream binaryInputStream = new ByteArrayInputStream(fileAttachment.getContent());
							InputStream base64 = new Base64InputStream(binaryInputStream,true);
							attachmentXml.setCdataValue(StreamUtil.streamToString(base64, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING));
						} else {
							attachmentXml.setValue(fileAttachment.getName());
							session.put(fileAttachment.getName(), fileAttachment.getContent());
						}
						attachments.addSubElement(attachmentXml);
					}
				}
			} catch (Exception e) {
				log.error("unable to list all attachments", e);
				throw new SenderException(e);
			}
			return new SenderResult(attachments.asMessage());
		}
	}


}
