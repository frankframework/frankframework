package org.frankframework.filesystem.exchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.models.BodyType;
import com.microsoft.graph.models.EmailAddress;
import com.microsoft.graph.models.Importance;
import com.microsoft.graph.models.ItemBody;
import com.microsoft.graph.models.MailFolder;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.Recipient;
import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.item.UserItemRequestBuilder;
import com.microsoft.graph.users.item.mailfolders.item.MailFolderItemRequestBuilder;
import com.microsoft.graph.users.item.messages.item.move.MovePostRequestBody;

import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.StringUtil;

public class ExchangeFileSystemTestHelper implements IFileSystemTestHelper {

	private static final String SCOPE = "https://graph.microsoft.com/.default";

	private String clientId;
	private String clientSecret;
	private String authAlias;
	private String tenantId;
	private String mailAddress;
	private String baseFolder;

	private String baseFolderId;
	private MailFolderItemRequestBuilder baseMailFolder;

	private GraphServiceClient msGraphClient;
	private static UserItemRequestBuilder requestBuilder;

	public ExchangeFileSystemTestHelper(String clientId, String clientSecret, String tenantId, String mailAddress, String baseFolder) {
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.tenantId = tenantId;
		this.mailAddress = mailAddress;
		this.baseFolder = baseFolder;
	}

	@BeforeEach
	@Override
	public void setUp() {
		if (requestBuilder == null) {
			CredentialFactory cf = new CredentialFactory(authAlias, clientId, clientSecret);
			TokenCredential credential = new ClientSecretCredentialBuilder()
					.tenantId(tenantId)
					.clientId(cf.getUsername())
					.clientSecret(cf.getPassword())
					.build();

			msGraphClient = new GraphServiceClient(credential, SCOPE);
			User user = msGraphClient.usersWithUserPrincipalName(mailAddress).get(rc -> {
				rc.queryParameters.select = new String[]{"id", "userPrincipalName", "displayName", "givenName", "surname", "accountEnabled"};
			});

			requestBuilder = msGraphClient.users().byUserId(user.getId());

			List<String> baseFolderList = StringUtil.split(baseFolder, "/");

			List<MailFolder> folders = requestBuilder.mailFolders().get().getValue();
			MailFolder base = folders.get(0);

			for (String subMailFolder : baseFolderList) {
				for (MailFolder mailFolder : folders) {
					if (subMailFolder.equalsIgnoreCase(mailFolder.getDisplayName())) {
						folders = requestBuilder.mailFolders().byMailFolderId(mailFolder.getId()).childFolders().get().getValue();
						base = mailFolder;
					}
				}
			}

			baseFolderId = base.getId();
			baseMailFolder = requestBuilder.mailFolders().byMailFolderId(baseFolderId);
		}
	}

	private MailFolder findFolder(String folderNames) {
		List<String> baseFolderList = StringUtil.split(folderNames, "/");

		List<MailFolder> folders = baseMailFolder.childFolders().get().getValue();
		MailFolder base = null;

		for (String subMailFolder : baseFolderList) {
			for (MailFolder mailFolder : folders) {
				if (subMailFolder.equalsIgnoreCase(mailFolder.getDisplayName())) {
					folders = requestBuilder.mailFolders().byMailFolderId(mailFolder.getId()).childFolders().get().getValue();
					base = mailFolder;
				}
			}
		}
		return base;
	}

	@Override
	public void tearDown() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean _fileExists(String folder, String filename) throws Exception {
		String mailFolderId = (folder != null) ? findFolder(folder).getId() : baseFolderId;
		Message msg = requestBuilder.mailFolders().byMailFolderId(mailFolderId).messages().byMessageId(filename).get();
		return msg != null;
	}

	@Override
	public boolean _folderExists(String folderName) throws Exception {
		return findFolder(folderName) != null;
	}

	@Override
	public void _deleteFile(String folder, String filename) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public OutputStream _createFile(String folder, String filename) throws Exception {
		return new ByteArrayOutputStream() {

			@Override
			public void close() throws IOException {
				String content = new String(toByteArray());
				Message message = new Message();
				message.setSubject(filename);
				message.setImportance(Importance.Normal);

				ItemBody body = new ItemBody();
				body.setContentType(BodyType.Text);
				body.setContent(content);
				message.setBody(body);

				List<Recipient> toRecipients = new LinkedList<Recipient>();
				Recipient recipient = new Recipient();
				EmailAddress emailAddress = new EmailAddress();
				emailAddress.setAddress("sergi@frankframework.org");
				recipient.setEmailAddress(emailAddress);
				toRecipients.add(recipient);
				message.setToRecipients(toRecipients);

				Message response = requestBuilder.messages().post(message);

				MovePostRequestBody movePostRequestBody = new MovePostRequestBody();
				movePostRequestBody.setDestinationId(baseFolderId);
				requestBuilder.messages().byMessageId(response.getId()).move().post(movePostRequestBody);
			}
		};
	}

	@Override
	public InputStream _readFile(String folder, String filename) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void _createFolder(String foldername) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void _deleteFolder(String folderName) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
