package nl.nn.adapterframework.filesystem;

import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.service.folder.Folder;
import microsoft.exchange.webservices.data.core.service.schema.FolderSchema;
import microsoft.exchange.webservices.data.property.complex.FolderId;
import microsoft.exchange.webservices.data.property.complex.Mailbox;
import microsoft.exchange.webservices.data.search.FindFoldersResults;
import microsoft.exchange.webservices.data.search.FolderView;
import microsoft.exchange.webservices.data.search.filter.SearchFilter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.LogUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Acts as a cache for EWS components.
 * Stores Folder objects in memory for quick access.
 *
 * @author M63H114 - Laurens Mäkel.
 */
public class ExchangeFileSystemCache {
	private Logger log = LogUtil.getLogger(this);

	private final HashMap<String, FolderId> baseFolders = new HashMap<>(); // <Mailbox+FolderNaam, FolderId>
	private final HashMap<String, Folder> folders = new HashMap<>(); // <Mailbox+FolderNaam, Folder>
	private final ArrayList<String> mailboxesList = new ArrayList<>();

	/**
	 * Ensures that all Folder objects are present in cache.
	 *
	 * @param mailbox - Mailbox to ensure in cache.
	 */
	public void ensureMailboxIsRegistered(String mailbox, String baseFolderName, ExchangeService service) throws Exception {
		if(!isMailboxRegistered(mailbox)){
			registerMailbox(mailbox, baseFolderName, service);
		}
	}

	/**
	 * Retrieves Exchange Folder object from cache.
	 *
	 * @param mailbox - Mailbox to retrieve Folder from
	 * @param folder - Folder name to look for
	 *
	 * @return Folder - The EWS Folder object that matches parameters.
	 */
	public Folder getFolder(String mailbox, String folder) throws IllegalStateException {
		log.debug("Looking for folder ["+folder+"] in ["+mailbox+"]");

		Folder result = folders.get(mailbox+folder);
		if(result == null){
			log.warn("Cannot find folder for mailbox ["+mailbox+"] and folder ["+folder+"].");
			// throw new IllegalStateException("Cannot find folder for mailbox ["+mailbox+"] and folder ["+folder+"].");
		}

		return result;
	}

	public FolderId getBaseFolderId(String mailbox){
		return baseFolders.get(mailbox);
	}

	/**
	 * Checks if specified mailbox is already cached.
	 *
	 * @param mailbox - The name of the mailbox to check.
	 *
	 * @return boolean - Confirmation or denial.
	 */
	private boolean isMailboxRegistered(String mailbox){
		return mailboxesList.contains(mailbox);
	}


	/**
	 * Starts the process of caching all Folder objects for specified mailbox.
	 *
	 * @param mailbox - The name of the mailbox to cache.
	 */
	private synchronized void registerMailbox(String mailbox, String baseFolderName, ExchangeService service) throws Exception {
		if(!isMailboxRegistered(mailbox)){
			log.debug("Creating a local cache of folders for ["+mailbox+"].");

			FolderId baseFolderId = getBaseFolderId(mailbox, baseFolderName, service);
			baseFolders.put(mailbox, baseFolderId);

			ArrayList<Folder> folders = findFolders(service, baseFolderId, Integer.MAX_VALUE);
			for (Folder localFolder : folders) {
				registerFolder(mailbox, localFolder);
			}

			mailboxesList.add(mailbox);
		}
	}

