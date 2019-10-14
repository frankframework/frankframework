package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

public class MiscTest {

	@Test
	public void testStreamToString() throws IOException {
		String tekst = "dit is een string";
		ByteArrayInputStream bais = new ByteArrayInputStream(tekst.getBytes());
		
		final Boolean inputStreamClosed = new Boolean(false);
		CloseChecker closeChecker = new CloseChecker(bais); 
		String actual = Misc.streamToString(closeChecker);
		
		assertEquals(tekst, actual);
		assertTrue("inputstream was not closed", closeChecker.inputStreamClosed);
	}
	
	private class CloseChecker extends FilterInputStream {

		boolean inputStreamClosed;
		
		public CloseChecker(InputStream arg0) {
			super(arg0);
		}

		@Override
		public void close() throws IOException {
			inputStreamClosed=true;;
			super.close();
		}
		
		
	}
}
