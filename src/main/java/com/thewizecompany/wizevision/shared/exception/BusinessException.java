package com.thewizecompany.wizevision.shared.exception;

import lombok.Getter;

/*
 * Thrown when a business rule is violated.
 *
 * More specific than a generic RuntimeException —
 * carries an errorCode that the frontend can use
 * to display specific user-friendly messages.
 *
 * Usage in service:
 *   if (bid.getStatus() != BidStatus.DRAFT) {
 *       throw new BusinessException(
 *           "Only draft bids can be submitted",
 *           "BID_INVALID_STATUS"
 *       );
 *   }
 *
 * Results in:
 * HTTP 422 Unprocessable Entity
 * {
 *   "success": false,
 *   "message": "Only draft bids can be submitted",
 *   "errorCode": "BID_INVALID_STATUS"
 * }
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String errorCode;

    public BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

}