package nl.nn.adapterframework.secrets;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.filesystem.LocalFileSystem;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class FileSystemCredentialsTest {

	private LocalFileSystem lfs;
	
	@Before
	public void setup() throws IOException {
		lfs = new LocalFileSystem();
		String url = TestFileUtils.getTestFileURL("/secrets").toExternalForm();
		lfs.setRoot(url.substring(url.indexOf(":/")+2));
	}
	
	@Test
	public void testNoAlias() {
		
		String alias = null;
		String username = "fakeUsername";
		String password = "fakePassword";
		
		FileSystemCredentials fsc = new FileSystemCredentials(alias, username, password, null);
		
		assertEquals(username, fsc.getUsername());
		assertEquals(password, fsc.getPassword());
	}

	@Test
	public void testNoFileSystem() {
		
		String alias = "fakeAlias";
		String username = "fakeUsername";
		String password = "fakePassword";
		
		FileSystemCredentials fsc = new FileSystemCredentials(alias, username, password, null);
		
		assertEquals(username, fsc.getUsername());
		assertEquals(password, fsc.getPassword());
	}

	@Test
	public void testPlainAlias() {
		
		String alias = "plain";
		String username = "fakeUsername";
		String password = "fakePassword";
		String expectedUsername = "username from alias";
		String expectedPassword = "password from alias";
		
		FileSystemCredentials fsc = new FileSystemCredentials(alias, username, password, lfs);
		
		assertEquals(expectedUsername, fsc.getUsername());
		assertEquals(expectedPassword, fsc.getPassword());
	}

	@Test
	public void testAliasWithoutUsername() {
		
		String alias = "noUsername";
		String username = "fakeUsername";
		String password = "fakePassword";
		String expectedUsername = username;
		String expectedPassword = "password from alias";
		
		FileSystemCredentials fsc = new FileSystemCredentials(alias, username, password, lfs);
		
		assertEquals(expectedUsername, fsc.getUsername());
		assertEquals(expectedPassword, fsc.getPassword());
	}
}
