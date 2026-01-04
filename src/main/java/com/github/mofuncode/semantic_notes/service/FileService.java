package com.github.mofuncode.semantic_notes.service;

import com.github.mofuncode.semantic_notes.entity.Note;
import com.github.mofuncode.semantic_notes.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Service responsible for reading note files from the filesystem
 * and indexing them into the database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final NoteRepository noteRepository;

    // Spring injects this value from application.properties: notes.directory=/Users/erimo/test-notes
    @Value("${notes.directory}")
    private String notesDirectory;

    /**
     * Indexes all notes from the configured notes directory.
     * Scans for .md and .txt files, and saves them to the database.
     * If a note already exists (same filepath), it updates the content.
     *
     * @return number of notes successfully indexed
     */
    public int indexAllNotes() {
        log.info("Starting to index notes from directory: {}", notesDirectory);

        // Step 1: Validate that the directory exists
        Path rootPath = Paths.get(notesDirectory);
        if (!Files.exists(rootPath)) {
            String errorMsg = "Notes directory does not exist: " + notesDirectory;
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        if (!Files.isDirectory(rootPath)) {
            String errorMsg = "Path is not a directory: " + notesDirectory;
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // Counter for successfully indexed notes
        AtomicInteger indexedCount = new AtomicInteger(0);

        // Step 2: Walk through all files in the directory
        try (Stream<Path> paths = Files.walk(rootPath)) {
            paths
                .filter(Files::isRegularFile)  // Only process actual files, not directories
                .filter(this::hasValidExtension)  // Only .md and .txt files
                .forEach(path -> {
                    try {
                        indexNote(path);
                        indexedCount.incrementAndGet();
                    } catch (Exception e) {
                        // Don't stop the whole process if one file fails
                        log.error("Failed to index note: {}", path, e);
                    }
                });
        } catch (IOException e) {
            log.error("Error walking directory: {}", notesDirectory, e);
            throw new RuntimeException("Failed to walk notes directory", e);
        }

        int total = indexedCount.get();
        log.info("Successfully indexed {} notes", total);
        return total;
    }

    /**
     * Indexes a single note file.
     * If the note already exists in the database, updates it.
     * If it's new, creates a new database entry.
     *
     * @param filePath the path to the note file
     * @throws IOException if the file cannot be read
     */
    private void indexNote(Path filePath) throws IOException {
        log.debug("Indexing note: {}", filePath);

        // Read the file content
        String content = Files.readString(filePath);
        String filepath = filePath.toString();
        String filename = filePath.getFileName().toString();

        // Check if this note already exists in the database
        Optional<Note> existingNote = noteRepository.findByFilepath(filepath);

        if (existingNote.isPresent()) {
            // Update existing note
            Note note = existingNote.get();
            note.setContent(content);
            // updatedAt will be set automatically by @UpdateTimestamp
            noteRepository.save(note);
            log.debug("Updated existing note: {}", filename);
        } else {
            // Create new note
            Note note = new Note();
            note.setFilepath(filepath);
            note.setFilename(filename);
            note.setContent(content);
            // createdAt and updatedAt will be set automatically by Hibernate
            noteRepository.save(note);
            log.debug("Created new note: {}", filename);
        }
    }

    /**
     * Checks if a file has a valid extension (.md or .txt)
     *
     * @param path the file path to check
     * @return true if the file has .md or .txt extension
     */
    private boolean hasValidExtension(Path path) {
        String filename = path.getFileName().toString().toLowerCase();
        return filename.endsWith(".md") || filename.endsWith(".txt");
    }

}
