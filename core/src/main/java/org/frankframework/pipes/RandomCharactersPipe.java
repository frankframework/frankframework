/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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
package org.frankframework.pipes;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.stream.Message;
import org.frankframework.util.UUIDUtil;

import java.util.Random;

import static org.frankframework.pipes.RandomCharactersPipe.Type.ALPHANUMERIC;
import static org.frankframework.pipes.RandomCharactersPipe.Type.ALPHANUMERIC_UPPERCASE;
import static org.frankframework.pipes.RandomCharactersPipe.Type.NUMERIC;

/**
 * Pipe that generates a random character sequence.
 * @author Tom van der Heijden
 */
@Setter
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.TRANSLATOR)
public class RandomCharactersPipe extends FixedForwardPipe {

    /**
     * Format of generated string.
     *
     * @ff.default alphanumeric
     */
    private @Getter Type type = ALPHANUMERIC;
    /**
     * Length of the generated sting. Maximum length is 999.
     *
     * @ff.default 6
     */
    private @Getter int length = 6;

    public enum Type {
        ALPHANUMERIC("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"),
		ALPHANUMERIC_UPPERCASE("ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"),
        NUMERIC("0123456789");

		@Getter private final String characters;

		Type(String characters) {
			this.characters = characters;
		}
	}

    @Override
    public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
        if (length > 999) {
            throw new PipeRunException(this, "Length is greater than 999");
        }
        return new PipeRunResult(getSuccessForward(), getSaltString());
    }

    private String getSaltString() {
        String saltChars = getSaltChars();
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < length) {
            int index = (int) (rnd.nextFloat() * saltChars.length());
            salt.append(saltChars.charAt(index));
        }
        return salt.toString();
    }

    private String getSaltChars() {
		return switch (type) {
			case ALPHANUMERIC -> ALPHANUMERIC.getCharacters();
			case ALPHANUMERIC_UPPERCASE -> ALPHANUMERIC_UPPERCASE.getCharacters();
			case NUMERIC -> NUMERIC.getCharacters();
		};
	}
}
