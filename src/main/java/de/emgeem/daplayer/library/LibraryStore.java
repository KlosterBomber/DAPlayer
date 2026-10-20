package de.emgeem.daplayer.library;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import de.emgeem.daplayer.model.LibraryEntry;
import de.emgeem.daplayer.util.AppPaths;

public class LibraryStore {

    private static final String FILE_NAME = "library.json";
    private static final int FORMAT_VERSION = 1;

    private final Path file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public LibraryStore() {this(new File(AppPaths.getConfigDirectory(), FILE_NAME).toPath());}
    public LibraryStore(Path file) {this.file = file;}
    public Path getFile() {return file;}

    public static class LibraryData {
        private int version = FORMAT_VERSION;
        private List<String> folders = new ArrayList<>();
        private List<LibraryEntry> entries = new ArrayList<>();
        public LibraryData() {}
        public LibraryData(List<String> folders, List<LibraryEntry> entries) {
            this.folders = new ArrayList<>(folders);
            this.entries = new ArrayList<>(entries);
        }
        public int getVersion() {return version;}
        public List<String> getFolders() {return folders;}
        public List<LibraryEntry> getEntries() {return entries;}
    }

    public LibraryData load() {
        if (!Files.isRegularFile(file)) {return new LibraryData();}
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            LibraryData data = gson.fromJson(reader, LibraryData.class);
            if (data == null) {return new LibraryData();}
            if (data.folders == null) {data.folders = new ArrayList<>();}
            if (data.entries == null) {data.entries = new ArrayList<>();}
            data.entries.removeIf(entry -> entry == null || entry.getPath() == null);
            return data;
        } catch (IOException | JsonParseException e) {
            System.err.println("Mediathek nicht lesbar: " + e.getMessage());
            backupBrokenFile();
            return new LibraryData();
        }
    }

    private void backupBrokenFile() {
        try {
            Path backup = file.resolve(FILE_NAME + ".broken");
            Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
            System.err.println("Defekte Datei gesichert als: " + backup);
        } catch (IOException e) {System.err.println("Sicherung fehlgeschlagen: " + e.getMessage());}
    }

    public void save(LibraryData data) throws IOException {
        Path temp = file.resolveSibling(FILE_NAME + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {gson.toJson(data, writer);}
        try {Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);}
        catch (AtomicMoveNotSupportedException e) {Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);}
    }
}
