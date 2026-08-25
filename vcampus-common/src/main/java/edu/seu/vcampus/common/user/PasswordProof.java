package edu.seu.vcampus.common.user;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Creates a non-plaintext proof for the temporary development login protocol.
 */
public final class PasswordProof {

    private static final byte[] DOMAIN = "vcampus-dev-login-v1\n".getBytes(StandardCharsets.UTF_8);

    private PasswordProof() {
    }

    /**
     * Derives a deterministic proof from a username and password.
     *
     * <p>This prevents raw password text from entering the request object, but it is
     * not a replacement for TLS and a slow password hash in the final design.</p>
     *
     * @param username login name
     * @param password caller-owned password characters
     * @return lowercase SHA-256 proof
     */
    public static String create(String username, char[] password) {
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(password, "password must not be null");
        if (password.length == 0) {
            throw new IllegalArgumentException("password must not be empty");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(DOMAIN);
            digest.update(username.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            digest.update((byte) ':');
            ByteBuffer encodedPassword = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password));
            digest.update(encodedPassword.duplicate());
            if (encodedPassword.hasArray()) {
                Arrays.fill(encodedPassword.array(), (byte) 0);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
