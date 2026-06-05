package com.thewizecompany.wizevision.marketing.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LeadSource {

    REFERRAL("Referral"),
    COLD_CALL("Cold Call"),
    WEBSITE("Website"),
    EXHIBITION("Exhibition / Trade Show"),
    LINKEDIN("LinkedIn"),
    INSTAGRAM("Instagram"),
    FACEBOOK("Facebook"),
    EMAIL_CAMPAIGN("Email Campaign"),
    EXISTING_CLIENT("Existing Client Referral"),
    OTHER("Other");

    private final String displayName;
}