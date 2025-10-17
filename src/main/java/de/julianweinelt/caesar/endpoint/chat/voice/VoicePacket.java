package de.julianweinelt.caesar.endpoint.chat.voice;

import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;

/**
 * Represents a voice packet used in the voice communication protocol.<br>
 * <br>
 * Packet Structure:<br>
 * - Type (1 byte): 0=Audio, 1=Event, 2=Control<br>
 * - Version (1 byte): Protocol version (currently 1)<br>
 * - Flags (1 byte): Reserved for future use<br>
 * - Length (2 bytes): Length of the payload<br>
 * - Payload (variable length): The actual data (audio, event message, or control message)<br>
 *
 * @author Julian Weinelt
 */
@ApiStatus.Internal
public class VoicePacket {
    public byte type;       // 0=Audio,1=Event,2=Control
    public byte version;    // 1
    public byte flags;      // reserved
    public byte[] payload;  // content

    /**
     * Serializes the VoicePacket into a byte array.
     * @return The byte array representation of the VoicePacket.
     */
    public byte[] toBytes() {
        int length = payload.length;
        byte[] data = new byte[5 + length];
        data[0] = type;
        data[1] = 0x01; // Version
        data[2] = flags;
        data[3] = (byte)(length & 0xFF);
        data[4] = (byte)((length >> 8) & 0xFF);
        System.arraycopy(payload, 0, data, 5, length);
        return data;
    }

    /**
     * Deserializes a byte array into a VoicePacket.
     * @param data The byte array containing the VoicePacket data.
     * @param len The length of valid data in the array.
     * @return The deserialized VoicePacket.
     */
    public static VoicePacket fromBytes(byte[] data, int len) {
        VoicePacket p = new VoicePacket();
        p.type = data[0];
        p.version = data[1];
        p.flags = data[2];
        int length = (data[3] & 0xFF) | ((data[4] & 0xFF) << 8);
        if (length > len - 5) length = len - 5; // Safety
        p.payload = Arrays.copyOfRange(data, 5, 5 + length);
        return p;
    }

    /**
     * Creates an audio VoicePacket with the given Opus-encoded data.
     * @param opusData The Opus-encoded audio data.
     * @return The created audio VoicePacket.
     */
    public static VoicePacket createAudio(byte[] opusData) {
        VoicePacket p = new VoicePacket();
        p.type = 0x00;
        p.version = 0x01;
        p.flags = 0;
        p.payload = opusData;
        return p;
    }

    /**
     * Creates an event VoicePacket with the given message.
     * @param msg The event message.
     * @return The created event VoicePacket.
     */
    public static VoicePacket createEvent(String msg) {
        VoicePacket p = new VoicePacket();
        p.type = 0x01;
        p.version = 0x01;
        p.flags = 0;
        p.payload = msg.getBytes();
        return p;
    }

    /**
     * Creates a control VoicePacket with the given message.
     * @param msg The control message.
     * @return The created control VoicePacket.
     */
    public static VoicePacket createControl(String msg) {
        VoicePacket p = new VoicePacket();
        p.type = 0x02;
        p.version = 0x01;
        p.flags = 0;
        p.payload = msg.getBytes();
        return p;
    }
}
