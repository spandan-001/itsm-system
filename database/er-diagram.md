# I-ITSM Entity-Relationship Diagram

Matches the JPA entities and `schema.sql` + `account-links.sql` + `backend-additions.sql`.

```mermaid
erDiagram
    employees ||--o{ incidents : "reports (reporter_id)"
    technicians ||--o{ incidents : "is assigned (assigned_technician_id)"
    incidents ||--o{ incidents : "duplicate of (duplicate_of_id)"
    incidents ||--o{ incident_services : "affects"
    it_services ||--o{ incident_services : "is affected by"
    technicians ||--o{ technician_skills : "has"
    incidents ||--o{ incident_notes : "has thread"
    incidents ||--o{ audit_logs : "has history"
    employees |o--o| account_links : "signs in as"
    technicians |o--o| account_links : "signs in as"

    employees {
        bigint id PK
        varchar name
        varchar email UK
        varchar department
    }

    technicians {
        bigint id PK
        varchar name
        varchar email UK
        varchar domain
        boolean available
    }

    technician_skills {
        bigint technician_id PK,FK
        varchar skill PK "IncidentCategory"
    }

    account_links {
        uuid supabase_user_id PK "Supabase Auth user"
        bigint employee_id FK,UK "exactly one of"
        bigint technician_id FK,UK "employee or technician"
    }

    it_services {
        bigint id PK
        varchar name UK
        text description
        varchar criticality "LOW | MEDIUM | HIGH"
        boolean active
    }

    incidents {
        bigint id PK
        varchar title
        text description
        varchar category
        varchar impact
        varchar urgency
        varchar priority "P1-P4"
        varchar status "lifecycle state"
        timestamp created_at
        timestamp updated_at
        bigint reporter_id FK
        bigint assigned_technician_id FK
        bigint duplicate_of_id FK
        double duplicate_score
        timestamp response_due_at
        timestamp resolution_due_at
        timestamp responded_at
        timestamp resolved_at
        boolean response_warned
        boolean resolution_warned
        boolean response_breached
        boolean resolution_breached
        boolean awaiting_reporter_info
        varchar resolution_summary
        varchar resolved_by
        timestamp closed_at
        int feedback_rating "1-5"
        varchar feedback_comment
        timestamp feedback_at
    }

    incident_services {
        bigint incident_id PK,FK
        bigint service_id PK,FK
    }

    incident_notes {
        bigint id PK
        bigint incident_id FK
        varchar type "INFO_REQUEST, INVESTIGATION, ..."
        varchar author_name
        varchar author_email
        varchar author_role
        varchar body
        timestamp created_at
    }

    audit_logs {
        bigint id PK
        bigint incident_id FK
        varchar action
        varchar from_status
        varchar to_status
        varchar details
        varchar actor
        timestamp occurred_at
    }

    sla_policies {
        bigint id PK
        varchar priority UK
        bigint response_minutes
        bigint resolution_minutes
    }

    notifications {
        bigint id PK
        varchar recipient_name
        varchar recipient_email
        varchar channel "IN_APP | EMAIL"
        varchar subject
        varchar message
        bigint incident_id "not a FK: kept if the incident is deleted"
        timestamp created_at
        boolean is_read
    }
```

## Important relationships

- **Inheritance:** `User` is a JPA `@MappedSuperclass`, so `employees` and `technicians` are separate tables with the same `name`/`email` columns.
- **Incident is the aggregate root.** Notes and audit entries belong to one incident and are deleted with it (`ON DELETE CASCADE`).
- **Many-to-many:** an incident affects one or more IT services through `incident_services`.
- **Self-reference:** a duplicate points at the original incident (`duplicate_of_id`), always the root, never another duplicate.
- **Sign-in:** `account_links` maps a Supabase (Google) user to exactly one employee or technician. Administrators are configured by user id (`ITSM_ADMIN_USER_IDS`) and have no row.
- `sla_policies` is keyed by priority; the SLA deadlines are copied onto each incident when it is created, so changing a policy affects new incidents only.
