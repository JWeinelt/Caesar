package de.julianweinelt.caesar.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.NoSuchFileException;

@Deprecated
/**
 * A utility class for managing the saving and loading of objects to and from JSON files,
 * with optional encryption support.
 *
 * <p>The class is generic and allows type-safe handling of specific types. It uses GSON for
 * serialization and deserialization, and AES encryption for secure storage if enabled.</p>
 *
 * @param <T> The type of object to be managed by this class.
 */
public abstract class LoadableManager<T> {

    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Logger log;
    private final boolean encrypt;
    private final String encryptionKey;
    private T toSave;

    /**
     * Constructs a new instance of {@code LoadableManager}.
     *
     * @param log           The {@link Logger} instance used for logging information or errors.
     * @param encrypt       A flag indicating whether encryption is enabled.
     * @param encryptionKey The encryption key used for encrypting and decrypting data.
     *                      Must be a valid AES key.
     */
    protected LoadableManager(Logger log, boolean encrypt, String encryptionKey) {
        this.log = log;
        this.encrypt = encrypt;
        this.encryptionKey = encryptionKey;
    }

    /**
     * Loads a JSON file from the specified file path and deserializes it into the specified type.
     *
     * <p>If encryption is enabled, the method attempts to decrypt the file content
     * before deserializing it.</p>
     *
     * @param path The file path of the JSON file to be loaded.
     * @param type The type of the object to be deserialized. This can be specified
     *             using a {@link com.google.gson.reflect.TypeToken}.
     * @return The deserialized object of type {@code T}, or {@code null} if an error occurs
     *         while reading or deserializing the file.
     * @throws NullPointerException if the specified file path is {@code null}.
     * @throws NoSuchFileException  if the specified file does not exist.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Type mapType = new TypeToken<Map<String, Integer>>(){}.getType();
     * Map<String, Integer> myMap = loadObject("path/to/file.json", mapType);
     * }</pre>
     */
    @Nullable
    public T loadObject(String path, Type type) throws NoSuchFileException {
        if (!new File(path).exists()) {
            throw new NoSuchFileException(path);
        }
        if (encrypt) {
            try {
                byte[] encryptedData = loadEncryptedData(path);
                byte[] key = encryptionKey.getBytes();
                byte[] iv = encryptionKey.getBytes();
                String decryptedData = decrypt(encryptedData, key, iv);
                return GSON.fromJson(decryptedData, type);
            } catch (Exception e) {
                log.error("Could not parse file. Maybe it's not encrypted?");
                log.error(e.getMessage());
                return null;
            }
        } else {
            try (BufferedReader br = new BufferedReader(new FileReader(path))) {
                StringBuilder jsonStringBuilder = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    jsonStringBuilder.append(line);
                }
                return GSON.fromJson(jsonStringBuilder.toString(), type);
            } catch (IOException e) {
                log.error(e.getMessage());
                return null;
            }
        }
    }

    /**
     * Saves the given object as a JSON file at the specified path.
     *
     * <p>If encryption is enabled, the method encrypts the JSON content before writing it to the file.</p>
     *
     * @param directory The directory where the JSON file should be saved.
     *                  If the directory does not exist, it will be created.
     * @param fileName  The name of the JSON file (without the path).
     * @throws NullPointerException if {@code directory} or {@code fileName} is {@code null}.
     */
    public void saveObject(File directory, String fileName) {
        if (directory.mkdirs()) {
            log.info("Creating data folders...");
        }
        if (encrypt) {
            try {
                byte[] encryptedData = encrypt(GSON.toJson(toSave), encryptionKey.getBytes(), encryptionKey.getBytes());
                try (FileOutputStream fos = new FileOutputStream(new File(directory, fileName + ".json"))) {
                    fos.write(encryptedData);
                }
            } catch (Exception e) {
                log.error("Failed to save encrypted data:");
                log.error(e.getMessage());
            }
        } else {
            try (FileWriter writer = new FileWriter(new File(directory, fileName + ".json"))) {
                writer.write(GSON.toJson(toSave));
            } catch (IOException e) {
                log.error("Failed to save object: " + e.getMessage());
            }
        }
    }

    /**
     * Saves the given object as a JSON file at the specified path.
     *
     * <p>If encryption is enabled, the method encrypts the JSON content before writing it to the file.</p>
     *
     * @param file  The name of the JSON file (without the path).
     * @throws NullPointerException if {@code directory} or {@code fileName} is {@code null}.
     */
    public void saveObject(File file) {
        if (encrypt) {
            try {
                byte[] encryptedData = encrypt(GSON.toJson(toSave), encryptionKey.getBytes(), encryptionKey.getBytes());
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(encryptedData);
                }
            } catch (Exception e) {
                log.error("Failed to save encrypted data:");
                log.error(e.getMessage());
            }
        } else {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(GSON.toJson(toSave));
            } catch (IOException e) {
                log.error("Failed to save object: " + e.getMessage());
            }
        }
    }

    /**
     * Sets the object to be saved. This object will be serialized to JSON when {@link #saveObject(File, String)} is called.
     *
     * @param object The object to save.
     * @throws NullPointerException if {@code object} is {@code null}.
     */
    public void setDataToSave(T object) {
        this.toSave = object;
    }

    /**
     * Retrieves the currently set object to save.
     *
     * @return The object to be saved.
     */
    public T getSaveData() {
        return toSave;
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


    public abstract void loadData();
    public abstract void saveData();
}
