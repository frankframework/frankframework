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

package net.wedjaa.ansible.vault.crypto.decoders.implementation;

import java.io.IOException;
import java.util.Arrays;

import de.rtner.security.auth.spi.PBKDF2Engine;
import de.rtner.security.auth.spi.PBKDF2Parameters;

import org.frankframework.util.UUIDUtil;


public class EncryptionKeychain
{
    private static final String CHAR_ENCODING = "UTF-8";

    private final String password;
    private final byte[] salt;
    private final int keylen;
    private final int ivlen;
    private final int iterations;
    private final String algo;

    private byte [] encryptionKey;
    private byte [] hmacKey;
    private byte [] iv;


    public EncryptionKeychain(byte [] salt, String password, int keylen, int ivlen, int iterations, String algo)
    {
        this.password = password;
        this.salt = salt;
        this.keylen = keylen;
        this.ivlen = ivlen;
        this.iterations=iterations;
        this.algo = algo;
    }

    public EncryptionKeychain(int saltLen, String password, int keylen, int ivlen, int iterations, String algo)
    {
        this.password = password;
        this.salt = generateSalt(saltLen);
        this.keylen = keylen;
        this.ivlen = ivlen;
        this.iterations=iterations;
        this.algo = algo;
    }


    private byte[] createRawKey() throws IOException
    {
        try {
            PBKDF2Parameters params = new PBKDF2Parameters(algo, CHAR_ENCODING, salt, iterations);
            int keylength = ivlen + 2 * keylen;
            PBKDF2Engine pbkdf2Engine = new PBKDF2Engine(params);
            byte [] keys = pbkdf2Engine.deriveKey(password, keylength);
            return keys;
        } catch (Exception ex) {
            throw new IOException("Cryptofailure: " + ex.getMessage());
        }

    }

    public void createKeys() throws IOException
    {
        byte [] rawkeys = createRawKey();
        this.encryptionKey = getEncryptionKey(rawkeys);
        this.hmacKey = getHMACKey(rawkeys);
        this.iv = getIV(rawkeys);
    }

    private byte[] getEncryptionKey(byte[] keys)
    {
        byte [] result = Arrays.copyOfRange(keys, 0, keylen);
        return result;
    }

    private byte[] getHMACKey(byte[] keys)
    {
        byte [] result = Arrays.copyOfRange(keys, keylen, keylen*2);
        return result;
    }

    private byte[] getIV(byte[] keys)
    {
        byte [] result = Arrays.copyOfRange(keys, keylen * 2, keylen * 2 + ivlen);
        return result;
    }

    private byte[] generateSalt(int length)
    {
        byte[] salt = new byte[length];
        UUIDUtil.RANDOM.nextBytes(salt);
        return salt;
    }


    public byte[] getSalt()
    {
        return salt;
    }

    public byte[] getEncryptionKey()
    {
        return encryptionKey;
    }


    public byte[] getHmacKey()
    {
        return hmacKey;
    }

    public byte[] getIv()
    {
        return iv;
    }


}
