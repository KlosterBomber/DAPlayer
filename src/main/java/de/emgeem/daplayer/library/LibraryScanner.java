package de.emgeem.daplayer.library;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.emgeem.daplayer.decoder.DecoderManager;
import de.emgeem.daplayer.model.LibraryEntry;
import de.emgeem.daplayer.model.Track;

public class LibraryScanner {

    private static final int PROGRESS_INTERVAL = 25;

    @FunctionalInterface 
    public interface ScanListener {void onProgress(int done, int total, String currentFile);}

    public record ScanResult(List<LibraryEntry> entries, int added, int updated, int unchanged, int removed, int offline, int failed, boolean cancelled, long durationMillis) {}

    private final DecoderManager decoderManager;
    private volatile boolean cancelled;

    public LibraryScanner(DecoderManager decoderManager) {this.decoderManager = decoderManager;}
    
    public void cancel() {cancelled = true;}

    public ScanResult scan(List<String> folders, List<LibraryEntry> previouEntries, ScanListener listener) {
        cancelled = false;
        long start = System.currentTimeMillis();
        
        Map<String, LibraryEntry> previous = new HashMap<>();
        for (LibraryEntry entry : previouEntries) {previous.put(entry.getPath(), entry);}

        List<String> missingFolders = new ArrayList<>();
        List<File> files = new ArrayList<>();
        for (String folder : folders) {
            File root = new File(folder);
            if (!root.isDirectory()) {missingFolders.add(root.getAbsolutePath()); continue;}
            collectAudioFiles(root.toPath(), files);
        }
        files.sort(Comparator.comparing(File::getAbsolutePath, String.CASE_INSENSITIVE_ORDER));

        List<LibraryEntry> result = new ArrayList<>(files.size());
        int added = 0, updated = 0, unchanged = 0, failed = 0;
        for (int i = 0; i < files.size(); i++) {
            if (cancelled) {return new ScanResult(previouEntries, 0, 0, 0, 0, 0, 0, true, System.currentTimeMillis() - start);}
            File file = files.get(i);
            LibraryEntry old = previous.remove(file.getAbsolutePath());
            if (old != null && old.isUpToDate(file)) {
                result.add(old);
                unchanged++;
            } else {
                try {
                    Track track = decoderManager.readTrack(file);
                    result.add(LibraryEntry.fromTrack(track));
                    if (old == null) {added++;} else {updated++;}
                } catch (Exception e) {
                    failed++;
                    System.err.println("Nicht lesbar: " + file + " (" + e.getMessage() + ")");
                }
            }
            if (listener != null && (i % PROGRESS_INTERVAL == 0 || i == files.size() - 1)) {listener.onProgress(i + 1, files.size(), file.getName());}
        }

        int removed = 0;
        int offline = 0;
        for (LibraryEntry leftover : previous.values()) {
            if (isInsideAny(leftover.getPath(), missingFolders)) {
                result.add(leftover);
                offline++;
            } else {removed++;}
        }
        return new ScanResult(result, added, updated, unchanged, removed, offline, failed, false, System.currentTimeMillis() - start);
    }

    private void collectAudioFiles(Path root, List<File> files) {
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override 
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                    if (cancelled) {return FileVisitResult.TERMINATE;}
                    File file = path.toFile();
                    if (attrs.isRegularFile() && DecoderManager.isSupportedFile(file)) {files.add(file);}
                    return FileVisitResult.CONTINUE;
                }
                @Override 
                public FileVisitResult visitFileFailed(Path path, IOException e) {
                    System.err.println("Übersprungen: " + path + " (" + e.getMessage() + ")");
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {System.err.println("Ordner nicht lesbar: " + root + " (" + e.getMessage() + ")");}
    }

    private static boolean isInsideAny(String path, List<String> folders) {
        for (String folder : folders) {if (path.startsWith(folder + File.separator)) {return true;}}
        return false;
    }
}
