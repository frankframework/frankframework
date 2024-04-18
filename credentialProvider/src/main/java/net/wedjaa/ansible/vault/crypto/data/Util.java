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

import java.util.Arrays;
import java.util.stream.Collectors;


public class Util
{

    private static final int DEFAULT_LINE_LENGTH = 80;

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    final protected static String LINE_BREAK = "\n";
    final protected static String CHAR_ENCODING = "UTF-8";

    public static String join(String [] datalines)
    {
    	//LF + CR (Windows & Linux files supported
    	return Arrays.asList(datalines).stream().map(n -> n.trim()).collect(Collectors.joining());
    }

    public static byte[] unhex(String hexed)
    {
        int dataLen = hexed.length();
        byte[] output = new byte[dataLen/2];
        for (int charIdx = 0; charIdx < dataLen; charIdx+=2) {
            output[charIdx/2] = (byte) ((Character.digit(hexed.charAt(charIdx), 16) << 4)
                    + Character.digit(hexed.charAt(charIdx+1), 16));
        }
        return output;
    }

    public static String hexit(byte [] unhexed)
    {
        return hexit(unhexed, DEFAULT_LINE_LENGTH);
    }

    public static String hexit(byte [] unhexed, int lineLength)
    {
        String result = "";
        int colIdx = 0;
        for (byte val: unhexed)
        {
            result += "%02x".formatted(val);
            colIdx++;
            if (lineLength > 0 && colIdx>=lineLength/2) {
                result += LINE_BREAK;
                colIdx=0;
            }
        }

        return result;
    }

    public static VaultInfo getVaultInfo(String vaultData)
    {
    	//LF + CR (Windows & Linux files supported
    	String infoString =  vaultData.substring(0, vaultData.indexOf(LINE_BREAK)).trim();
        return new VaultInfo(infoString);
    }

    public static VaultInfo getVaultInfo(byte [] vaultData)
    {
        return getVaultInfo(new String(vaultData));
    }

    public static String removeVaultInfo(String vaultData)
    {
        return vaultData.substring(vaultData.indexOf(LINE_BREAK) + 1);
    }

    public static byte[] getVaultData(String vaultData)
    {
        String rawData = join(removeVaultInfo(vaultData).split(LINE_BREAK));
        return unhex(rawData);
    }

    public static byte[] getVaultData(byte [] vaultData)
    {
        String rawData = join(removeVaultInfo(new String(vaultData)).split(LINE_BREAK));
        return unhex(rawData);
    }

}
