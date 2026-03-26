/*
  Copyright 2026 WeAreFrank!

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.networknt.schema.ExecutionContext;

class OpenApiLenientDateTimeFormatTest {
    private OpenApiLenientDateTimeFormat format;
    private ExecutionContext ctx;

    @BeforeEach
    void setUp() {
        format = new OpenApiLenientDateTimeFormat();
        ctx = null; // Not used in current implementation
    }

    @Test
    void testStrictRfc3339WithColonNoNanos() {
        assertTrue(format.matches(ctx, "2023-03-26T12:34:56+01:00"));
    }

    @Test
    void testStrictRfc3339WithColonWithNanos() {
        assertTrue(format.matches(ctx, "2023-03-26T12:34:56.123456789+01:00"));
        assertTrue(format.matches(ctx, "2023-03-26T12:34:56.123+01:00"));
    }

    @Test
    void testFallbackNoColonNoNanos() {
        assertTrue(format.matches(ctx, "2023-03-26T12:34:56+0100"));
    }

    @Test
    void testFallbackNoColonWithNanos() {
        assertTrue(format.matches(ctx, "2023-03-26T12:34:56.123456789+0100"));
        assertTrue(format.matches(ctx, "2023-03-26T12:34:56.123+0100"));
    }

    @Test
    void testInvalidCases() {
        assertFalse(format.matches(ctx, "2023-03-26T12:34:56")); // No offset
        assertFalse(format.matches(ctx, "2023-03-26 12:34:56+01:00")); // Space instead of T
        assertFalse(format.matches(ctx, "not-a-date"));
    }

    @Test
    void testNullValue() {
        assertTrue(format.matches(ctx, null));
    }
}

