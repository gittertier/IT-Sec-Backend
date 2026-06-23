# Termin- & Praxis-API (für Frontend)

Basis-URL: `/api/v1`
Alle Datumswerte: ISO-8601 ohne Zeitzone, z.B. `2026-06-23T09:00:00`.

---

## Authentifizierung & CSRF

- **Login:** `POST /api/v1/public/login` mit JSON `{ "username": "...", "password": "..." }`
  → bei Erfolg `200 {"message":"login successful"}` und ein Session-Cookie (`SESSION`).
- **Logout:** `POST /api/v1/public/logout` → `204`.
- Alle anderen Endpunkte erfordern eine **eingeloggte Session** (Cookie mitsenden).
- **CSRF:** Bei jedem ändernden Request (`POST`) muss der CSRF-Token gesendet werden:
  Cookie `XSRF-TOKEN` auslesen → als Header `X-XSRF-TOKEN` mitschicken.
  (Ausnahme: der Login selbst.)

---

## Praxen

### Praxisliste / Filter
`GET /api/v1/praxen`

Optionale Query-Parameter:
| Param | Typ | Beschreibung |
|---|---|---|
| `postalCode` | String | exakte PLZ (5-stellig) |
| `name` | String | Teiltext im Namen (case-insensitive) |

→ `200` Liste von **PraxisDto**.

### Praxis anlegen  *(nur ADMIN/STAFF)*
`POST /api/v1/praxen`
Body: **PraxisCreateRequestDto** → `201` PraxisDto.

---

## Termine

### Freie Slots
`GET /api/v1/termine/free`

Optionale Query-Parameter:
| Param | Typ | Beschreibung |
|---|---|---|
| `plz` | String | PLZ der Praxis |
| `praxisId` | UUID | bestimmte Praxis |
| `from` | DateTime | frühester Start; um weiter in die Zukunft zu suchen. Vergangene Werte werden auf „jetzt" geklemmt |

→ `200` Liste von **TerminDto** (nur freie, zukünftige Slots).
Beispiel: `GET /api/v1/termine/free?plz=12345&from=2026-09-01T00:00:00`

### Slots suchen / filtern
`GET /api/v1/termine/search`

Alle Query-Parameter optional:
| Param | Typ | Beschreibung |
|---|---|---|
| `praxisId` | UUID | bestimmte Praxis |
| `postalCode` | String | PLZ der Praxis |
| `status` | enum | `FREE` / `BOOKED` / `CANCELLED` |
| `from` | DateTime | frühester Start (inkl.) |
| `to` | DateTime | spätester Start (inkl.) |

→ `200` Liste von **TerminDto**. **Hinweis:** `note` ist hier immer `null` (Datenschutz — fremde Notizen werden nicht ausgegeben).

### Eigene Termine
`GET /api/v1/termine/mine`

Alle Query-Parameter optional (nur die eigenen Termine):
| Param | Typ | Beschreibung |
|---|---|---|
| `praxisId` | UUID | bestimmte Praxis |
| `status` | enum | `FREE` / `BOOKED` / `CANCELLED` |
| `from` | DateTime | frühester Start (inkl.) |
| `to` | DateTime | spätester Start (inkl.) |

→ `200` Liste von **TerminDto** (mit `note`, da eigene Termine).
Beispiel „meine gebuchten Termine an einem Tag":
`GET /api/v1/termine/mine?status=BOOKED&from=2026-06-23T00:00:00&to=2026-06-23T23:59:59`

### Slot anlegen  *(nur ADMIN/STAFF)*
`POST /api/v1/termine`
Body: **TerminCreateRequestDto** → `201` TerminDto.

### Slot buchen
`POST /api/v1/termine/{slotId}/book`
Body (optional): **TerminBookingRequestDto** → `200` TerminDto.

### Buchung stornieren
`POST /api/v1/termine/{slotId}/cancel` → `204` (Slot wird wieder `FREE`).

---

## DTOs

### PraxisDto (Response)
```json
{ "id": "uuid", "name": "Demo Praxis", "address": "Musterstrasse 1, 12345 Musterstadt", "postalCode": "12345" }
```

### PraxisCreateRequestDto (Request)
```json
{ "name": "Praxis Dr. Müller", "address": "Hauptstr. 5, 10115 Berlin", "postalCode": "10115" }
```
- `name`: Pflicht, nicht leer
- `address`: Pflicht, nicht leer
- `postalCode`: Pflicht, **5 Ziffern** (`\d{5}`)

### TerminDto (Response)
```json
{
  "id": "uuid",
  "praxisId": "uuid",
  "praxisName": "Demo Praxis",
  "startTime": "2026-06-23T09:00:00",
  "endTime": "2026-06-23T09:30:00",
  "status": "FREE",
  "vaccine": "BioNTech",
  "note": null
}
```
- `vaccine`: welcher Impfstoff am Slot geimpft wird (Freitext, kann `null` sein).

### TerminCreateRequestDto (Request)
```json
{ "praxisId": "uuid", "startTime": "2026-06-23T09:00:00", "endTime": "2026-06-23T09:30:00", "vaccine": "BioNTech" }
```
- `praxisId`, `startTime`, `endTime`: Pflicht; `startTime` in der Zukunft und vor `endTime`.
- `vaccine`: optional.

### TerminBookingRequestDto (Request, optional)
```json
{ "note": "Bitte morgens" }
```
- `note`: optionaler Freitext (wird serverseitig verschlüsselt gespeichert).

---

## Statuswerte (enum `TerminStatus`)
`FREE` · `BOOKED` · `CANCELLED`

## Fehlerformat
- `400` – Validierungs-/Fachfehler, Body = Klartext-Meldung (z.B. `"postalCode must be a 5-digit German PLZ"`).
- `401` – nicht eingeloggt (`{"error":"Nicht authentifiziert"}`).
- `403` – eingeloggt, aber keine Berechtigung (z.B. kein ADMIN/STAFF beim Anlegen).
