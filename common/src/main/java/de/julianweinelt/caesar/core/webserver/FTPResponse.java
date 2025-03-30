package de.julianweinelt.caesar.core.webserver;

import com.google.gson.Gson;
import lombok.Getter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Getter
public class FTPResponse {
    private final List<String> folders = new ArrayList<>();
    private final List<String> files = new ArrayList<>();

    public void setup(File path) {
        File[] result = path.listFiles();
        if (result== null) return;
        for (File f : result) {
            if (f.isFile()) files.add(f.getName());
            else folders.add(f.getName());
        }
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}