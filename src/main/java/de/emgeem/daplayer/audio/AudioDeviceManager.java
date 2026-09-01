package de.emgeem.daplayer.audio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

public class AudioDeviceManager {

    private static final String AUDIO_DEVICE_KEY = "audio.device";

    private Mixer.Info selectedDevice;
    private final File configFile;

    public AudioDeviceManager() {
        File configDir = new File(System.getProperty("user.home"), "AppData/Roaming/emgeem/DAPlayer");
        configDir.mkdirs();
        configFile = new File(configDir, "settings.properties");
        List<Mixer.Info> devices = getDevices();
        if (!devices.isEmpty()) {selectedDevice = devices.get(0);}
        load();
    }

    public List<Mixer.Info> getDevices() {
        List<Mixer.Info> devices = new ArrayList<>();
        DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, null);
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(info);
            if (!mixer.isLineSupported(lineInfo)) continue;
            if (info.getName().startsWith("Port ")) continue;
            if (info.getDescription().contains("Capture")) continue;
            if (info.getName().equals("Primärer Soundtreiber") || info.getName().equals("Primary Sound Driver")) {continue;}
            devices.add(info);
        }
        return devices;
    }

    public Mixer.Info getSelectedDevice() {return selectedDevice;}

    public void setSelectedDevice(Mixer.Info device) {this.selectedDevice = device;}

    public void save() {
        if (selectedDevice == null) {return;}
        Properties properties = new Properties();
        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                properties.load(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        properties.setProperty(AUDIO_DEVICE_KEY, selectedDevice.getName());
        try (FileOutputStream out = new FileOutputStream(configFile)) {
            properties.store(out, "DAPlayer Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        if (!configFile.exists()) {return;}
        Properties properties = new Properties();
        try (FileInputStream in = new FileInputStream(configFile)) {
            properties.load(in);
            String savedDevice = properties.getProperty(AUDIO_DEVICE_KEY);
            if (savedDevice == null) {return;}
            for (Mixer.Info info : getDevices()) {
                if (info.getName().equals(savedDevice)) {
                    selectedDevice = info;
                    return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}