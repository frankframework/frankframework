package nl.nn.adapterframework.jta;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.util.StreamUtils;

import nl.nn.adapterframework.util.LogUtil;

public abstract class StatusRecordingTransactionManagerTestBase<S extends StatusRecordingTransactionManager> {
	protected Logger log = LogUtil.getLogger(this);

	public String STATUS_FILE = "status.txt";
	public String TMUID_FILE = "tm-uid.txt";

	public String folder;
	public String statusFile;
	public String tmUidFile;

	protected S transactionManager;

	protected abstract S createTransactionManager();

	@Before
	public void setup() throws IOException {
		statusFile = folder+"/"+STATUS_FILE;
		tmUidFile = folder+"/"+TMUID_FILE;
	}

	@After
	public void tearDown() {
		if (transactionManager != null) {
			transactionManager.shutdownTransactionManager();
			transactionManager = null;
		}
	}


	protected S setupTransactionManager() {
		log.debug("setupTransactionManager folder ["+folder+"]");
		S result = createTransactionManager();
		result.setStatusFile(statusFile);
		result.setUidFile(tmUidFile);
		transactionManager = result;
		return result;
	}


	public void assertStatus(String status, String tmUid) {
		assertEquals(status, read(statusFile));
		if (tmUid!=null) {
			assertEquals(tmUid, read(tmUidFile));
		}
	}

	public void delete(String filename) throws TransactionSystemException {
		Path file = Paths.get(filename);
		try {
			if (Files.exists(file)) {
				Files.delete(file);
			}
		} catch (Exception e) {
			throw new TransactionSystemException("Cannot delete file ["+file+"]", e);
		}
	}

	public void write(String filename, String text) throws TransactionSystemException {
		Path file = Paths.get(filename);
		try {
			try (OutputStream fos = Files.newOutputStream(file)) {
				fos.write(text.getBytes(StandardCharsets.UTF_8));
			}
		} catch (Exception e) {
			throw new TransactionSystemException("Cannot write line ["+text+"] to file ["+file+"]", e);
		}
	}

	public String read(String filename) {
		Path file = Paths.get(filename);
		if (!Files.exists(file)) {
			return null;
		}
		try (InputStream fis = Files.newInputStream(file)) {
			return StreamUtils.copyToString(fis, StandardCharsets.UTF_8).trim();
		} catch (Exception e) {
			throw new TransactionSystemException("Cannot read from file ["+file+"]", e);
		}
	}

}
