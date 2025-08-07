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

package net.wedjaa.ansible.vault.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.wedjaa.ansible.vault.crypto.data.Util;
import net.wedjaa.ansible.vault.crypto.data.VaultInfo;
import net.wedjaa.ansible.vault.crypto.decoders.CypherFactory;
import net.wedjaa.ansible.vault.crypto.decoders.implementation.CypherAES256;
import net.wedjaa.ansible.vault.crypto.decoders.inter.CypherInterface;

/**
 * Created by mrwho on 03/06/15.
 */
public class VaultHandler
{

    public final static String DEFAULT_CYPHER = CypherAES256.CYPHER_ID;

    public final static String CHAR_ENCODING = "UTF-8";


    public static byte [] encrypt(byte [] cleartext, String password, String cypher) throws IOException
    {
        CypherInterface cypherInstance = CypherFactory.getCypher(cypher);
        byte [] vaultData = cypherInstance.encrypt(cleartext, password);
        String vaultDataString = new String(vaultData);
        String vaultPackage = cypherInstance.infoLine() + "\n" + vaultDataString;
        return vaultPackage.getBytes();
    }

    public static byte [] encrypt(byte [] cleartext, String password) throws IOException
    {
        return encrypt(cleartext, password, DEFAULT_CYPHER);
    }

    public static void encrypt(InputStream clearText, OutputStream cipherText, String password, String cypher) throws IOException
    {

        cipherText.write(encrypt(toByteArray(clearText), password, cypher));
    }

    public static void encrypt(InputStream clearText, OutputStream cipherText, String password) throws IOException
    {
        encrypt(clearText, cipherText, password, DEFAULT_CYPHER);
    }

    public static void decrypt(InputStream encryptedVault, OutputStream decryptedVault, String password) throws IOException
    {
        decryptedVault.write(decrypt(toByteArray(encryptedVault), password));
    }

    public static byte[] decrypt(byte[] encrypted, String password) throws IOException
    {

        VaultInfo vaultInfo = Util.getVaultInfo(encrypted);
        if ( !vaultInfo.isEncryptedVault() ) {
            throw new IOException("File is not an Ansible Encrypted Vault");
        }

        if ( !vaultInfo.isValidVault() )
        {
            throw new IOException("The vault is not a format we can handle - check the cypher.");
        }

        byte [] encryptedData = Util.getVaultData(encrypted);

        return vaultInfo.getCypher().decrypt(encryptedData, password);
    }

    /**
     * Gets the contents of an <code>InputStream</code> as a <code>byte[]</code>.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     *
     * @param input the <code>InputStream</code> to read from
     * @return the requested byte array
     * @throws NullPointerException if the input is null
     * @throws IOException          if an I/O error occurs
     */
    public static byte[] toByteArray(final InputStream input) throws IOException {
        try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            int n;
            final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            while (EOF != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
            }

        	return output.toByteArray();
        }
    }

    /**
     * Represents the end-of-file (or stream).
     */
    public static final int EOF = -1;

    /**
     * The default buffer size ({@value}) to use in copy methods.
     */
    public static final int DEFAULT_BUFFER_SIZE = 8192;
}
