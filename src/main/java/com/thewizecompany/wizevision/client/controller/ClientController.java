package com.thewizecompany.wizevision.client.controller;

import com.thewizecompany.wizevision.client.dto.ClientContactResponse;
import com.thewizecompany.wizevision.client.dto.ClientResponse;
import com.thewizecompany.wizevision.client.dto.ClientSummaryResponse;
import com.thewizecompany.wizevision.client.dto.CreateClientContactRequest;
import com.thewizecompany.wizevision.client.dto.CreateClientRequest;
import com.thewizecompany.wizevision.client.dto.UpdateClientContactRequest;
import com.thewizecompany.wizevision.client.dto.UpdateClientRequest;
import com.thewizecompany.wizevision.client.service.ClientService;
import com.thewizecompany.wizevision.shared.responses.ApiResponse;
import com.thewizecompany.wizevision.shared.responses.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Tag(
        name = "Client Management",
        description = "Manage fabricator clients and their contacts"
)
@SecurityRequirement(name = "bearerAuth")
public class ClientController {

    private final ClientService clientService;

    // ─────────────────────────────────────────────────────────
    // CLIENT ENDPOINTS
    // ─────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN'," +
                    "'MARKETING_EXECUTIVE','PROJECT_MANAGER')"
    )
    @Operation(summary = "Create a new client")
    public ResponseEntity<ApiResponse<ClientResponse>> create(
            @Valid @RequestBody CreateClientRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        clientService.createClient(request),
                        "Client created successfully"
                ));
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','MARKETING_EXECUTIVE'," +
                    "'PROJECT_MANAGER','HR_MANAGER','FINANCE')"
    )
    @Operation(summary = "Get client by ID with contacts")
    public ResponseEntity<ApiResponse<ClientResponse>> getById(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                ApiResponse.ok(clientService.getById(id))
        );
    }

    @GetMapping
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','MARKETING_EXECUTIVE'," +
                    "'PROJECT_MANAGER','HR_MANAGER','FINANCE')"
    )
    @Operation(
            summary = "Search clients with pagination",
            description = "Search by company name, code, city, or email"
    )
    public ResponseEntity<ApiResponse<PageResponse<ClientSummaryResponse>>>
    search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        clientService.searchClients(
                                search,
                                isActive,
                                PageRequest.of(
                                        page, size

                                )
                        )
                )
        );
    }

    /*
     * DROPDOWN ENDPOINT
     * Used by bid creation form to populate client selector.
     * Returns all active clients — lightweight summary only.
     */
    @GetMapping("/dropdown")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','MARKETING_EXECUTIVE'," +
                    "'PROJECT_MANAGER')"
    )
    @Operation(
            summary = "Get all active clients for dropdown",
            description = "Lightweight list for form selectors. " +
                    "No pagination."
    )
    public ResponseEntity<ApiResponse<List<ClientSummaryResponse>>>
    getForDropdown() {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        clientService.getAllActiveForDropdown()
                )
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','MARKETING_EXECUTIVE')"
    )
    @Operation(summary = "Update client details")
    public ResponseEntity<ApiResponse<ClientResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateClientRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        clientService.updateClient(id, request),
                        "Client updated successfully"
                )
        );
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Deactivate a client")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable UUID id) {

        clientService.deactivateClient(id);
        return ResponseEntity.ok(
                ApiResponse.ok("Client deactivated")
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Soft delete a client")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal String currentUserEmail) {

        clientService.deleteClient(id, currentUserEmail);
        return ResponseEntity.ok(
                ApiResponse.ok("Client deleted successfully")
        );
    }

    // ─────────────────────────────────────────────────────────
    // CLIENT CONTACT ENDPOINTS
    // Nested under /clients/{clientId}/contacts
    // ─────────────────────────────────────────────────────────

    @PostMapping("/{clientId}/contacts")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','MARKETING_EXECUTIVE'," +
                    "'PROJECT_MANAGER')"
    )
    @Operation(summary = "Add a contact to a client")
    public ResponseEntity<ApiResponse<ClientContactResponse>>
    addContact(
            @PathVariable UUID clientId,
            @Valid @RequestBody
            CreateClientContactRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        clientService.addContact(clientId, request),
                        "Contact added successfully"
                ));
    }

    @GetMapping("/{clientId}/contacts")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','MARKETING_EXECUTIVE'," +
                    "'PROJECT_MANAGER','FINANCE')"
    )
    @Operation(summary = "Get all contacts for a client")
    public ResponseEntity<ApiResponse<List<ClientContactResponse>>>
    getContacts(@PathVariable UUID clientId) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        clientService.getContacts(clientId)
                )
        );
    }

    @PutMapping("/{clientId}/contacts/{contactId}")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','MARKETING_EXECUTIVE')"
    )
    @Operation(summary = "Update a client contact")
    public ResponseEntity<ApiResponse<ClientContactResponse>>
    updateContact(
            @PathVariable UUID clientId,
            @PathVariable UUID contactId,
            @Valid @RequestBody
            UpdateClientContactRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        clientService.updateContact(
                                clientId, contactId, request
                        ),
                        "Contact updated successfully"
                )
        );
    }

    @DeleteMapping("/{clientId}/contacts/{contactId}")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','MARKETING_EXECUTIVE')"
    )
    @Operation(summary = "Delete a client contact")
    public ResponseEntity<ApiResponse<Void>> deleteContact(
            @PathVariable UUID clientId,
            @PathVariable UUID contactId,
            @AuthenticationPrincipal String currentUserEmail) {

        clientService.deleteContact(
                clientId, contactId, currentUserEmail
        );
        return ResponseEntity.ok(
                ApiResponse.ok("Contact deleted successfully")
        );
    }
}