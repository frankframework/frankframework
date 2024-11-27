package org.frankframework.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.OutputStream;
import java.io.Writer;
import java.sql.ResultSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.core.SenderException;
import org.frankframework.dbms.IDbmsSupport;

class Result2BlobWriterTest {
	private final IDbmsSupport dbmsSupport = mock(IDbmsSupport.class);
	private final ResultSet rs = mock(ResultSet.class);
	private final Result2BlobWriter writer = new Result2BlobWriter();

	@BeforeEach
	void setUp() {
		writer.querySender = new FixedQuerySender();
	}

	@AfterEach
	void tearDown() throws SenderException {
		writer.querySender.stop();
		writer.close();
	}

	@Test
	public void getLobHandleReturnsCorrectValue() throws Exception {
		when(dbmsSupport.getBlobHandle(rs, 1)).thenReturn("BlobHandle");
		Object result = writer.getLobHandle(dbmsSupport, rs);
		assertEquals("BlobHandle", result);
	}

	@Test
	public void updateLobUpdatesCorrectly() throws Exception {
		writer.updateLob(dbmsSupport, "BlobHandle", rs);
		verify(dbmsSupport).updateBlob(rs, 1, "BlobHandle");
	}

	@Test
	public void getWriterReturnsCorrectWriter() throws Exception {
		when(dbmsSupport.getBlobOutputStream(rs, 1, "BlobHandle")).thenReturn(new OutputStream() {
			@Override
			public void write(int b) {
				// NOOP
			}
		});
		writer.setBlobsCompressed(false);
		Writer blobHandleWriter = writer.getWriter(dbmsSupport, "BlobHandle", rs);
		assertNotNull(blobHandleWriter);
	}

	@Test
	public void getWriterThrowsExceptionForNullCharset() {
		assertThrows(SenderException.class, () -> writer.getWriter(dbmsSupport, "BlobHandle", rs));
	}
}
