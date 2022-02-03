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

import microsoft.exchange.webservices.data.core.service.folder.Folder;
import microsoft.exchange.webservices.data.property.complex.FolderId;
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
 * @author Laurens Mäkel.
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
	public void ensureMailboxIsRegistered(String mailbox, FolderId baseFolderId, List<Folder> folders) throws Exception {
		if(!isMailboxRegistered(mailbox)){
			registerMailbox(mailbox, baseFolderId, folders);
		}
	}

	public FolderId getFolder(ExchangeFileSystemResolver resolver){
		return getFolder(resolver.getMailbox(), resolver.getFolderName());
	}

	/**
	 * Retrieves Exchange Folder object from cache.
	 *
	 * @param mailbox - Mailbox to retrieve Folder from
	 * @param folderName - Folder name to look for
	 *
	 * @return Folder - The EWS Folder object that matches parameters.
	 */
	public FolderId getFolder(String mailbox, String folderName) throws IllegalStateException {
		log.debug("Looking for folder ["+folderName+"] in ["+mailbox+"]");

		FolderId result = folders.get(constructKey(mailbox, folderName));
		if(result == null){
			log.warn("Cannot find folder for mailbox ["+mailbox+"] and folder ["+folderName+"].");
		}

		return result;
	}

	public FolderId getBaseFolderId(String mailbox){
		log.debug("Looking for base folder for ["+mailbox+"]");

		FolderId result = baseFolders.get(mailbox);
		if(result == null){
			log.warn("Cannot find base folder folder for mailbox ["+mailbox+"].");
		}

		return result;
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
	private synchronized void registerMailbox(String mailbox, FolderId baseFolderId, List<Folder> folders) throws Exception {
		if(!isMailboxRegistered(mailbox)){
			log.debug("Creating a local cache of folders for ["+mailbox+"].");

			baseFolders.put(mailbox, baseFolderId);

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
		String key = constructKey(mailbox, folderName);
		if(!folders.containsKey(key)){
			log.debug("Creating a local cache of folder ["+folderName+"] for ["+mailbox+"] under key ["+key+"].");

			folders.put(key, folder.getId());
		}
	}

	public void registerFolder(ExchangeFileSystemResolver resolver, FolderId folderId) throws Exception {
		String key = resolver.getMailbox() + resolver.getFolderName();

		if(!folders.containsKey(key)){
			log.debug("Creating a local cache of folder ["+resolver.getFolderName()+"] for ["+ resolver.getMailbox()+"] under key ["+key+"].");

			folders.put(key,folderId);
		}
	}

	public void removeFolder(String mailbox, Folder folder) throws Exception {
		String folderName = folder.getDisplayName();
		String key = constructKey(mailbox, folderName);
		if(folders.containsKey(key)){
			log.debug("Removing folder ["+folderName+"] for ["+mailbox+"] under key ["+key+"] from cache.");

			folders.remove(key);
		}
	}

	private String constructKey(String mailbox, String folderName){
		return mailbox + folderName;
	}

}
