/*
   Copyright 2020 WeAreFrank!

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

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import nl.nn.adapterframework.stream.Message;

public interface IWithAttachments<F,A> extends IBasicFileSystem<F> {
	
	public Iterator<A> listAttachments(F f) throws FileSystemException;

	public String getAttachmentName(A a) throws FileSystemException;
	public Message readAttachment(A a) throws FileSystemException, IOException;
	public long getAttachmentSize(A a) throws FileSystemException;
	public String getAttachmentContentType(A a) throws FileSystemException;
	public String getAttachmentFileName(A a) throws FileSystemException;
	
	public F getFileFromAttachment(A a) throws FileSystemException;

	public Map<String, Object> getAdditionalAttachmentProperties(A a) throws FileSystemException;

}
