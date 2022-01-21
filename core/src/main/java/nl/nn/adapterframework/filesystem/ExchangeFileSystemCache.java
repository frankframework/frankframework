/*
   Copyright 2022 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package nl.nn.adapterframework.filesystem;

import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.service.folder.Folder;
import microsoft.exchange.webservices.data.property.complex.FolderId;
import microsoft.exchange.webservices.data.search.FolderView;
import nl.nn.adapterframework.util.LogUtil;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Acts as a cache for EWS components.
 * Stores Folder objects in memory for quick access.
 *
 * @author M63H114 - Laurens Mäkel.
 */
public class ExchangeFileSystemCache {
	private Logger log = LogUtil.getLogger(this);

	private final Map<String, FolderId> baseFolders = new HashMap<>(); // <Mailbox+FolderNaam, FolderId>
	private final HashMap<String, FolderId> folders = new HashMap<>(); // <Mailbox+FolderNaam, FolderId>
	private final List<String> mailboxesList = new ArrayList<>();

	/**
	 * Ensures that all Folder objects are present in cache.
	 *
	 * @param mailbox - Mailbox to ensure in cache.
	 */
	public void ensureMailboxIsRegistered(String mailbox, FolderId baseFolderId, ExchangeService service) throws Exception {
		if(!isMailboxRegistered(mailbox)){
			registerMailbox(mailbox, baseFolderId, service);
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
	public FolderId getFolder(String mailbox, String folder) throws IllegalStateException {
		log.debug("Looking for folder ["+folder+"] in ["+mailbox+"]");

		FolderId result = folders.get(mailbox+folder);
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
	public boolean isMailboxRegistered(String mailbox){
		return mailboxesList.contains(mailbox);
	}


	/**
	 * Starts the process of caching all Folder objects for specified mailbox.
	 *
	 * @param mailbox - The name of the mailbox to cache.
	 */
	private synchronized void registerMailbox(String mailbox, FolderId baseFolderId, ExchangeService service) throws Exception {
		if(!isMailboxRegistered(mailbox)){
			log.debug("Creating a local cache of folders for ["+mailbox+"].");

			baseFolders.put(mailbox, baseFolderId);

			ArrayList<Folder> folders = findFolders(service, baseFolderId, Integer.MAX_VALUE);
			for (Folder localFolder : folders) {
				registerFolder(mailbox, localFolder);
			}

			mailboxesList.add(mailbox);
		}
	}

	/**
	 * Puts a Folder object in cache for specified mailbox.
	 *
	 * @param mailbox - The name of the mailbox to cache.
	 * @param folder - The Folder object to store in cache.
	 */
	public void registerFolder(String mailbox, Folder folder) throws Exception {
		String folderName = folder.getDisplayName();
		String key = mailbox + folderName;
		if(!folders.containsKey(key)){
			log.debug("Creating a local cache of folder ["+folderName+"] for ["+mailbox+"] under key ["+key+"].");

			folders.put(key, folder.getId());
		}
	}

	public void removeFolder(String mailbox, Folder folder) throws Exception {
		String folderName = folder.getDisplayName();
		String key = mailbox + folderName;
		if(folders.containsKey(key)){
			log.debug("Removing folder ["+folderName+"] for ["+mailbox+"] under key ["+key+"] from cache.");

			folders.remove(key);
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

}
