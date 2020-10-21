package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import com.microsoft.graph.logger.DefaultLogger;
import com.microsoft.graph.logger.LoggerLevel;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.MailFolder;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import com.microsoft.graph.requests.extensions.IMailFolderCollectionPage;
import com.microsoft.graph.requests.extensions.MessageCollectionPage;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.graph.App;
import nl.nn.adapterframework.graph.Authentication;
import nl.nn.adapterframework.graph.SimpleAuthProvider;

public class MsGraphFileSystem implements IBasicFileSystem<GraphItem> {

	private Properties oAuthProperties;
	private static boolean useConfiguredAccessToken = false;
	private static String ACCESS_TOKEN = null;
	protected IGraphServiceClient graphClient;

	@Override
	public String getPhysicalDestinationName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void configure() throws ConfigurationException {
		oAuthProperties = new Properties();
		try {
			oAuthProperties.load(App.class.getResourceAsStream("/oAuth.properties"));
		} catch (IOException e) {
			System.out.println(
					"Unable to read OAuth configuration. Make sure you have a properly formatted oAuth.properties file. See README for details.");
			return;
		}
	}

	@Override
	public void open() throws FileSystemException {
		final String appId = oAuthProperties.getProperty("app.id");
		final String[] appScopes = oAuthProperties.getProperty("app.scopes").split(",");

		// Get an access token
		Authentication.initialize(appId);
		String accessToken = useConfiguredAccessToken && StringUtils.isNotEmpty(ACCESS_TOKEN) ? ACCESS_TOKEN
				: Authentication.getUserAccessToken(appScopes);
		
		if (graphClient == null) {
            // Create the auth provider
            SimpleAuthProvider authProvider = new SimpleAuthProvider(accessToken);

            // Create default logger to only log errors
            DefaultLogger logger = new DefaultLogger();
            logger.setLoggingLevel(LoggerLevel.ERROR);

            // Build a Graph client
            graphClient = GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .logger(logger)
                .buildClient();
        }
	}

	@Override
	public void close() throws FileSystemException {
		try {
			graphClient.shutdown();
		} catch (Exception e) {
			throw new FileSystemException("Could not shutdown graphClient <Fixme:Some id?>", e);
		}
	}

	@Override
	public Iterator<GraphItem> listFiles(String folder) throws FileSystemException {
		IMailFolderCollectionPage page = getFolderByDisplayName(folder);
		if (page == null || page.getCurrentPage() == null || page.getCurrentPage().isEmpty()) {
			//Or throw exception?
			return Collections.emptyIterator();
		}
		MailFolder mailFolder = page.getCurrentPage().get(0);
		MessageCollectionPage messagePage = mailFolder.messages;
//		messagePage.getCurrentPage();
		return null;
	}

	@Override
	public String getName(GraphItem f) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GraphItem toFile(String filename) throws FileSystemException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GraphItem toFile(String defaultFolder, String filename) throws FileSystemException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean exists(GraphItem f) throws FileSystemException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean folderExists(String folder) throws FileSystemException {
		IMailFolderCollectionPage pages = getFolderByDisplayName(folder);
		return !pages.getCurrentPage().isEmpty();
	}

	private IMailFolderCollectionPage getFolderByDisplayName(String folderDisplayName) {
		final String filter = MessageFormat.format("displayName eq ''{0}''", folderDisplayName);
		IMailFolderCollectionPage pages = graphClient
				.me()
				.mailFolders()
				.buildRequest()
				.filter(filter)
				.get();
		return pages;
	}

	@Override
	public InputStream readFile(GraphItem f) throws FileSystemException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteFile(GraphItem f) throws FileSystemException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public GraphItem moveFile(GraphItem f, String destinationFolder, boolean createFolder) throws FileSystemException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GraphItem copyFile(GraphItem f, String destinationFolder, boolean createFolder) throws FileSystemException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void createFolder(String folder) throws FileSystemException {
		MailFolder mailFolder = new MailFolder();
		mailFolder.displayName = folder;
		graphClient.me().mailFolders().buildRequest().post(mailFolder);
	}

	@Override
	public void removeFolder(String folder) throws FileSystemException {
		IMailFolderCollectionPage folderByDisplayName = getFolderByDisplayName(folder);
		List<MailFolder> folders = folderByDisplayName.getCurrentPage();
		if (folders.isEmpty()) {
			//throw exception or log?
			throw new FileSystemException("Cannot remove " + folder + " because it doesn't exist");
		}
		MailFolder mailFolder = folders.get(0);
		graphClient.me().mailFolders(mailFolder.id).buildRequest().delete();
	}

	@Override
	public long getFileSize(GraphItem f) throws FileSystemException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getCanonicalName(GraphItem f) throws FileSystemException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getModificationTime(GraphItem f) throws FileSystemException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getAdditionalFileProperties(GraphItem f) throws FileSystemException {
		// TODO Auto-generated method stub
		return null;
	}
	
}
