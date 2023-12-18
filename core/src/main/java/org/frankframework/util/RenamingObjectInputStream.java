package org.frankframework.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

import org.frankframework.receivers.MessageWrapper;
import org.frankframework.stream.Message;

public class RenamingObjectInputStream extends ObjectInputStream {

	public RenamingObjectInputStream(InputStream in) throws IOException {
		super(in);
	}

	// Support old package names, already serialized in the database
	@Override
	protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
		ObjectStreamClass resultClassDescriptor = super.readClassDescriptor();

		if (resultClassDescriptor.getName().equals("nl.nn.adapterframework.receivers.MessageWrapper")) {
			resultClassDescriptor = ObjectStreamClass.lookup(MessageWrapper.class);
		}

		if (resultClassDescriptor.getName().equals("nl.nn.adapterframework.stream.Message")) {
			resultClassDescriptor = ObjectStreamClass.lookup(Message.class);
		}

		return resultClassDescriptor;
	}
}
