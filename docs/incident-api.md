# Incident API

Base path `/api/incidents`. Requests with a body use `Content-Type: application/json`.

## Verify the signed-in account

After Google sign-in through Supabase, send the Supabase session's access token
as `Authorization: Bearer <access_token>` to `GET /api/me`. A linked account
gets `200` with `userId` (the Supabase user UUID), `email`, `role`
(`EMPLOYEE` or `TECHNICIAN`), and `accountId` (the ITSM record ID).
A missing, expired, or invalid token returns `401`. A valid token without
an ITSM account link returns `403`.

Spring defaults to the ITSM development Supabase issuer. For another Supabase
project using ES256 signing keys, set `SUPABASE_AUTH_ISSUER` to its project URL
followed by `/auth/v1`. To provision accounts on PostgreSQL, apply
`database/account-links.sql`, then link each approved Supabase user UUID to
an existing `employee_id` or `technician_id` in `account_links`. Roles are
assigned through these links. Incident creation requires the employee role.

| Method and path | Request | Success response |
| --- | --- | --- |
| `POST /api/incidents` | Employee bearer token and incident fields shown below | `200` with the created incident, including its calculated priority. It is classified and routed automatically, so it is usually `ASSIGNED` (or `QUEUED`, or `NEW` with `duplicateOfId` if it was detected as a duplicate) |
| `GET /api/services` | Linked account bearer token | `200` with the active services for the create form |
| `GET /api/incidents?page=0&size=20` | Zero-based page and page size. Defaults to newest ID first. | `200` with a `content` array and `page` metadata (`number`, `size`, `totalElements`, `totalPages`) |
| `GET /api/incidents/{id}` | Incident ID | `200` with the incident |
| `PUT /api/incidents/{id}/assignment` | Administrator bearer token and `{"technicianId": 1}` | `200` with the incident in `ASSIGNED` status |
| `PATCH /api/incidents/{id}/status` | Linked account bearer token and a status such as `{"status": "ACKNOWLEDGED"}` | `200` with the updated incident |

## Create an incident

Send the Supabase access token in `Authorization: Bearer <access_token>`.
Spring takes the reporter from the linked employee account. Missing or invalid
tokens return `401`. Unlinked accounts and technicians return `403`.

```json
{
  "title": "Campus Wi-Fi unavailable",
  "description": "Devices cannot connect in the library.",
  "category": "NETWORK",
  "impact": "HIGH",
  "urgency": "HIGH",
  "serviceIds": [1]
}
```

Use IDs that exist in the development database. All fields are required, and `serviceIds` must contain at least one ID. Impact and urgency accept `LOW`, `MEDIUM`, or `HIGH`. Categories are `HARDWARE`, `SOFTWARE`, `NETWORK`, `DATABASE`, `CLOUD`, `SECURITY`, `AUTHENTICATION`, `APPLICATION`, and `STORAGE`.

Incident responses include `id`, `title`, `description`, `category`, `impact`, `urgency`, `priority`, `status`, `createdAt`, `updatedAt`, `reporter`, `assignedTechnician`, and `affectedServices`. `assignedTechnician` is null before assignment.

## Move an incident through the workflow

`NEW → CLASSIFIED → (QUEUED →) ASSIGNED → ACKNOWLEDGED → IN_PROGRESS → RESOLVED → VERIFIED → CLOSED`, plus `RESOLVED → IN_PROGRESS` to reopen.

New incidents are classified and routed automatically (see [backend-design.md](backend-design.md)). An administrator can manually assign or reassign a `CLASSIFIED`, `QUEUED`, or `ASSIGNED` incident to an available technician.

Employees see incidents they reported. Their available status actions are `VERIFIED` and `CLOSED`. Technicians see incidents assigned to them. Their available status actions are `ACKNOWLEDGED`, `IN_PROGRESS`, and `RESOLVED`. Intersect these actions with the incident's `allowedTransitions` when displaying buttons. The same incident access rules apply to search, audit, and SLA details.

The API returns `400` for invalid request fields, `404` for missing records, and `409` for unavailable technicians or invalid workflow transitions.

Send the bearer token with API requests. A valid token with insufficient privileges returns `403`. An incident outside the linked account's scope returns `404`.

Configure administrative access with `ITSM_ADMIN_USER_IDS`, a comma-separated
list of approved Supabase user UUIDs. The default list is empty. Administration
covers manual routing, global audit, analytics, SLA policy updates, strategy
selection, technician management, and duplicate-management endpoints.
