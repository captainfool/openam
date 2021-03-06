/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package com.iplanet.dpro.session;

import static org.assertj.core.api.Assertions.*;
import org.forgerock.util.encode.Base64;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.MalformedInputException;

public class SessionIDIntegrationTest {

    @Test
    public void shouldParseExtensionsCorrectly() throws Exception {
        // Given
        final SessionIDExtensions extensions = new LegacySessionIDExtensions();
        for (int i = 0; i < 1000; ++i) {
            extensions.add("Key" + i, "Value" + i);
        }

        final String encoded = SessionID.makeSessionID("", extensions, null);

        // When
        final LegacySessionIDExtensions result = new LegacySessionIDExtensions(encoded);

        // Then
        assertThat(result.asMap()).isEqualTo(extensions.asMap());
    }

    @Test
    public void shouldParseWholeSessionIDCorrectly() throws Exception {
        // Given
        final SessionIDExtensions extensions = new LegacySessionIDExtensions();
        extensions.add("one", "one");
        extensions.add("two", "two");
        final String encryptedId = "someEncryptedId";
        final String tail = "I'm a donkey";

        // When
        final SessionID sid = new SessionID(SessionID.makeSessionID(encryptedId, extensions, tail));

        // Then
        assertThat(sid.getTail()).isEqualTo(tail);
        assertThat(sid.getExtension().get("one")).isEqualTo("one");
        assertThat(sid.getExtension().get("two")).isEqualTo("two");
    }

    @Test(expectedExceptions = MalformedInputException.class)
    public void shouldRejectNullBytesInExtensionString() throws Exception {
        // Given
        final SessionIDExtensions extensions = new LegacySessionIDExtensions();
        extensions.add("Key\u0000", "Value\u0000");

        final String encoded = SessionID.makeSessionID("", extensions, null);

        // When
        new LegacySessionIDExtensions(encoded);

        // Then - exception
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectGarbageInput() throws Exception {
        // Given
        String encoded = "This isn't valid Base64";

        // When
        new LegacySessionIDExtensions(encoded);

        // Then - exception
    }

    @Test(expectedExceptions = MalformedInputException.class)
    public void shouldRejectInvalidUTF8Data() throws Exception {
        // Given
        // e-acute (U+00e9) is 0xe9 in iso-8859-1 but 0xc3,0xa9 in UTF-8. 0xe9 on it's own is invalid utf-8.
        byte[] isoBytes = "Caf\u00e9".getBytes("ISO-8859-1");
        // Mimick writeUTF but using ISO-8859-1 bytes instead of modified UTF-8
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(baos);
        out.writeShort(isoBytes.length);
        out.write(isoBytes);
        String encoded = Base64.encode(baos.toByteArray());

        // When
        new LegacySessionIDExtensions(encoded);

        // Then - exception
    }
}