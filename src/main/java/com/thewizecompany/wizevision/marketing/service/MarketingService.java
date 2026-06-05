package com.thewizecompany.wizevision.marketing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thewizecompany.wizevision.client.domain.Client;
import com.thewizecompany.wizevision.client.domain.ClientContact;
import com.thewizecompany.wizevision.client.repository.ClientContactRepository;
import com.thewizecompany.wizevision.client.repository.ClientRepository;
import com.thewizecompany.wizevision.employee.domain.Employee;
import com.thewizecompany.wizevision.employee.repository.EmployeeRepository;
import com.thewizecompany.wizevision.marketing.domain.InquiryStatus;
import com.thewizecompany.wizevision.marketing.domain.Lead;
import com.thewizecompany.wizevision.marketing.domain.LeadStatus;
import com.thewizecompany.wizevision.marketing.domain.ProjectInquiry;
import com.thewizecompany.wizevision.marketing.dto.ConvertLeadRequest;
import com.thewizecompany.wizevision.marketing.dto.CreateInquiryRequest;
import com.thewizecompany.wizevision.marketing.dto.CreateLeadRequest;
import com.thewizecompany.wizevision.marketing.dto.ForwardInquiryRequest;
import com.thewizecompany.wizevision.marketing.dto.InquiryResponse;
import com.thewizecompany.wizevision.marketing.dto.LeadResponse;
import com.thewizecompany.wizevision.marketing.dto.UpdateLeadRequest;
import com.thewizecompany.wizevision.marketing.repository.LeadRepository;
import com.thewizecompany.wizevision.marketing.repository.ProjectInquiryRepository;
import com.thewizecompany.wizevision.shared.exception.BusinessException;
import com.thewizecompany.wizevision.shared.exception.ResourceNotFoundException;
import com.thewizecompany.wizevision.shared.responses.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketingService {

    private final LeadRepository leadRepository;
    private final ProjectInquiryRepository inquiryRepository;
    private final EmployeeRepository employeeRepository;
    private final ClientRepository clientRepository;
    private final ClientContactRepository contactRepository;
    private final ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────
    // LEAD OPERATIONS
    // ─────────────────────────────────────────────────────────

    @Transactional
    public LeadResponse createLead(
            CreateLeadRequest request,
            UUID assignedToId) {

        String leadNumber = generateLeadNumber();

        Lead lead = Lead.builder()
                .leadNumber(leadNumber)
                .companyName(request.getCompanyName().trim())
                .industryType(request.getIndustryType())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .contactName(request.getContactName())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .contactWhatsapp(request.getContactWhatsapp())
                .contactDesignation(request.getContactDesignation())
                .source(request.getSource())
                .status(LeadStatus.NEW)
                .notes(request.getNotes())
                .assignedToId(assignedToId)
                .build();

        Lead saved = leadRepository.save(lead);

        log.info(
                "Lead created: {} — {}",
                saved.getLeadNumber(),
                saved.getCompanyName()
        );

        return mapLeadToResponse(saved);
    }

    @Transactional(readOnly = true)
    public LeadResponse getLeadById(UUID id) {
        return mapLeadToResponse(findLead(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<LeadResponse> searchLeads(
            String search,
            String status,
            UUID assignedToId,
            Pageable pageable) {

        Page<Lead> page = leadRepository.searchLeads(
                search,
                status,
                assignedToId != null
                        ? assignedToId.toString()
                        : null,
                pageable
        );

        return PageResponse.from(
                page.map(this::mapLeadToResponse)
        );
    }

    @Transactional
    public LeadResponse updateLead(
            UUID id,
            UpdateLeadRequest request) {

        Lead lead = findLead(id);

        if (lead.getStatus() == LeadStatus.CONVERTED) {
            throw new BusinessException(
                    "Cannot edit a converted lead",
                    "LEAD_ALREADY_CONVERTED"
            );
        }

        if (request.getCompanyName() != null)
            lead.setCompanyName(request.getCompanyName().trim());
        if (request.getIndustryType() != null)
            lead.setIndustryType(request.getIndustryType());
        if (request.getCity() != null)
            lead.setCity(request.getCity());
        if (request.getState() != null)
            lead.setState(request.getState());
        if (request.getCountry() != null)
            lead.setCountry(request.getCountry());
        if (request.getContactName() != null)
            lead.setContactName(request.getContactName());
        if (request.getContactEmail() != null)
            lead.setContactEmail(request.getContactEmail());
        if (request.getContactPhone() != null)
            lead.setContactPhone(request.getContactPhone());
        if (request.getContactWhatsapp() != null)
            lead.setContactWhatsapp(request.getContactWhatsapp());
        if (request.getContactDesignation() != null)
            lead.setContactDesignation(
                    request.getContactDesignation()
            );
        if (request.getSource() != null)
            lead.setSource(request.getSource());
        if (request.getStatus() != null)
            lead.setStatus(request.getStatus());
        if (request.getNotes() != null)
            lead.setNotes(request.getNotes());
        if (request.getLostReason() != null)
            lead.setLostReason(request.getLostReason());

        return mapLeadToResponse(leadRepository.save(lead));
    }

    /*
     * CONVERT LEAD
     *
     * This is the key marketing → PM handoff operation.
     *
     * Steps:
     * 1. Mark lead as CONVERTED
     * 2. Create or link client record
     * 3. Create ProjectInquiry
     *
     * The ProjectInquiry is then forwarded to PM
     * via a separate forwardInquiry() call.
     */
    @Transactional
    public InquiryResponse convertLead(
            UUID leadId,
            ConvertLeadRequest request) {

        Lead lead = findLead(leadId);

        if (lead.getStatus() == LeadStatus.CONVERTED) {
            throw new BusinessException(
                    "Lead is already converted",
                    "LEAD_ALREADY_CONVERTED"
            );
        }

        if (lead.getStatus() == LeadStatus.LOST) {
            throw new BusinessException(
                    "Cannot convert a lost lead",
                    "LEAD_IS_LOST"
            );
        }

        /*
         * Resolve client — use existing or create new.
         */
        UUID clientId;

        if (request.getExistingClientId() != null) {
            clientRepository
                    .findByIdAndIsDeletedFalse(
                            request.getExistingClientId()
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Client",
                                    request.getExistingClientId().toString()
                            )
                    );
            clientId = request.getExistingClientId();
        } else {
            /*
             * Create new client from lead data.
             * Company name comes from the lead.
             */
            Client newClient = Client.builder()
                    .companyCode(generateClientCode())
                    .companyName(lead.getCompanyName())
                    .email(lead.getContactEmail())
                    .phone(lead.getContactPhone())
                    .city(lead.getCity())
                    .state(lead.getState())
                    .country(lead.getCountry())
                    .industryType(lead.getIndustryType())
                    .isActive(true)
                    .build();

            Client savedClient = clientRepository.save(newClient);
            clientId = savedClient.getId();

            log.info(
                    "New client created from lead: {}",
                    savedClient.getCompanyCode()
            );
        }

        /*
         * Create the project inquiry.
         */
        CreateInquiryRequest inquiryReq = request.getInquiry();
        String inquiryNumber = generateInquiryNumber();

        String documentRefsJson = null;
        if (inquiryReq.getDocumentReferences() != null
                && !inquiryReq.getDocumentReferences().isEmpty()) {
            try {
                documentRefsJson = objectMapper.writeValueAsString(
                        inquiryReq.getDocumentReferences()
                );
            } catch (JsonProcessingException e) {
                log.warn(
                        "Could not serialize document references: {}",
                        e.getMessage()
                );
            }
        }

        ProjectInquiry inquiry = ProjectInquiry.builder()
                .inquiryNumber(inquiryNumber)
                .leadId(leadId)
                .clientId(clientId)
                .clientContactId(inquiryReq.getClientContactId())
                .projectName(inquiryReq.getProjectName().trim())
                .projectLocation(inquiryReq.getProjectLocation())
                .description(inquiryReq.getDescription())
                .documentReferences(documentRefsJson)
                .status(InquiryStatus.NEW)
                .notes(inquiryReq.getNotes())
                .build();

        ProjectInquiry savedInquiry =
                inquiryRepository.save(inquiry);

        /*
         * Mark lead as converted.
         */
        lead.setStatus(LeadStatus.CONVERTED);
        lead.setClientId(clientId);
        lead.setConvertedAt(Instant.now());
        leadRepository.save(lead);

        log.info(
                "Lead converted: {} → Inquiry: {}",
                lead.getLeadNumber(),
                savedInquiry.getInquiryNumber()
        );

        return mapInquiryToResponse(savedInquiry);
    }

    // ─────────────────────────────────────────────────────────
    // INQUIRY OPERATIONS
    // ─────────────────────────────────────────────────────────

    /*
     * Create a direct inquiry (not from a lead).
     * Used when an existing client calls directly
     * with a new project — no lead pipeline needed.
     */
    @Transactional
    public InquiryResponse createDirectInquiry(
            CreateInquiryRequest request) {

        if (request.getClientId() == null) {
            throw new BusinessException(
                    "Client ID is required for direct inquiry",
                    "CLIENT_REQUIRED"
            );
        }

        clientRepository
                .findByIdAndIsDeletedFalse(request.getClientId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Client",
                                request.getClientId().toString()
                        )
                );

        String documentRefsJson = null;
        if (request.getDocumentReferences() != null
                && !request.getDocumentReferences().isEmpty()) {
            try {
                documentRefsJson = objectMapper.writeValueAsString(
                        request.getDocumentReferences()
                );
            } catch (JsonProcessingException e) {
                log.warn(
                        "Could not serialize document references: {}",
                        e.getMessage()
                );
            }
        }

        ProjectInquiry inquiry = ProjectInquiry.builder()
                .inquiryNumber(generateInquiryNumber())
                .clientId(request.getClientId())
                .clientContactId(request.getClientContactId())
                .projectName(request.getProjectName().trim())
                .projectLocation(request.getProjectLocation())
                .description(request.getDescription())
                .documentReferences(documentRefsJson)
                .status(InquiryStatus.NEW)
                .notes(request.getNotes())
                .build();

        ProjectInquiry saved = inquiryRepository.save(inquiry);

        log.info(
                "Direct inquiry created: {}",
                saved.getInquiryNumber()
        );

        return mapInquiryToResponse(saved);
    }

    /*
     * FORWARD TO PM
     *
     * This is the marketing → PM handoff.
     * Marketing selects which PM to send to.
     * PM gets notified (notification module — future).
     * Inquiry status → FORWARDED.
     */
    @Transactional
    public InquiryResponse forwardToPm(
            UUID inquiryId,
            ForwardInquiryRequest request,
            UUID forwardedById) {

        ProjectInquiry inquiry = findInquiry(inquiryId);

        if (inquiry.getStatus() != InquiryStatus.NEW) {
            throw new BusinessException(
                    "Inquiry has already been forwarded",
                    "INQUIRY_ALREADY_FORWARDED"
            );
        }

        /*
         * Validate PM exists and has PM role.
         */
        var pm = employeeRepository
                .findByIdAndIsDeletedFalse(request.getForwardedToId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee",
                                request.getForwardedToId().toString()
                        )
                );

        if (pm.getRole() != com.thewizecompany
                .wizevision.employee.domain.Role.PROJECT_MANAGER) {
            throw new BusinessException(
                    "Selected employee is not a Project Manager",
                    "NOT_A_PROJECT_MANAGER"
            );
        }

        inquiry.setForwardedToId(request.getForwardedToId());
        inquiry.setForwardedById(forwardedById);
        inquiry.setForwardedAt(Instant.now());
        inquiry.setForwardingNotes(request.getNotes());
        inquiry.setStatus(InquiryStatus.FORWARDED);

        ProjectInquiry saved = inquiryRepository.save(inquiry);

        log.info(
                "Inquiry {} forwarded to PM: {}",
                inquiry.getInquiryNumber(),
                pm.getFullName()
        );

        /*
         * TODO: Send notification to PM
         * Will be implemented in notification module.
         */

        return mapInquiryToResponse(saved);
    }

    @Transactional(readOnly = true)
    public InquiryResponse getInquiryById(UUID id) {
        return mapInquiryToResponse(findInquiry(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<InquiryResponse> searchInquiries(
            String search,
            String status,
            UUID forwardedToId,
            Pageable pageable) {

        Page<ProjectInquiry> page =
                inquiryRepository.searchInquiries(
                        search,
                        status,
                        forwardedToId != null
                                ? forwardedToId.toString()
                                : null,
                        pageable
                );

        return PageResponse.from(
                page.map(this::mapInquiryToResponse)
        );
    }

    /*
     * PM's inbox — inquiries waiting for a bid.
     */
    @Transactional(readOnly = true)
    public List<InquiryResponse> getMyInquiries(
            UUID pmId) {

        return inquiryRepository
                .findByForwardedToIdAndStatusAndIsDeletedFalse(
                        pmId,
                        InquiryStatus.FORWARDED
                )
                .stream()
                .map(this::mapInquiryToResponse)
                .toList();
    }

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────

    private Lead findLead(UUID id) {
        return leadRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Lead", id.toString()
                        )
                );
    }

    private ProjectInquiry findInquiry(UUID id) {
        return inquiryRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "ProjectInquiry", id.toString()
                        )
                );
    }

    private String generateLeadNumber() {
        int year = Year.now().getValue();
        long count = leadRepository.countByIsDeletedFalse();
        return "LEAD-" + year + "-"
                + String.format("%03d", count + 1);
    }

    private String generateInquiryNumber() {
        int year = Year.now().getValue();
        long count = inquiryRepository.countByIsDeletedFalse();
        return "INQ-" + year + "-"
                + String.format("%03d", count + 1);
    }

    private String generateClientCode() {
        long count = clientRepository.countByIsDeletedFalse();
        return "WC-" + String.format("%03d", count + 1);
    }

    private LeadResponse mapLeadToResponse(Lead lead) {
        String assignedToName = employeeRepository
                .findByIdAndIsDeletedFalse(lead.getAssignedToId())
                .map(Employee::getFullName)
                .orElse(null);

        String clientName = null;
        if (lead.getClientId() != null) {
            clientName = clientRepository
                    .findByIdAndIsDeletedFalse(lead.getClientId())
                    .map(Client::getCompanyName)
                    .orElse(null);
        }

        return LeadResponse.builder()
                .id(lead.getId())
                .leadNumber(lead.getLeadNumber())
                .companyName(lead.getCompanyName())
                .industryType(lead.getIndustryType())
                .city(lead.getCity())
                .state(lead.getState())
                .country(lead.getCountry())
                .contactName(lead.getContactName())
                .contactEmail(lead.getContactEmail())
                .contactPhone(lead.getContactPhone())
                .contactWhatsapp(lead.getContactWhatsapp())
                .contactDesignation(lead.getContactDesignation())
                .source(lead.getSource())
                .status(lead.getStatus())
                .notes(lead.getNotes())
                .lostReason(lead.getLostReason())
                .assignedToId(lead.getAssignedToId())
                .assignedToName(assignedToName)
                .clientId(lead.getClientId())
                .clientName(clientName)
                .convertedAt(lead.getConvertedAt())
                .createdAt(lead.getCreatedAt())
                .createdBy(lead.getCreatedBy())
                .build();
    }

    private InquiryResponse mapInquiryToResponse(
            ProjectInquiry inquiry) {

        String clientName = null;
        String clientCode = null;
        if (inquiry.getClientId() != null) {
            var client = clientRepository
                    .findByIdAndIsDeletedFalse(
                            inquiry.getClientId()
                    );
            clientName = client
                    .map(Client::getCompanyName)
                    .orElse(null);
            clientCode = client
                    .map(Client::getCompanyCode)
                    .orElse(null);
        }

        String clientContactName = null;
        if (inquiry.getClientContactId() != null) {
            clientContactName = contactRepository
                    .findByIdAndIsDeletedFalse(
                            inquiry.getClientContactId()
                    )
                    .map(ClientContact::getFullName)
                    .orElse(null);
        }

        String forwardedToName = null;
        if (inquiry.getForwardedToId() != null) {
            forwardedToName = employeeRepository
                    .findByIdAndIsDeletedFalse(
                            inquiry.getForwardedToId()
                    )
                    .map(Employee::getFullName)
                    .orElse(null);
        }

        String forwardedByName = null;
        if (inquiry.getForwardedById() != null) {
            forwardedByName = employeeRepository
                    .findByIdAndIsDeletedFalse(
                            inquiry.getForwardedById()
                    )
                    .map(Employee::getFullName)
                    .orElse(null);
        }

        String leadCompanyName = null;
        if (inquiry.getLeadId() != null) {
            leadCompanyName = leadRepository
                    .findByIdAndIsDeletedFalse(
                            inquiry.getLeadId()
                    )
                    .map(Lead::getCompanyName)
                    .orElse(null);
        }

        return InquiryResponse.builder()
                .id(inquiry.getId())
                .inquiryNumber(inquiry.getInquiryNumber())
                .leadId(inquiry.getLeadId())
                .leadCompanyName(leadCompanyName)
                .clientId(inquiry.getClientId())
                .clientName(clientName)
                .clientCode(clientCode)
                .clientContactId(inquiry.getClientContactId())
                .clientContactName(clientContactName)
                .projectName(inquiry.getProjectName())
                .projectLocation(inquiry.getProjectLocation())
                .description(inquiry.getDescription())
                .documentReferences(inquiry.getDocumentReferences())
                .status(inquiry.getStatus())
                .forwardedToId(inquiry.getForwardedToId())
                .forwardedToName(forwardedToName)
                .forwardedById(inquiry.getForwardedById())
                .forwardedByName(forwardedByName)
                .forwardedAt(inquiry.getForwardedAt())
                .forwardingNotes(inquiry.getForwardingNotes())
                .bidId(inquiry.getBidId())
                .notes(inquiry.getNotes())
                .createdAt(inquiry.getCreatedAt())
                .createdBy(inquiry.getCreatedBy())
                .build();
    }
}