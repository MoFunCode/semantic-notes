package com.github.mofuncode.semantic_notes.repository;

import com.github.mofuncode.semantic_notes.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    Optional<Note> findByFilepath(String filepath);


}
