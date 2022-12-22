package nl.nn.adapterframework.collection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;

import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;

public class TestCollector<E extends ICollectingElement> implements ICollector<E> {

		boolean open=true;

		StringWriter input;
		PipeLineSession session;
		ParameterValueList pvl;
		Object writingElement;

		OutputStream outputStream;
		MessageOutputStream messageOutputStream;

		public TestCollector() throws CollectionException {
			this(null,null,null);
		}

		public TestCollector(Message input, PipeLineSession session, ParameterValueList pvl) throws CollectionException {
			try {
				this.input = new StringWriter();
				if (input!=null) this.input.write(input.asString());
				this.session = session;
				this.pvl = pvl;
				if (input!=null && input.asString().equals("exception")) {
					throw new CollectionException("TestCollector exception");
				}
			} catch (IOException e) {
				throw new CollectionException(e);
			}
		}

		@Override
		public Message writeItem(Message input, PipeLineSession session, ParameterValueList pvl, E writingElement) throws CollectionException, TimeoutException {
			try {
				if (input!=null) this.input.write(input.asString());
				this.session = session;
				this.pvl = pvl;
				this.writingElement = writingElement;
				if (input.asString().equals("exception")) {
					throw new CollectionException("TestCollector exception");
				}
				if (input.asString().equals("timeout")) {
					throw new TimeoutException("TestCollector timeout");
				}
			} catch (IOException e) {
				throw new CollectionException(e);
			}
			return new Message("writeItem");
		}

		@Override
		public OutputStream streamItem(Message input, PipeLineSession session, ParameterValueList pvl, E writingElement) throws CollectionException {
			try {
				if (input!=null) this.input.write(input.asString());
				this.session = session;
				this.pvl = pvl;
				this.writingElement = writingElement;
				if (input.asString().equals("exception")) {
					throw new CollectionException("TestCollector exception");
				}
			} catch (IOException e) {
				throw new CollectionException(e);
			}
			outputStream = new ByteArrayOutputStream();
			return outputStream;
		}

		@Override
		public MessageOutputStream provideOutputStream(PipeLineSession session, ParameterValueList pvl, E writingElement) throws CollectionException {
			this.session = session;
			this.pvl = pvl;
			this.writingElement = writingElement;
			messageOutputStream = new MessageOutputStream((INamedObject)writingElement, input, (IForwardTarget)null);
			messageOutputStream.setResponse(new Message("writeItem"));
			return messageOutputStream;
		}


		@Override
		public void close() throws Exception {
			open=false;
		}

	}
