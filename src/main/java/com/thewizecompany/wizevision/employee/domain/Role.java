package com.thewizecompany.wizevision.employee.domain;

/*
 * SYSTEM ROLES
 *
 * Every employee has exactly ONE role.
 * The role determines what they can see and do.
 *
 * HIERARCHY (highest to lowest access):
 *
 * SUPER_ADMIN
 *   └── Full system access including system configuration
 *       Only for the system owner / IT head
 *
 * ADMIN
 *   └── Full business access
 *       Finalizes bids, manages billing, manages users
 *       Cannot touch system configuration
 *
 * PROJECT_MANAGER
 *   └── Creates bid proposals, manages projects
 *       Assigns team members, tracks progress
 *       Cannot see bid amounts until admin sets them
 *
 * MARKETING_EXECUTIVE
 *   └── Manages leads and clients
 *       Forwards projects to PM for bidding
 *       Cannot see financial data
 *
 * HR_MANAGER
 *   └── Employee management, payroll, leave approval
 *       Onboarding, policies, documents
 *       Cannot see project/bid financials
 *
 * IT_ADMIN
 *   └── Device management, system monitoring
 *       User account issues, technical support
 *       Cannot see business financials
 *
 * FINANCE
 *   └── Invoices, payments, billing reports
 *       Cannot manage employees or projects directly
 *
 * CHECKER
 *   └── Steel detailing checker
 *       Reviews and approves work on assigned projects
 *       Own attendance, leave, profile
 *
 * EDITOR
 *   └── Steel detailing editor
 *       Updates task status on assigned projects
 *       Own attendance, leave, profile
 *
 * MODELER
 *   └── Steel detailing modeler
 *       Updates task status on assigned projects
 *       Own attendance, leave, profile
 *
 * EMPLOYEE
 *   └── Base role for new joiners
 *       Own profile, attendance, leave requests only
 *       Upgraded to specific role after onboarding
 */
public enum Role {

    SUPER_ADMIN,
    ADMIN,
    PROJECT_MANAGER,
    MARKETING_EXECUTIVE,
    HR_MANAGER,
    IT_ADMIN,
    FINANCE,
    CHECKER,
    EDITOR,
    MODELER,
    EMPLOYEE
}