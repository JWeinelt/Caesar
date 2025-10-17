package de.julianweinelt.caesar.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.julianweinelt.caesar.Caesar;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

@Slf4j
public class APIKeySaver {
    private final File folder = new File("data/connections");
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final String encryptionKey;

    public APIKeySaver() {
        folder.mkdirs();
        this.encryptionKey = LocalStorage.getInstance().getData().getConnectionAPISecret();
    }

    public static APIKeySaver getInstance() {
        return Caesar.getInstance().getApiKeySaver();
    }

    public byte[] loadKey(String cKey) {
        try {
            byte[] encryptedData = loadEncryptedData(new File(folder, cKey + ".cae").getPath() );
            byte[] key = truncate(encryptionKey.getBytes());
            byte[] iv = truncate(encryptionKey.getBytes());
            String decryptedData = decrypt(encryptedData, key, iv);
            ConnectionKey keyPair = GSON.fromJson(decryptedData, new TypeToken<ConnectionKey>(){}.getType());
            return keyPair.key();
        } catch (Exception e) {
            log.error("Could not parse file. Maybe it's not encrypted?");
            log.error(e.getMessage());
        }
        return new byte[0];
    }

    public void saveKey(String cKey, byte[] key) {
        try {
            log.info("Size: {}", truncate(encryptionKey.getBytes()).length);
            byte[] encryptedData = encrypt(GSON.toJson(new ConnectionKey(cKey, key)),
                    truncate(encryptionKey.getBytes()), truncate(encryptionKey.getBytes()));
            try (FileOutputStream fos = new FileOutputStream(new File(folder, cKey + ".cae"))) {
                fos.write(encryptedData);
            }
        } catch (Exception e) {
            log.error("Failed to save encrypted data for key:");
            log.error(e.getMessage());
        }
    }

    /**
     * Decrypts the given encrypted data using AES.
     *
     * @param encryptedData The encrypted data as a byte array.
     * @param key           The AES key as a byte array.
     * @param iv            The initialization vector as a byte array.
     * @return The decrypted data as a {@link String}.
     * @throws Exception If decryption fails.
     */
    private String decrypt(byte[] encryptedData, byte[] key, byte[] iv) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] decryptedBytes = cipher.doFinal(encryptedData);
        return new String(decryptedBytes);
    }

    /**
     * Encrypts the given data using AES.
     *
     * @param data The data to encrypt as a {@link String}.
     * @param key  The AES key as a byte array.
     * @param iv   The initialization vector as a byte array.
     * @return The encrypted data as a byte array.
     * @throws Exception If encryption fails.
     */
    private byte[] encrypt(String data, byte[] key, byte[] iv) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        return cipher.doFinal(data.getBytes());
    }

    /**
     * Loads encrypted data from a file.
     *
     * @param filePath The path to the file containing the encrypted data.
     * @return The encrypted data as a byte array.
     * @throws IOException If reading the file fails.
     */
    private byte[] loadEncryptedData(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            return fis.readAllBytes();
        }
    }

    private byte[] truncate(byte[] input) {
        return Arrays.copyOf(input, 16);
    }


    public record ConnectionKey(String name, byte[] key) {}
}
