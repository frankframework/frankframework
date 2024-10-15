package org.frankframework.extensions.sap.jco3;

import com.sap.conn.idoc.IDocDocument;
import com.sap.conn.idoc.IDocDocumentIterator;

import static org.mockito.Mockito.mock;

public class IDocDocumentIteratorDummy implements IDocDocumentIterator {

	private int index = 0;

	@Override
	public boolean hasNext() {
		index += 1;
		return index <= 2;
	}

	@Override
	public IDocDocument next() {
		return mock(IDocDocument.class);
	}
}
