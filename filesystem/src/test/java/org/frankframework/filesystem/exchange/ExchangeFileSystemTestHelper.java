package org.frankframework.filesystem.exchange;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
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
import com.microsoft.graph.models.odataerrors.ODataError;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.item.UserItemRequestBuilder;
import com.microsoft.graph.users.item.mailfolders.item.MailFolderItemRequestBuilder;
import com.microsoft.graph.users.item.mailfolders.item.childfolders.ChildFoldersRequestBuilder;
import com.microsoft.graph.users.item.mailfolders.item.messages.item.MessageItemRequestBuilder;
import com.microsoft.graph.users.item.messages.item.move.MovePostRequestBody;

import lombok.extern.log4j.Log4j2;

import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.StringUtil;

@Log4j2
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
	private String userId;

	public ExchangeFileSystemTestHelper(String clientId, String clientSecret, String tenantId, String mailAddress, String baseFolder) {
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.tenantId = tenantId;
		this.mailAddress = mailAddress;
		this.baseFolder = baseFolder;
	}

	@BeforeEach
	@Override
	public void setUp() throws Exception {
		if (userId == null) {
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

			userId = user.getId();

			List<String> baseFolderList = StringUtil.split(baseFolder, "/");

			UserItemRequestBuilder requestBuilder = getRequestBuilder();
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

			cleanMailFolder();
		}
	}

	private void cleanMailFolder() throws Exception {
		List<MailFolder> folders = baseMailFolder.childFolders().get().getValue();
		for (MailFolder mailFolder : folders) {
			deleteFolderById(mailFolder.getId());
		}

		// remove all messages in the base folder
		baseMailFolder.messages().get().getValue().stream().forEach(msg -> {
			try {
				_deleteFile(null, msg.getId());
			} catch (Exception e) {
				log.error("unable to remove file", e);
			}
		});
	}

	private UserItemRequestBuilder getRequestBuilder() {
		return msGraphClient.users().byUserId(userId);
	}

	private MailFolder findFolder(String folderNames) {
		List<String> baseFolderList = StringUtil.split(folderNames, "/");

		List<MailFolder> folders = baseMailFolder.childFolders().get().getValue();
		MailFolder base = null;

		for (String subMailFolder : baseFolderList) {
			for (MailFolder mailFolder : folders) {
				if (subMailFolder.equalsIgnoreCase(mailFolder.getDisplayName())) {
					folders = getRequestBuilder().mailFolders().byMailFolderId(mailFolder.getId()).childFolders().get().getValue();
					base = mailFolder;
				}
			}
		}
		return base;
	}

	@Override
	public void tearDown() throws Exception {
		// Nothing to do here
	}

	@Override
	public boolean _fileExists(String folder, String filename) throws Exception {
		String mailFolderId = baseFolderId;
		if (folder != null) {
			MailFolder fFolder = findFolder(folder);
			if (fFolder == null) {
				return false;
			}
			mailFolderId = fFolder.getId();
		}
		MessageItemRequestBuilder mirb = getRequestBuilder().mailFolders().byMailFolderId(mailFolderId).messages().byMessageId(filename);

		try {
			mirb.get();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean _folderExists(String folderName) throws Exception {
		return findFolder(folderName) != null;
	}

	@Override
	public void _deleteFile(String folder, String messageId) throws Exception {
		String mailFolderId = (folder != null) ? findFolder(folder).getId() : baseFolderId;
		MessageItemRequestBuilder mirb = getRequestBuilder().mailFolders().byMailFolderId(mailFolderId).messages().byMessageId(messageId);

		mirb.delete();
	}

	@Override
	public String createFile(String folder, String filename, String content) throws Exception {
		String mailFolderId = (folder != null) ? findFolder(folder).getId() : baseFolderId;

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

		Message response = getRequestBuilder().messages().post(message);

		MovePostRequestBody movePostRequestBody = new MovePostRequestBody();
		movePostRequestBody.setDestinationId(mailFolderId);
		Message movedMessage = getRequestBuilder().messages().byMessageId(response.getId()).move().post(movePostRequestBody);
		return movedMessage.getId();
	}

	@Override
	public InputStream _readFile(String folder, String mailId) throws Exception {
		String mailFolderId = (folder != null) ? findFolder(folder).getId() : baseFolderId;

		Message mailMessage = getRequestBuilder().mailFolders().byMailFolderId(mailFolderId).messages().byMessageId(mailId).get();
		return new ByteArrayInputStream(mailMessage.getBodyPreview().getBytes());
	}

	@Override
	public void _createFolder(String foldername) throws Exception {
		List<String> folders = StringUtil.split(foldername, "/");
		ChildFoldersRequestBuilder crb = baseMailFolder.childFolders();

		for (String folder : folders) {
			if (StringUtils.isEmpty(folder)) return;

			MailFolder mailFolder = new MailFolder();
			mailFolder.setDisplayName(folder);
			mailFolder.setIsHidden(false);

			try {
				MailFolder newMailFolder = crb.post(mailFolder);
				crb = getRequestBuilder().mailFolders().byMailFolderId(newMailFolder.getId()).childFolders();
			} catch (ODataError e) {
				if (e.getMessage().contains("A folder with the specified name already exists.")) {
					continue;
				}
			}
		}
	}

	@Override
	public void _deleteFolder(String folderName) throws Exception {
		if (StringUtils.isNotBlank(folderName)) {
			MailFolder folder = findFolder(folderName);
			if(folder != null) {
				deleteFolderById(folder.getId());
			}
		}
	}

	private void deleteFolderById(String mailFolderId) throws Exception {
		MailFolderItemRequestBuilder mirb = getRequestBuilder().mailFolders().byMailFolderId(mailFolderId);
		mirb.delete();
	}
}
