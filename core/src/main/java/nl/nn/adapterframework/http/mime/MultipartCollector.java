/*
   Copyright 2022 WeAreFrank!

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

import java.io.OutputStream;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.springframework.util.MimeType;

import lombok.Getter;
import nl.nn.adapterframework.collection.CollectionActor.Action;
import nl.nn.adapterframework.collection.CollectionException;
import nl.nn.adapterframework.collection.ICollector;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.http.HttpSender.PostType;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;

public class MultipartCollector implements ICollector<IMultipartCollectingElement> {

	public static final String PARAMETER_PARTNAME="partname";
	public static final String PARAMETER_MIMETYPE="mimeType";
	public static final String PARAMETER_FILENAME="filename";
	public static final String PARAMETER_CONTENTS="contents";

	private @Getter MultipartEntityBuilder entityBuilder;

	public MultipartCollector() {
		entityBuilder = MultipartEntityBuilder.create();
	}

	public static void configure(Action action, ParameterList parameterList) throws ConfigurationException {
		switch (action) {
			case OPEN:
				break;
			case WRITE:
			case LAST:
			case STREAM:
				break;
			case CLOSE:
				break;
			default:
				throw new ConfigurationException("unknwon action ["+action+"]");
		}
	}

	public static MultipartCollector openCollection(Message message, PipeLineSession session, ParameterValueList pvl, IMultipartCollectingElement writingElement) throws CollectionException {
		MultipartCollector collector = new MultipartCollector();
		if(writingElement.getPostType() == PostType.MTOM) {
			collector.getEntityBuilder().setMtomMultipart();
		}
		return collector;
	}


	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub
	}

	@Override
	public Message writeItem(Message input, PipeLineSession session, ParameterValueList pvl, IMultipartCollectingElement collectingElement) throws CollectionException, TimeoutException {
		String partname = ParameterValueList.getValue(pvl, PARAMETER_PARTNAME, collectingElement.getPartname());
		String mimeTypeStr = ParameterValueList.getValue(pvl, PARAMETER_MIMETYPE, collectingElement.getMimeType());
		String filename = ParameterValueList.getValue(pvl, PARAMETER_FILENAME, collectingElement.getFilename());
		Message contents = ParameterValueList.getValue(pvl, PARAMETER_CONTENTS, input);
		MimeType mimeType = null;
		if(StringUtils.isNotEmpty(mimeTypeStr)) {
			mimeType = MimeType.valueOf(mimeTypeStr);
		}
		entityBuilder.addPart(partname, new MessageContentBody(contents, mimeType, filename));
		return contents==input ? Message.nullMessage() : input;
	}

	@Override
	public OutputStream streamItem(Message input, PipeLineSession session, ParameterValueList pvl, IMultipartCollectingElement collectingElement) throws CollectionException {
		throw new NotImplementedException("Cannot Stream");
	}

	public HttpEntity getEntity() {
		return entityBuilder.build();
	}
}
