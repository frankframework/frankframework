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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IDataIterator;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.pipes.IteratingPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;

public class ForEachAttachmentPipe<F, A, FS extends IWithAttachments<F,A>> extends IteratingPipe<A> {

	private Set<String> onlyProperties;
	private Set<String> excludeProperties;
	
	private FS fileSystem;
	
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		getFileSystem().configure();
	}
	
	@Override
	public void start() throws PipeStartException {
		super.start();
		try {
			FS fileSystem=getFileSystem();
			fileSystem.open();
		} catch (FileSystemException e) {
			throw new PipeStartException("Cannot open fileSystem",e);
		}
	}
	
	@Override
	public void stop()  {
		try {
			getFileSystem().close();
		} catch (FileSystemException e) {
			log.warn("Cannot close fileSystem",e);
		}
		super.stop();
	}

	private class AttachmentIterator implements IDataIterator<A> {

		private Iterator<A> it;
		
		AttachmentIterator(Iterator<A> it) {
			this.it=it;
		}
		
		@Override
		public boolean hasNext() throws SenderException {
			return it.hasNext();
		}

		@Override
		public A next() throws SenderException {
			return it.next();
		}

		@Override
		public void close() throws SenderException {
			// no action required
		}
	}
	
	@Override
	protected IDataIterator<A> getIterator(Message message, PipeLineSession session, Map<String,Object> threadContext) throws SenderException {
		
		FS ifs = getFileSystem();
		
		try {
			F file = ifs.toFile(message.asString());
			Iterator<A> it = ifs.listAttachments(file);
			return new AttachmentIterator(it);
		} catch (Exception e) {
			throw new SenderException("unable to get file", e);
		}
	}

	@Override
	protected Message itemToMessage(A item) throws SenderException {
		FS ifs = getFileSystem();
		XmlBuilder result=new XmlBuilder("attachment");
		try {
			result.addAttribute("name", ifs.getAttachmentName(item));
			result.addAttribute("filename", ifs.getAttachmentFileName(item));
			result.addAttribute("contentType", ifs.getAttachmentContentType(item));
			result.addAttribute("size", ifs.getAttachmentSize(item));
			Map<String,Object> attachmentProperties = ifs.getAdditionalAttachmentProperties(item);
			if (attachmentProperties!=null) {
				XmlBuilder properties = new XmlBuilder("properties");
				Set<String> excludes=getExcludeProperties();
				Set<String> includes=getOnlyProperties();
				if (excludes!=null || includes==null) {
					for(Entry<String,Object>entry:attachmentProperties.entrySet()) {
						if (excludes==null || !excludes.contains(entry.getKey())) {
							XmlBuilder property = new XmlBuilder("property");
							property.addAttribute("name", entry.getKey());
							if (entry.getValue()!=null) {
								property.setValue(entry.getValue().toString());
							}
							properties.addSubElement(property);
						}
					}
				} else {
					for(String key:includes) {
						XmlBuilder property = new XmlBuilder("property");
						property.addAttribute("name", key);
						Object value=attachmentProperties.get(key);
						if (value!=null) {
							property.setValue(value.toString());
						}
						properties.addSubElement(property);
					}
				}
				result.addSubElement(properties);
			}
		} catch (Exception e) {
			throw new SenderException("unable to read attachment attributes", e);
		}
		return new Message(result.toXML());
	}

	
	public String getPhysicalDestinationName() {
		if (getFileSystem() instanceof HasPhysicalDestination) {
			return ((HasPhysicalDestination)getFileSystem()).getPhysicalDestinationName();
		}
		return null;
	}

	public FS getFileSystem() {
		return fileSystem;
	}

	public void setFileSystem(FS fileSystem) {
		this.fileSystem = fileSystem;
	}

	
	@IbisDoc({"comma separated list of attachment properties to list", ""})
	public void setOnlyProperties(String onlyPropertiesList) {
		if (onlyProperties==null) {
			onlyProperties=new LinkedHashSet<String>();
		}
		Misc.addItemsToList(onlyProperties,onlyPropertiesList,"properties to list",false);
	}
	public Set<String> getOnlyProperties() {
		return onlyProperties;
	}

	@IbisDoc({"comma separated list of attachment properties not to list. When specified, 'onlyProperties' is ignored", ""})
	public void setExcludeProperties(String excludePropertiesList) {
		if (excludeProperties==null) {
			excludeProperties=new LinkedHashSet<String>();
		}
		Misc.addItemsToList(excludeProperties,excludePropertiesList,"properties not to list",false);
	}
	public Set<String> getExcludeProperties() {
		return excludeProperties;
	}

}
