package com.thewizecompany.wizevision.shared.service;


import com.thewizecompany.wizevision.shared.domain.SequenceType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SequenceService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public Integer nextSequence(SequenceType sequenceType, int year){
        String query = """
                INSERT INTO sequences (sequence_type, sequence_year, last_sequence) VALUES (?, ?, 1)
                ON CONFLICT (sequence_type, sequence_year) DO UPDATE SET last_sequence = sequences.last_sequence + 1
                RETURNING last_sequence
                """;
        Integer nextSequence = jdbcTemplate.queryForObject(query, Integer.class, sequenceType.name(), year);
        if(nextSequence == null){
            throw new IllegalStateException(
                    "Failed to generate sequence for "
                    + sequenceType
                    + " and year "
                    + year
            );
        }

        return nextSequence;
    }
}
