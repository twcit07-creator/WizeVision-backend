package com.thewizecompany.wizevision.template.domain;

import com.thewizecompany.wizevision.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
 * DOCUMENT TEMPLATE
 *
 * Stores HTML/Thymeleaf templates for PDF generation.
 *
 * content field stores the full HTML template.
 * Thymeleaf expressions are used to inject data:
 * ${invoice.invoiceNumber}, ${invoice.clientName}, etc.
 *
 * IT Admin uploads templates via the portal.
 * One template per type can be DEFAULT.
 * DEFAULT template is used when generating PDFs
 * unless a specific template is requested.
 *
 * Templates are validated on upload:
 * - Must be valid HTML
 * - Must contain required Thymeleaf variables
 *   for the template type
 */
@Entity
@Table(
        name = "document_templates",
        indexes = {
                @Index(
                        name = "idx_templates_type",
                        columnList = "template_type"
                ),
                @Index(
                        name = "idx_templates_default",
                        columnList = "template_type, is_default"
                ),
                @Index(
                        name = "idx_templates_is_deleted",
                        columnList = "is_deleted"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTemplate extends BaseEntity {

    @Column(
            name = "name",
            nullable = false,
            length = 100
    )
    private String name;

    @Column(
            name = "description",
            length = 500
    )
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "template_type",
            nullable = false,
            length = 30
    )
    private TemplateType templateType;

    /*
     * The HTML/Thymeleaf template content.
     * Stored as TEXT in the database.
     * Can be several kilobytes for complex templates.
     */
    @Column(
            name = "content",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String content;

    /*
     * Version tag for tracking changes.
     * Example: "v1.0", "v1.1-updated-logo"
     */
    @Column(name = "template_version", length = 20)
    private String templateVersion;

    /*
     * Only one template per type can be default.
     * Enforced by partial unique index in SQL.
     * Service layer ensures this before saving.
     */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    /*
     * Preview thumbnail URL (optional).
     * IT admin can upload a screenshot of
     * what the generated PDF looks like.
     */
    @Column(
            name = "preview_image_url",
            length = 500
    )
    private String previewImageUrl;
}