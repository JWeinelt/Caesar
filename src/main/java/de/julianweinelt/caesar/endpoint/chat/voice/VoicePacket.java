package de.julianweinelt.caesar.endpoint.chat.voice;

import java.util.Arrays;

public class VoicePacket {
    public byte type;       // 0=Audio,1=Event,2=Control
    public byte version;    // 1
    public byte flags;      // reserved
    public byte[] payload;  // content

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

    public static VoicePacket createAudio(byte[] opusData) {
        VoicePacket p = new VoicePacket();
        p.type = 0x00;
        p.version = 0x01;
        p.flags = 0;
        p.payload = opusData;
        return p;
    }

    public static VoicePacket createEvent(String msg) {
        VoicePacket p = new VoicePacket();
        p.type = 0x01;
        p.version = 0x01;
        p.flags = 0;
        p.payload = msg.getBytes();
        return p;
    }

    public static VoicePacket createControl(String msg) {
        VoicePacket p = new VoicePacket();
        p.type = 0x02;
        p.version = 0x01;
        p.flags = 0;
        p.payload = msg.getBytes();
        return p;
    }
}
