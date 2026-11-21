# Incident API

Base path `/api/incidents`. Requests with a body use `Content-Type: application/json`.

| Method and path | Request | Success response |
| --- | --- | --- |
| `POST /api/incidents` | Incident fields shown below | `200` with the created incident, including its calculated priority and `NEW` status |
| `GET /api/incidents?page=0&size=20` | Zero-based page and page size. Defaults to newest ID first. | `200` with a `content` array and `page` metadata (`number`, `size`, `totalElements`, `totalPages`) |
| `GET /api/incidents/{id}` | Incident ID | `200` with the incident |
| `PUT /api/incidents/{id}/assignment` | `{"technicianId": 1}` | `200` with the incident in `ASSIGNED` status |
| `PATCH /api/incidents/{id}/status` | `{"status": "CLASSIFIED"}` | `200` with the updated incident |

## Create an incident

```json
{
  "title": "Campus Wi-Fi unavailable",
  "description": "Devices cannot connect in the library.",
  "category": "NETWORK",
  "impact": "HIGH",
  "urgency": "HIGH",
  "reporterId": 1,
  "serviceIds": [1]
}
```

Use IDs that exist in the development database. All fields are required, and `serviceIds` must contain at least one ID. Impact and urgency accept `LOW`, `MEDIUM`, or `HIGH`. Categories are `HARDWARE`, `SOFTWARE`, `NETWORK`, `DATABASE`, `CLOUD`, `SECURITY`, `AUTHENTICATION`, `APPLICATION`, and `STORAGE`.

Incident responses include `id`, `title`, `description`, `category`, `impact`, `urgency`, `priority`, `status`, `createdAt`, `updatedAt`, `reporter`, `assignedTechnician`, and `affectedServices`. `assignedTechnician` is null before assignment.

## Move an incident through the workflow

`NEW → CLASSIFIED → ASSIGNED → ACKNOWLEDGED → IN_PROGRESS → RESOLVED → VERIFIED → CLOSED`

Use the assignment endpoint to move a classified incident to `ASSIGNED`. The technician must be available. Use the status endpoint for every other transition.

The API returns `400` for invalid request fields, `404` for missing records, and `409` for unavailable technicians or invalid workflow transitions.

Use this contract for development integration. Authentication and role checks are pending. The create request currently supplies `reporterId`; after sign-in is integrated, the backend will obtain it from the signed-in user.
