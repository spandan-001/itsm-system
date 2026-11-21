# I-ITSM Initial Entity-Relationship Diagram

```mermaid
erDiagram
    employees ||--o{ incidents : "reporter_id (FK -> employees.id)"
    technicians ||--o{ incidents : "assigned_technician_id (FK -> technicians.id)"
    incidents ||--o{ incident_services : "incident_id (PK, FK -> incidents.id)"
    it_services ||--o{ incident_services : "service_id (PK, FK -> it_services.id)"

    employees {
        bigint id PK
        varchar name
        varchar email
        varchar password_hash
        varchar department
        timestamp created_at
    }

    technicians {
        bigint id PK
        varchar name
        varchar email
        varchar password_hash
        varchar domain
        boolean available
        timestamp created_at
    }

    it_services {
        bigint id PK
        varchar name
        text description
        varchar criticality
        boolean active
    }

    incidents {
        bigint id PK
        varchar title
        text description
        varchar category
        varchar impact
        varchar urgency
        varchar priority
        varchar status
        timestamp created_at
        timestamp updated_at
        bigint reporter_id FK
        bigint assigned_technician_id FK
    }

    incident_services {
        bigint incident_id PK,FK
        bigint service_id PK,FK
    }
    ```
