package com.thewizecompany.wizevision.employee.service;

import com.thewizecompany.wizevision.employee.domain.Department;
import com.thewizecompany.wizevision.employee.dto.CreateDepartmentRequest;
import com.thewizecompany.wizevision.employee.dto.DepartmentResponse;
import com.thewizecompany.wizevision.employee.repository.DepartmentRepository;
import com.thewizecompany.wizevision.employee.repository.EmployeeRepository;
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
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public DepartmentResponse create(
            CreateDepartmentRequest request) {

        if (departmentRepository
                .existsByNameAndIsDeletedFalse(request.getName())) {
            throw new DuplicateResourceException(
                    "Department", "name", request.getName()
            );
        }

        /*
         * Validate head employee exists if provided.
         */
        if (request.getHeadEmployeeId() != null) {
            employeeRepository
                    .findByIdAndIsDeletedFalse(
                            request.getHeadEmployeeId()
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Employee",
                                    request.getHeadEmployeeId().toString()
                            )
                    );
        }

        Department department = Department.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .headEmployeeId(request.getHeadEmployeeId())
                .isActive(true)
                .build();

        Department saved = departmentRepository.save(department);
        log.info("Department created: {}", saved.getName());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllActive() {
        return departmentRepository
                .findByIsActiveTrueAndIsDeletedFalse()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getById(UUID id) {
        return mapToResponse(findDepartment(id));
    }

    @Transactional
    public DepartmentResponse update(
            UUID id,
            CreateDepartmentRequest request) {

        Department department = findDepartment(id);

        /*
         * Check name uniqueness only if name is changing.
         */
        if (!department.getName()
                .equalsIgnoreCase(request.getName())) {
            if (departmentRepository
                    .existsByNameAndIsDeletedFalse(
                            request.getName()
                    )) {
                throw new DuplicateResourceException(
                        "Department", "name", request.getName()
                );
            }
        }

        if (request.getName() != null) {
            department.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            department.setDescription(request.getDescription());
        }
        if (request.getHeadEmployeeId() != null) {
            employeeRepository
                    .findByIdAndIsDeletedFalse(
                            request.getHeadEmployeeId()
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Employee",
                                    request.getHeadEmployeeId().toString()
                            )
                    );
            department.setHeadEmployeeId(
                    request.getHeadEmployeeId()
            );
        }

        return mapToResponse(departmentRepository.save(department));
    }

    @Transactional
    public void deactivate(UUID id) {
        Department department = findDepartment(id);

        if (!department.isActive()) {
            throw new BusinessException(
                    "Department is already inactive",
                    "DEPARTMENT_ALREADY_INACTIVE"
            );
        }

        department.setActive(false);
        departmentRepository.save(department);
        log.info("Department deactivated: {}", department.getName());
    }

    @Transactional
    public void delete(UUID id, String deletedBy) {
        Department department = findDepartment(id);
        department.markAsDeleted(deletedBy);
        departmentRepository.save(department);
        log.info(
                "Department deleted: {} by: {}",
                department.getName(), deletedBy
        );
    }

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────

    private Department findDepartment(UUID id) {
        return departmentRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Department", id.toString()
                        )
                );
    }

    private DepartmentResponse mapToResponse(Department dept) {
        /*
         * Fetch head employee name if headEmployeeId is set.
         * We fetch only what we need — not the full entity.
         */
        String headName = null;
        String headCode = null;

        if (dept.getHeadEmployeeId() != null) {
            var head = employeeRepository
                    .findByIdAndIsDeletedFalse(
                            dept.getHeadEmployeeId()
                    );
            if (head.isPresent()) {
                headName = head.get().getFullName();
                headCode = head.get().getEmployeeCode();
            }
        }

        return DepartmentResponse.builder()
                .id(dept.getId())
                .name(dept.getName())
                .description(dept.getDescription())
                .headEmployeeId(dept.getHeadEmployeeId())
                .headEmployeeName(headName)
                .headEmployeeCode(headCode)
                .isActive(dept.isActive())
                .createdAt(dept.getCreatedAt())
                .createdBy(dept.getCreatedBy())
                .build();
    }
}