/*
 * Copyright 2016 - Fabio "MrWHO" Torchetti
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.wedjaa.ansible.vault.crypto.data;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import net.wedjaa.ansible.vault.crypto.decoders.CypherFactory;
import net.wedjaa.ansible.vault.crypto.decoders.inter.CypherInterface;

public class VaultInfo
{
	private static final Log logger = LogFactory.getLog(VaultInfo.class);

    public final static String INFO_SEPARATOR = ";";
    public final static int INFO_ELEMENTS = 3;
    public final static int MAGIC_PART = 0;
    public final static int VERSION_PART = 1;
    public final static int CYPHER_PART = 2;

    public final static String VAULT_MAGIC="$ANSIBLE_VAULT";
    public final static String VAULT_VERSION="1.1";

    private boolean validVault;
    private String vaultVersion;
    private String vaultCypher;

    public static String vaultInfoForCypher(String vaultCypher)
    {
        String infoLine = VAULT_MAGIC+";"+VAULT_VERSION+";"+vaultCypher;
        return infoLine;
    }

    public VaultInfo(String infoLine)
    {
    	if( logger.isDebugEnabled() ) {
        	logger.debug("Ansible Vault info: %s".formatted(infoLine));
    	}

        String [] infoParts = infoLine.split(INFO_SEPARATOR);
        if (infoParts.length == INFO_ELEMENTS)
        {
            if ( infoParts[MAGIC_PART].equals(VAULT_MAGIC) ) {
                validVault = true;
                vaultVersion = infoParts[VERSION_PART];
                vaultCypher = infoParts[CYPHER_PART];
            }
        }
    }

    public boolean isEncryptedVault()
    {
        return validVault;
    }

    public CypherInterface getCypher()
    {
        return CypherFactory.getCypher(vaultCypher);
    }

    public String getVaultVersion()
    {
        return vaultVersion;
    }

    public boolean isValidVault()
    {
        return isEncryptedVault() && getCypher() != null;
    }

}
