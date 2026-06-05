package com.thewizecompany.wizevision.client.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ClientContactResponse {

    private final UUID id;
    private final UUID clientId;
    private final String clientName;
    private final String firstName;
    private final String lastName;
    private final String fullName;
    private final String designation;
    private final String email;
    private final String phone;
    private final String whatsapp;
    private final String notes;
    private final boolean isPrimary;
    private final boolean isActive;
    private final Instant createdAt;
}