	private FolderId getBaseFolderId(String emailAddress, String baseFolderName, ExchangeService service) throws FileSystemException {
		FolderId basefolderId;

		log.debug("searching inbox");
		FolderId inboxId;
		if (StringUtils.isNotEmpty(emailAddress)) {
			Mailbox mailbox = new Mailbox(emailAddress);
			inboxId = new FolderId(WellKnownFolderName.Inbox, mailbox);
		} else {
			inboxId = new FolderId(WellKnownFolderName.Inbox);
		}
		log.debug("determined inbox ["+inboxId+"] foldername ["+inboxId.getFolderName()+"]");

		if (StringUtils.isNotEmpty(baseFolderName)) {
			try {
				basefolderId=findFolder(service,inboxId,baseFolderName);
			} catch (Exception e) {
				throw new FileSystemException("Could not find baseFolder ["+baseFolderName+"] as subfolder of ["+inboxId.getFolderName()+"]", e);
			}
			if (basefolderId==null) {
				log.debug("Could not get baseFolder ["+baseFolderName+"] as subfolder of ["+inboxId.getFolderName()+"]");
				basefolderId=findFolder(service,null,baseFolderName);
			}
			if (basefolderId==null) {
				throw new FileSystemException("Could not find baseFolder ["+baseFolderName+"]");
			}
		} else {
			basefolderId=inboxId;
		}

		return basefolderId;
	}

	/**
	 * Puts a Folder object in cache for specified mailbox.
	 *
	 * @param mailbox - The name of the mailbox to cache.
	 * @param folder - The Folder object to store in cache.
	 */
	private void registerFolder(String mailbox, Folder folder) throws Exception {
		String folderName = folder.getDisplayName();
		String key = mailbox + folderName;
		if(!folders.containsKey(key)){
			log.debug("Creating a local cache of folder ["+folderName+"] for ["+mailbox+"] under key ["+key+"].");

			folders.put(key, folder);
		}
	}

	/**
	 * Standard method to retrieve a list of Folder objects within a parent Folder.
	 *
	 * @param service - An instance of Exchange Service to use to make the call
	 * @param parentFolderId - The ID of the parent folder to check for sub Folders.
	 *
	 * @return ArrayList<Folder> - The list of found Folder objects within parent folder.
	 */
	private ArrayList<Folder> findFolders(ExchangeService service, FolderId parentFolderId, int folderViewCount) throws Exception {
		return service.findFolders(parentFolderId, new FolderView(folderViewCount)).getFolders();
	}

	private FolderId findFolder(ExchangeService service, FolderId baseFolderId, String folderName) throws FileSystemException {
		try {
			FindFoldersResults findFoldersResultsIn;
			FolderId result;
			FolderView folderViewIn = new FolderView(10);
			if (StringUtils.isNotEmpty(folderName)) {
				log.debug("searching folder ["+folderName+"]");
				SearchFilter searchFilterIn = new SearchFilter.IsEqualTo(FolderSchema.DisplayName, folderName);
				if (baseFolderId==null) {
					findFoldersResultsIn = service.findFolders(WellKnownFolderName.MsgFolderRoot, searchFilterIn, folderViewIn);
				} else {
					findFoldersResultsIn = service.findFolders(baseFolderId, searchFilterIn, folderViewIn);
				}
				if (findFoldersResultsIn.getTotalCount() == 0) {
					if(log.isDebugEnabled()) log.debug("no folder found with name [" + folderName + "] in basefolder ["+baseFolderId+"]");
					return null;
				}
				if (findFoldersResultsIn.getTotalCount() > 1) {
					if (log.isDebugEnabled()) {
						for (Folder folder:findFoldersResultsIn.getFolders()) {
							log.debug("found folder ["+folder.getDisplayName()+"]");
						}
					}
					throw new ConfigurationException("multiple folders found with name ["+ folderName + "]");
				}
			} else {
				//findFoldersResultsIn = getExchangeService().findFolders(baseFolderId, folderViewIn);
				return baseFolderId;
			}
			if (findFoldersResultsIn.getFolders().isEmpty()) {
				result=baseFolderId;
			} else {
				result=findFoldersResultsIn.getFolders().get(0).getId();
			}
			return result;
		} catch (Exception e) {
//			invalidateConnection(exchangeService);
			throw new FileSystemException("Cannot find folder ["+folderName+"]", e);
		} finally {
//			releaseConnection(exchangeService);
		}
	}
}
