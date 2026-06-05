package com.thewizecompany.wizevision.template.repository;

import com.thewizecompany.wizevision.template.domain.DocumentTemplate;
import com.thewizecompany.wizevision.template.domain.TemplateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentTemplateRepository
        extends JpaRepository<DocumentTemplate, UUID> {

    Optional<DocumentTemplate> findByIdAndIsDeletedFalse(
            UUID id
    );

    /*
     * Get the default template for a type.
     * Used when generating PDFs without specifying
     * a specific template.
     */
    Optional<DocumentTemplate>
    findByTemplateTypeAndIsDefaultTrueAndIsActiveTrueAndIsDeletedFalse(
            TemplateType templateType
    );

    List<DocumentTemplate>
    findByTemplateTypeAndIsActiveTrueAndIsDeletedFalse(
            TemplateType templateType
    );

    List<DocumentTemplate> findByIsDeletedFalse();

    /*
     * Before setting a new default, demote
     * the current default for that type.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE DocumentTemplate t
        SET t.isDefault = FALSE
        WHERE t.templateType = :type
        AND t.isDefault = TRUE
        AND t.isDeleted = FALSE
        """)
    void demoteCurrentDefault(
            @Param("type") TemplateType type
    );
}