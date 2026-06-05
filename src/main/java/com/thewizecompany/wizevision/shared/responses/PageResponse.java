package com.thewizecompany.wizevision.shared.responses;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/*
 * PAGINATED RESPONSE WRAPPER
 *
 * When returning lists (employees, bids, invoices),
 * you never return all records at once — that would
 * crash with large data sets.
 *
 * This wraps Spring's Page<T> into a clean JSON structure:
 * {
 *   "content": [...],
 *   "page": 0,
 *   "size": 20,
 *   "totalElements": 150,
 *   "totalPages": 8,
 *   "first": true,
 *   "last": false,
 *   "hasNext": true
 * }
 *
 * Frontend uses totalPages to build pagination controls.
 * Frontend uses hasNext to know if "Load More" should appear.
 *
 * Usage in service:
 *   Page<Employee> page = employeeRepository.findAll(pageable);
 *   return PageResponse.from(page.map(employeeMapper::toResponse));
 */
@Getter
@Builder
public class PageResponse<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean first;
    private final boolean last;
    private final boolean hasNext;
    private final boolean hasPrevious;

    /*
     * FACTORY METHOD
     * Takes Spring's Page<T> and converts to our clean response.
     * T must already be the DTO type, not the entity type.
     * Map entities to DTOs BEFORE calling this method.
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}