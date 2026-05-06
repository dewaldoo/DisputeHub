package com.disputehub.api.entity;

/**
 * Dispute lifecycle statuses.
 * Typical flow: PENDING → UNDER_REVIEW → MERCHANT_CONTACTED → RESOLVED or REJECTED
 */
public enum DisputeStatus {
    PENDING,
    UNDER_REVIEW,
    MERCHANT_CONTACTED,
    RESOLVED,
    REJECTED
}
