package alsclinic.server;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public final class AdminPassword {
    private static final int ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private AdminPassword() {
    }

    public static void main(String[] args) {
        Console console = System.console();
        boolean verifyOnly = args.length == 2 && "--verify".equals(args[0]);

        if (!verifyOnly && (args.length != 1 || "--verify".equals(args[0]))) {
            System.err.println("Usage: AdminPassword [--verify] <verifier-file>");
            return;
        }

        if (console == null) {
            System.err.println("Run AdminPassword from a terminal.");
            return;
        }

        char[] password = console.readPassword(
                verifyOnly ? "Administrator password: " : "New administrator password: ");
        if (password == null) {
            return;
        }

        try {
            if (verifyOnly) {
                boolean matches = verify(password, Path.of(args[1]));
                System.out.println(matches ? "Password is correct." : "Password is incorrect.");
                return;
            }

            if (password.length == 0) {
                System.err.println("Enter a nonempty password.");
                return;
            }

            Path file = Path.of(args[0]);
            byte[] salt = new byte[SALT_BYTES];
            new SecureRandom().nextBytes(salt);

            Base64.Encoder encoder = Base64.getEncoder();
            Files.write(file, List.of(
                            encoder.encodeToString(salt),
                            encoder.encodeToString(derive(password, salt))),
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

            System.out.println("Administrator verifier created.");
        } catch (IOException | GeneralSecurityException e) {
            System.err.println("Could not " + (verifyOnly ? "verify password" : "create verifier")
                    + ": " + e.getMessage());
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    public static boolean verify(char[] password, Path file)
            throws IOException, GeneralSecurityException {
        List<String> lines = Files.readAllLines(file);
        if (lines.size() != 2) {
            throw new IOException("Invalid administrator verifier file.");
        }

        Base64.Decoder decoder = Base64.getDecoder();
        byte[] salt;
        byte[] expected;
        try {
            salt = decoder.decode(lines.get(0));
            expected = decoder.decode(lines.get(1));
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid administrator verifier file.", e);
        }

        if (salt.length != SALT_BYTES || expected.length != KEY_BITS / Byte.SIZE) {
            throw new IOException("Invalid administrator verifier file.");
        }

        return MessageDigest.isEqual(expected, derive(password, salt));
    }

    private static byte[] derive(char[] password, byte[] salt)
            throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }
}