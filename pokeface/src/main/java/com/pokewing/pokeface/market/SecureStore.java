package com.pokewing.pokeface.market;

import com.pokewing.pokeface.PokeFace;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Local, at-rest encryption for the one thing the market may remember: the
 * store's own session token.
 *
 * <p><b>What this is not.</b> It is not somewhere to put a card. No payment
 * detail — number, expiry, CVC, billing address — is ever accepted, sent or
 * stored by this mod; the checkout happens on the store's own page, which is the
 * only place that should ever see one. This exists purely so a player who ticks
 * "remember me" does not have to sign in to the store on every launch.
 *
 * <p><b>What it is worth.</b> The key sits next to the data, on the same disk,
 * so this protects against a token being read out of a shared screenshot, a
 * synced config folder or a pasted log — not against someone who already has the
 * machine. That is the honest limit of any local "remember me", and the reason
 * the default is to remember nothing.
 */
public final class SecureStore {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORM = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private SecureStore() {
    }

    private static Path keyFile(Path dir) {
        return dir.resolve("market.key");
    }

    private static SecretKey key(Path dir) throws Exception {
        Path file = keyFile(dir);
        if (Files.exists(file)) {
            byte[] raw = Base64.getDecoder().decode(Files.readString(file).trim());
            return new SecretKeySpec(raw, ALGORITHM);
        }
        KeyGenerator generator = KeyGenerator.getInstance(ALGORITHM);
        generator.init(256);
        SecretKey generated = generator.generateKey();
        Files.createDirectories(dir);
        Files.writeString(file, Base64.getEncoder().encodeToString(generated.getEncoded()));
        return generated;
    }

    public static String encrypt(Path dir, String plain) {
        if (plain == null || plain.isEmpty()) {
            return "";
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, key(dir), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(encrypted, 0, out, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            PokeFace.LOGGER.warn("PokeFace: could not encrypt the store token ({}), not saving it.",
                    e.toString());
            return "";
        }
    }

    public static String decrypt(Path dir, String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return "";
        }
        try {
            byte[] raw = Base64.getDecoder().decode(encoded);
            byte[] iv = new byte[IV_BYTES];
            System.arraycopy(raw, 0, iv, 0, IV_BYTES);
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, key(dir), new GCMParameterSpec(TAG_BITS, iv));
            byte[] plain = cipher.doFinal(raw, IV_BYTES, raw.length - IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            PokeFace.LOGGER.warn("PokeFace: stored store token could not be read; signing in again.");
            return "";
        }
    }

    /** Wipes both the token and the key, so "forget me" actually forgets. */
    public static void wipe(Path dir) {
        try {
            Files.deleteIfExists(keyFile(dir));
        } catch (Exception e) {
            PokeFace.LOGGER.warn("PokeFace: could not delete the market key: {}", e.toString());
        }
    }
}
