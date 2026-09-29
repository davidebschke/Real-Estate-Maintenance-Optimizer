# Error Handling & Robustness

Binding error-handling and robustness conventions for the Real-Estate-Maintenance-Optimizer repository. Referenced from `CLAUDE.md`, Section 10.

## Fail Fast vs. Graceful Degradation

- **Fail fast**: invalid state must stop the request, not continue silently. Missing required configuration or a required dependency fails at application startup, not via a silent fallback default. Invalid input is rejected at the boundary (the `@RestController`, Bean Validation) with a clear 4xx, not "corrected" silently.
- **Graceful degradation** is acceptable only for genuinely optional features — e.g. an appointment map marker falling back from a property's stored coordinates to the `GET /api/geocode` proxy only when those are missing (`frontend/readme_frontend_en.md`, "Daily appointment map"). Ask: *if this fails, is the response the caller receives still correct?* If not, propagate the error instead of logging and continuing.
- A deliberate degradation must be documented in the enclosing method's JavaDoc/TSDoc (per the comment rule in `coding-conventions.md` — no inline comments) and must log the failure via `slf4j`/the frontend's existing error handling, not fail silently. `GeocodingService.fetchRaw` is the reference example: its JavaDoc already states it "returns an empty list instead of throwing if the request fails", and the `catch` block logs the failure via `log.warn` before returning the safe fallback.

## External Calls

- Every outgoing HTTP call must have a timeout; Spring's default `RestClient`/`RestTemplate` request factories have none. Configure the timeout once on the shared `RestClient.Builder` bean (see `config/RestClientConfig.java`), never per call site.
- A failure from an external call is caught at the service boundary (see `GeocodingService`) and turned into a domain exception or an explicit fallback — never left to propagate as a raw `RestClientException`/Axios error to the controller or component.

## Exceptions (Backend)

- Exceptions are mapped to HTTP responses exclusively through `GlobalExceptionHandler`; controllers and services throw domain exceptions from `exception/`, they don't build error `ResponseEntity`s inline.
- A caught exception is either rethrown (optionally wrapped with added context) or handled with a logged, deliberate fallback per the section above — never swallowed by an empty `catch` block or a bare log line with no further action.
- When wrapping an exception, pass the original as the cause (`throw new XyzException("...", originalException)`) so the root cause stays visible in logs and stack traces.

## Resources

- Anything that opens a resource (HTTP response body, file handle, a JDBC connection acquired outside Spring-managed transactions) is closed via try-with-resources or an equivalent Spring-managed abstraction, never left to the garbage collector.

## Frontend (Axios/Pinia)

- A store action or service call that can fail must surface the failure to the calling component (a thrown/rejected error, or an explicit error state on the store) — not swallow it in a bare `.catch(console.error)`.
- `services/httpClient.ts` already centralizes session-expiry handling (a 401 outside the session-independent auth endpoints triggers `onSessionExpired`); extend that interceptor for new cross-cutting Axios error handling instead of duplicating ad hoc handling per call site.
