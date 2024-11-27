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
import java.io.OutputStream;

import net.wedjaa.ansible.vault.crypto.data.VaultInfo;
import net.wedjaa.ansible.vault.crypto.decoders.inter.CypherInterface;

public class CypherAES implements CypherInterface
{

    public final static String CYPHER_ID = "AES";

    public void decrypt(OutputStream decodedStream, byte[] data, String password) throws IOException
    {
        throw new IOException(CYPHER_ID + " is not implemented.");
    }

    public byte[] decrypt(byte[] data, String password) throws IOException
    {
        throw new IOException(CYPHER_ID + " is not implemented.");
    }

    public void encrypt(OutputStream encodedStream, byte[] data, String password) throws IOException
    {
        throw new IOException(CYPHER_ID + " is not implemented.");
    }

    public byte[] encrypt(byte[] data, String password) throws IOException
    {
        throw new IOException(CYPHER_ID + " is not implemented.");
    }

    public String infoLine()
    {
        return VaultInfo.vaultInfoForCypher(CYPHER_ID);
    }
}
