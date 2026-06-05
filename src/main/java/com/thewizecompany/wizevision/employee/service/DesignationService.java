package com.thewizecompany.wizevision.employee.service;

import com.thewizecompany.wizevision.employee.domain.Designation;
import com.thewizecompany.wizevision.employee.dto.CreateDesignationRequest;
import com.thewizecompany.wizevision.employee.dto.DesignationResponse;
import com.thewizecompany.wizevision.employee.repository.DepartmentRepository;
import com.thewizecompany.wizevision.employee.repository.DesignationRepository;
import com.thewizecompany.wizevision.shared.exception.BusinessException;
import com.thewizecompany.wizevision.shared.exception.DuplicateResourceException;
import com.thewizecompany.wizevision.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DesignationService {

    private final DesignationRepository designationRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public DesignationResponse create(
            CreateDesignationRequest request) {

        /*
         * Check for duplicate title within same department.
         * Same title in different departments is allowed.
         * Example: "Manager" can exist in HR and in Finance.
         */
        if (request.getDepartmentId() != null
                && designationRepository
                .existsByTitleAndDepartment_IdAndIsDeletedFalse(
                        request.getTitle(),
                        request.getDepartmentId()
                )) {
            throw new DuplicateResourceException(
                    "Designation", "title", request.getTitle()
            );
        }

        Designation designation = new Designation();
        designation.setTitle(request.getTitle().trim());
        designation.setDescription(request.getDescription());
        designation.setActive(true);

        if (request.getDepartmentId() != null) {
            var department = departmentRepository
                    .findByIdAndIsDeletedFalse(
                            request.getDepartmentId()
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Department",
                                    request.getDepartmentId().toString()
                            )
                    );
            designation.setDepartment(department);
        }

        Designation saved = designationRepository.save(designation);
        log.info("Designation created: {}", saved.getTitle());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DesignationResponse> getAllActive() {
        return designationRepository
                .findByIsActiveTrueAndIsDeletedFalse()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DesignationResponse> getByDepartment(
            UUID departmentId) {
        return designationRepository
                .findByDepartment_IdAndIsActiveTrueAndIsDeletedFalse(
                        departmentId
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DesignationResponse getById(UUID id) {
        return mapToResponse(findDesignation(id));
    }

    @Transactional
    public void deactivate(UUID id) {
        Designation designation = findDesignation(id);

        if (!designation.isActive()) {
            throw new BusinessException(
                    "Designation is already inactive",
                    "DESIGNATION_ALREADY_INACTIVE"
            );
        }

        designation.setActive(false);
        designationRepository.save(designation);
    }

    @Transactional
    public void delete(UUID id, String deletedBy) {
        Designation designation = findDesignation(id);
        designation.markAsDeleted(deletedBy);
        designationRepository.save(designation);
    }

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────

    private Designation findDesignation(UUID id) {
        return designationRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Designation", id.toString()
                        )
                );
    }

    private DesignationResponse mapToResponse(
            Designation designation) {
        return DesignationResponse.builder()
                .id(designation.getId())
                .title(designation.getTitle())
                .description(designation.getDescription())
                .departmentId(
                        designation.getDepartment() != null
                                ? designation.getDepartment().getId()
                                : null
                )
                .departmentName(
                        designation.getDepartment() != null
                                ? designation.getDepartment().getName()
                                : null
                )
                .isActive(designation.isActive())
                .createdAt(designation.getCreatedAt())
                .build();
    }
}