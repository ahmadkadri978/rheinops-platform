# Runbook: active support SLA breach

## Trigger and impact

Use this runbook for `RheinOpsSupportSlaBreached`. At least one active support case has persisted `BREACHED` escalation and requires operational ownership; this alert does not imply platform downtime.

## Diagnose

1. Open the support queue filtered by `escalationLevel=BREACHED` with an authorised account.
2. Confirm response and resolution deadlines, priority, current owner/process, and linked incident.
3. Read only the minimum diagnostic data required; do not export requester contact data into monitoring systems.
4. Check whether scheduled SLA evaluation is running and whether server clock/database time is healthy.

## Respond

- Assign the case through the team process and contact the requester using the approved support channel.
- Link or create an incident when multiple customers or a service degradation are involved.
- Follow the attached HTTPS runbook and record resolution notes before resolving the case.
- Do not clear the metric manually. It reflects persisted domain state and resolves after the case leaves the active breached queue.

## Verify and close

Confirm there are no unowned breached cases, required customer communication occurred, and the metric/alert reflects the database state after its 15-second refresh interval. Review policy targets if repeated breaches share the same cause.
