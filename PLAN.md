# Implementation Plan

| # | Task | Status | Notes |
|---|------|--------|-------|
| 1 | Align persistence models/mappers (e.g., SMS/email records, QR codes) with the Go storage layer so support services have the same data contract. | Done | Added email-aware SMS records, fixed QR-code persistence, and filled in missing error codes. |
| 2 | Port all login & authentication flows (account/password login, registration, SMS/email send & login, QR-code login) to mirror `apis/login.go` and related Go services. | In Review | Implemented account/phone/email login, registration, code delivery/validation, and QR login backed by the same persistence as Go. |
| 3 | Complete user profile/settings/binding/lookup flows so they match Go logic, including masking fields, IM sync, and verification paths. | Done | Implemented keyword-search pagination, contact masking, and IM-setting sync hooks alongside the binding flows. |
| 4 | Finish friend relationship/application flows with the same validation, deduplication, and notification hooks as the Go implementation. | Done | Added strict target validation, bidirectional persistence, and application pagination; notification delivery will piggyback on the IM SDK work in Task 5. |
| 5 | Finish group lifecycle & management flows (creation, membership changes, management settings, invitations/applications, announcements) and wire them to IM operations. | In Progress | Ported group/member/application endpoints and management settings; IM SDK calls remain TODO until the dependency ships. |
| 6 | Implement message recall/deletion and translation/sync stubs so they enforce the same permissions and call the IM SDK just like the Go code. | In Progress | Added the same validation & permission checks for message recall/delete; IM calls remain blocked by the missing SDK. |
| 7 | Port the assistant prompts, file credentials, Telegram bot hooks, and post/feed/comment/reaction flows from the Go services. | In Progress | Controllers now delegate to their services, file/bot credentials follow the Go logic, and posts reuse the new feed/comment/reaction layers; outbound notifications still wait on the IM SDK. |

## Issues & Notes
- SMS/Email delivery engines from the Go stack are not available in this environment, so the verification service persists codes and validates them but does not actually send outbound messages.
- The IM SDK artifact (`com.juggle.im:imserver-sdk-java:1.1`) is still missing, so message recall/deletion only enforce business-side permission checks for now.
