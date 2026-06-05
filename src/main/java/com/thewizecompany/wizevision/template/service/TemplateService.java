package com.thewizecompany.wizevision.template.service;

import com.thewizecompany.wizevision.shared.exception.BusinessException;
import com.thewizecompany.wizevision.shared.exception.ResourceNotFoundException;
import com.thewizecompany.wizevision.template.domain.DocumentTemplate;
import com.thewizecompany.wizevision.template.domain.TemplateType;
import com.thewizecompany.wizevision.template.dto.CreateTemplateRequest;
import com.thewizecompany.wizevision.template.dto.TemplateResponse;
import com.thewizecompany.wizevision.template.dto.UpdateTemplateRequest;
import com.thewizecompany.wizevision.template.repository.DocumentTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final DocumentTemplateRepository templateRepository;

    // ─────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────

    @Transactional
    public TemplateResponse createTemplate(
            CreateTemplateRequest request) {

        /*
         * If setting as default, demote current default first.
         */
        if (request.isSetAsDefault()) {
            templateRepository.demoteCurrentDefault(
                    request.getTemplateType()
            );
        }

        DocumentTemplate template = DocumentTemplate.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .templateType(request.getTemplateType())
                .content(request.getContent())
                .templateVersion(request.getTemplateVersion())
                .isDefault(request.isSetAsDefault())
                .isActive(true)
                .previewImageUrl(request.getPreviewImageUrl())
                .build();

        DocumentTemplate saved =
                templateRepository.save(template);

        log.info(
                "Template created: {} ({})",
                saved.getName(),
                saved.getTemplateType()
        );

        return mapToResponse(saved, true);
    }

    // ─────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TemplateResponse> getAll() {
        return templateRepository
                .findByIsDeletedFalse()
                .stream()
                .map(t -> mapToResponse(t, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> getByType(
            TemplateType type) {
        return templateRepository
                .findByTemplateTypeAndIsActiveTrueAndIsDeletedFalse(
                        type
                )
                .stream()
                .map(t -> mapToResponse(t, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public TemplateResponse getById(UUID id) {
        return mapToResponse(findTemplate(id), true);
    }

    // ─────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────

    @Transactional
    public TemplateResponse updateTemplate(
            UUID id,
            UpdateTemplateRequest request) {

        DocumentTemplate template = findTemplate(id);

        if (request.getName() != null)
            template.setName(request.getName().trim());
        if (request.getDescription() != null)
            template.setDescription(request.getDescription());
        if (request.getContent() != null)
            template.setContent(request.getContent());
        if (request.getTemplateVersion() != null)
            template.setTemplateVersion(
                    request.getTemplateVersion()
            );
        if (request.getPreviewImageUrl() != null)
            template.setPreviewImageUrl(
                    request.getPreviewImageUrl()
            );
        if (request.getIsActive() != null)
            template.setActive(request.getIsActive());

        /*
         * Promote to default if requested.
         */
        if (Boolean.TRUE.equals(request.getSetAsDefault())
                && !template.isDefault()) {
            templateRepository.demoteCurrentDefault(
                    template.getTemplateType()
            );
            template.setDefault(true);
        }

        DocumentTemplate saved =
                templateRepository.save(template);

        log.info("Template updated: {}", saved.getName());

        return mapToResponse(saved, true);
    }

    // ─────────────────────────────────────────────────────────
    // SET DEFAULT
    // ─────────────────────────────────────────────────────────

    @Transactional
    public TemplateResponse setAsDefault(UUID id) {
        DocumentTemplate template = findTemplate(id);

        if (!template.isActive()) {
            throw new BusinessException(
                    "Cannot set an inactive template as default",
                    "TEMPLATE_INACTIVE"
            );
        }

        templateRepository.demoteCurrentDefault(
                template.getTemplateType()
        );
        template.setDefault(true);
        templateRepository.save(template);

        log.info(
                "Template set as default: {} for type: {}",
                template.getName(),
                template.getTemplateType()
        );

        return mapToResponse(template, false);
    }

    // ─────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────

    @Transactional
    public void deleteTemplate(UUID id, String deletedBy) {
        DocumentTemplate template = findTemplate(id);

        if (template.isDefault()) {
            throw new BusinessException(
                    "Cannot delete the default template. " +
                            "Set another template as default first.",
                    "CANNOT_DELETE_DEFAULT"
            );
        }

        template.markAsDeleted(deletedBy);
        templateRepository.save(template);

        log.info(
                "Template deleted: {} by: {}",
                template.getName(), deletedBy
        );
    }

    // ─────────────────────────────────────────────────────────
    // INTERNAL — used by PdfGenerationService
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DocumentTemplate getDefaultTemplate(
            TemplateType type) {
        return templateRepository
                .findByTemplateTypeAndIsDefaultTrueAndIsActiveTrueAndIsDeletedFalse(
                        type
                )
                .orElseThrow(() ->
                        new BusinessException(
                                "No default template found for " +
                                        type.getDisplayName() +
                                        ". Please upload and set a default " +
                                        "template in Settings → Templates.",
                                "NO_DEFAULT_TEMPLATE"
                        )
                );
    }

    @Transactional(readOnly = true)
    public DocumentTemplate getTemplateById(UUID id) {
        return findTemplate(id);
    }

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────

    private DocumentTemplate findTemplate(UUID id) {
        return templateRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Template", id.toString()
                        )
                );
    }

    private TemplateResponse mapToResponse(
            DocumentTemplate template,
            boolean includeContent) {

        return TemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .templateType(template.getTemplateType())
                .typeDisplay(
                        template.getTemplateType().getDisplayName()
                )
                .templateVersion(template.getTemplateVersion())
                .isDefault(template.isDefault())
                .isActive(template.isActive())
                .previewImageUrl(template.getPreviewImageUrl())
                .createdAt(template.getCreatedAt())
                .createdBy(template.getCreatedBy())
                .updatedAt(template.getUpdatedAt())
                .content(includeContent
                        ? template.getContent() : null)
                .build();
    }
}