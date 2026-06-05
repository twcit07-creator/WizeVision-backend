package com.thewizecompany.wizevision.client.service;

import com.thewizecompany.wizevision.client.domain.Client;
import com.thewizecompany.wizevision.client.domain.ClientContact;
import com.thewizecompany.wizevision.client.dto.ClientContactResponse;
import com.thewizecompany.wizevision.client.dto.ClientResponse;
import com.thewizecompany.wizevision.client.dto.ClientSummaryResponse;
import com.thewizecompany.wizevision.client.dto.CreateClientContactRequest;
import com.thewizecompany.wizevision.client.dto.CreateClientRequest;
import com.thewizecompany.wizevision.client.dto.UpdateClientContactRequest;
import com.thewizecompany.wizevision.client.dto.UpdateClientRequest;
import com.thewizecompany.wizevision.client.repository.ClientContactRepository;
import com.thewizecompany.wizevision.client.repository.ClientRepository;
import com.thewizecompany.wizevision.shared.exception.BusinessException;
import com.thewizecompany.wizevision.shared.exception.DuplicateResourceException;
import com.thewizecompany.wizevision.shared.exception.ResourceNotFoundException;
import com.thewizecompany.wizevision.shared.responses.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientContactRepository contactRepository;

    // ─────────────────────────────────────────────────────────
    // CLIENT CRUD
    // ─────────────────────────────────────────────────────────

    @Transactional
    public ClientResponse createClient(
            CreateClientRequest request) {

        /*
         * Check for duplicate company name.
         * Case-insensitive — "ABC Fabricators" and
         * "abc fabricators" are the same company.
         */
        if (clientRepository
                .existsByCompanyNameIgnoreCaseAndIsDeletedFalse(
                        request.getCompanyName()
                )) {
            throw new DuplicateResourceException(
                    "Client",
                    "company name",
                    request.getCompanyName()
            );
        }

        String companyCode = generateCompanyCode();

        Client client = Client.builder()
                .companyCode(companyCode)
                .companyName(request.getCompanyName().trim())
                .email(request.getEmail())
                .phone(request.getPhone())
                .website(request.getWebsite())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .pincode(request.getPincode())
                .gstNumber(request.getGstNumber())
                .industryType(request.getIndustryType())
                .notes(request.getNotes())
                .isActive(true)
                .build();

        Client saved = clientRepository.save(client);

        log.info(
                "Client created: {} ({})",
                saved.getCompanyName(),
                saved.getCompanyCode()
        );

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public ClientResponse getById(UUID id) {
        Client client = findClient(id);
        return mapToResponse(client);
    }

    @Transactional(readOnly = true)
    public PageResponse<ClientSummaryResponse> searchClients(
            String search,
            Boolean isActive,
            Pageable pageable) {

        Page<Client> page = clientRepository
                .searchClients(search, isActive, pageable);

        return PageResponse.from(
                page.map(this::mapToSummary)
        );
    }

    @Transactional(readOnly = true)
    public List<ClientSummaryResponse> getAllActiveForDropdown() {
        /*
         * Returns all active clients for dropdown selects.
         * Used in bid creation form:
         * "Select Client" → this list.
         * No pagination — dropdown needs all options.
         * In practice, a steel detailing company
         * rarely has more than a few hundred clients.
         */
        return clientRepository
                .findAllActiveForDropdown()
                .stream()
                .map(this::mapToSummary)
                .toList();
    }

    @Transactional
    public ClientResponse updateClient(
            UUID id,
            UpdateClientRequest request) {

        Client client = findClient(id);

        /*
         * Check for name conflict only if name is changing.
         */
        if (request.getCompanyName() != null
                && !request.getCompanyName()
                .equalsIgnoreCase(client.getCompanyName())) {
            if (clientRepository
                    .existsByCompanyNameIgnoreCaseAndIsDeletedFalse(
                            request.getCompanyName()
                    )) {
                throw new DuplicateResourceException(
                        "Client",
                        "company name",
                        request.getCompanyName()
                );
            }
            client.setCompanyName(
                    request.getCompanyName().trim()
            );
        }

        if (request.getEmail() != null)
            client.setEmail(request.getEmail());
        if (request.getPhone() != null)
            client.setPhone(request.getPhone());
        if (request.getWebsite() != null)
            client.setWebsite(request.getWebsite());
        if (request.getAddressLine1() != null)
            client.setAddressLine1(request.getAddressLine1());
        if (request.getAddressLine2() != null)
            client.setAddressLine2(request.getAddressLine2());
        if (request.getCity() != null)
            client.setCity(request.getCity());
        if (request.getState() != null)
            client.setState(request.getState());
        if (request.getCountry() != null)
            client.setCountry(request.getCountry());
        if (request.getPincode() != null)
            client.setPincode(request.getPincode());
        if (request.getGstNumber() != null)
            client.setGstNumber(request.getGstNumber());
        if (request.getIndustryType() != null)
            client.setIndustryType(request.getIndustryType());
        if (request.getNotes() != null)
            client.setNotes(request.getNotes());

        Client saved = clientRepository.save(client);

        log.info("Client updated: {}", saved.getCompanyCode());

        return mapToResponse(saved);
    }

    @Transactional
    public void deactivateClient(UUID id) {
        Client client = findClient(id);

        if (!client.isActive()) {
            throw new BusinessException(
                    "Client is already inactive",
                    "CLIENT_ALREADY_INACTIVE"
            );
        }

        client.setActive(false);
        clientRepository.save(client);

        log.info(
                "Client deactivated: {}",
                client.getCompanyCode()
        );
    }

    @Transactional
    public void deleteClient(UUID id, String deletedBy) {
        Client client = findClient(id);
        client.markAsDeleted(deletedBy);
        clientRepository.save(client);

        log.info(
                "Client soft deleted: {} by: {}",
                client.getCompanyCode(),
                deletedBy
        );
    }

    // ─────────────────────────────────────────────────────────
    // CLIENT CONTACT CRUD
    // ─────────────────────────────────────────────────────────

    @Transactional
    public ClientContactResponse addContact(
            UUID clientId,
            CreateClientContactRequest request) {

        Client client = findClient(clientId);

        /*
         * If this contact is being set as primary,
         * demote the existing primary contact first.
         */
        if (request.isPrimary()) {
            contactRepository.demotePrimaryContact(clientId);
        }

        /*
         * If this is the FIRST contact for this client,
         * make them primary automatically.
         */
        List<ClientContact> existing =
                contactRepository
                        .findByClient_IdAndIsDeletedFalse(clientId);

        boolean shouldBePrimary = request.isPrimary()
                || existing.isEmpty();

        ClientContact contact = ClientContact.builder()
                .client(client)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .designation(request.getDesignation())
                .email(request.getEmail())
                .phone(request.getPhone())
                .whatsapp(request.getWhatsapp())
                .notes(request.getNotes())
                .isPrimary(shouldBePrimary)
                .isActive(true)
                .build();

        ClientContact saved = contactRepository.save(contact);

        log.info(
                "Contact added: {} {} to client: {}",
                saved.getFirstName(),
                saved.getLastName(),
                client.getCompanyCode()
        );

        return mapContactToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ClientContactResponse> getContacts(
            UUID clientId) {

        findClient(clientId); // Validate client exists

        return contactRepository
                .findByClient_IdAndIsDeletedFalse(clientId)
                .stream()
                .map(this::mapContactToResponse)
                .toList();
    }

    @Transactional
    public ClientContactResponse updateContact(
            UUID clientId,
            UUID contactId,
            UpdateClientContactRequest request) {

        ClientContact contact = contactRepository
                .findByIdAndIsDeletedFalse(contactId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "ClientContact", contactId.toString()
                        )
                );

        /*
         * Verify contact belongs to this client.
         * Prevents updating a contact from a different client.
         */
        if (!contact.getClient().getId().equals(clientId)) {
            throw new BusinessException(
                    "Contact does not belong to this client",
                    "CONTACT_CLIENT_MISMATCH"
            );
        }

        if (request.getFirstName() != null)
            contact.setFirstName(request.getFirstName().trim());
        if (request.getLastName() != null)
            contact.setLastName(request.getLastName().trim());
        if (request.getDesignation() != null)
            contact.setDesignation(request.getDesignation());
        if (request.getEmail() != null)
            contact.setEmail(request.getEmail());
        if (request.getPhone() != null)
            contact.setPhone(request.getPhone());
        if (request.getWhatsapp() != null)
            contact.setWhatsapp(request.getWhatsapp());
        if (request.getNotes() != null)
            contact.setNotes(request.getNotes());

        /*
         * Promote to primary if requested.
         * Demote previous primary first.
         */
        if (Boolean.TRUE.equals(request.getIsPrimary())
                && !contact.isPrimary()) {
            contactRepository.demotePrimaryContact(clientId);
            contact.setPrimary(true);
        }

        ClientContact saved = contactRepository.save(contact);
        return mapContactToResponse(saved);
    }

    @Transactional
    public void deleteContact(
            UUID clientId,
            UUID contactId,
            String deletedBy) {

        ClientContact contact = contactRepository
                .findByIdAndIsDeletedFalse(contactId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "ClientContact", contactId.toString()
                        )
                );

        if (!contact.getClient().getId().equals(clientId)) {
            throw new BusinessException(
                    "Contact does not belong to this client",
                    "CONTACT_CLIENT_MISMATCH"
            );
        }

        /*
         * If deleting the primary contact,
         * warn but allow — HR/Marketing can reassign.
         */
        contact.markAsDeleted(deletedBy);
        contactRepository.save(contact);

        log.info(
                "Contact deleted: {} from client: {}",
                contact.getFullName(),
                clientId
        );
    }

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────

    private Client findClient(UUID id) {
        return clientRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Client", id.toString()
                        )
                );
    }

    private String generateCompanyCode() {
        long count = clientRepository.countByIsDeletedFalse();
        return "WC-" + String.format("%03d", count + 1);
    }

    private ClientResponse mapToResponse(Client client) {
        List<ClientContactResponse> contacts =
                contactRepository
                        .findByClient_IdAndIsDeletedFalse(
                                client.getId()
                        )
                        .stream()
                        .map(this::mapContactToResponse)
                        .toList();

        return ClientResponse.builder()
                .id(client.getId())
                .companyCode(client.getCompanyCode())
                .companyName(client.getCompanyName())
                .email(client.getEmail())
                .phone(client.getPhone())
                .website(client.getWebsite())
                .addressLine1(client.getAddressLine1())
                .addressLine2(client.getAddressLine2())
                .city(client.getCity())
                .state(client.getState())
                .country(client.getCountry())
                .pincode(client.getPincode())
                .gstNumber(client.getGstNumber())
                .industryType(client.getIndustryType())
                .notes(client.getNotes())
                .isActive(client.isActive())
                .createdAt(client.getCreatedAt())
                .createdBy(client.getCreatedBy())
                .contacts(contacts)
                .totalProjects(0L)  // populated when projects module is built
                .totalBids(0L)      // populated when bidding module is built
                .build();
    }

    private ClientSummaryResponse mapToSummary(Client client) {
        /*
         * Fetch primary contact for the dropdown display.
         * "ABC Fabricators — John Smith (Primary)"
         */
        var primaryContact = contactRepository
                .findByClient_IdAndIsPrimaryTrueAndIsDeletedFalse(
                        client.getId()
                );

        return ClientSummaryResponse.builder()
                .id(client.getId())
                .companyCode(client.getCompanyCode())
                .companyName(client.getCompanyName())
                .city(client.getCity())
                .phone(client.getPhone())
                .email(client.getEmail())
                .isActive(client.isActive())
                .primaryContactName(
                        primaryContact.map(ClientContact::getFullName)
                                .orElse(null)
                )
                .primaryContactPhone(
                        primaryContact.map(ClientContact::getPhone)
                                .orElse(null)
                )
                .build();
    }

    private ClientContactResponse mapContactToResponse(
            ClientContact contact) {
        return ClientContactResponse.builder()
                .id(contact.getId())
                .clientId(contact.getClient().getId())
                .clientName(contact.getClient().getCompanyName())
                .firstName(contact.getFirstName())
                .lastName(contact.getLastName())
                .fullName(contact.getFullName())
                .designation(contact.getDesignation())
                .email(contact.getEmail())
                .phone(contact.getPhone())
                .whatsapp(contact.getWhatsapp())
                .notes(contact.getNotes())
                .isPrimary(contact.isPrimary())
                .isActive(contact.isActive())
                .createdAt(contact.getCreatedAt())
                .build();
    }
}