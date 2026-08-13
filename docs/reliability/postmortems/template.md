# Postmortem: <short incident title>

Status: draft | reviewed | closed  
Incident window (UTC): `<start>` – `<end>`  
Severity: `<level>`  
Authors/reviewers: `<roles or names>`

## Executive summary

In two or three sentences, state what users experienced, how long it lasted, and what restored service. Avoid blame and speculation.

## Impact

- affected user journeys and scope;
- duration and request/error counts supported by evidence;
- SLO and error-budget impact;
- data integrity, security, and support-case impact;
- explicitly state when an area was checked and not affected.

## Detection

Describe the first signal, the detection delay, the alert/runbook used, and whether a user reported the problem first. Include dashboard, correlation ID, and trace ID references without tokens or personal data.

## Timeline (UTC)

| Time | Event, decision, or evidence |
|---|---|
| `<hh:mm>` | `<what happened>` |

## Root cause and contributing factors

Separate the technical root cause from conditions that increased impact. Explain why existing controls did not prevent or contain it. Do not use “human error” as a root cause.

## Resolution and recovery

Describe the actions that stopped impact, how recovery was verified, and any temporary mitigation still in place.

## Five whys

1. Why did users see the failure? `<answer>`
2. Why did that condition occur? `<answer>`
3. Why was it not contained? `<answer>`
4. Why was it not detected or recovered sooner? `<answer>`
5. What systemic condition allowed it? `<answer>`

## What went well

- `<effective signal, automation, collaboration, or design>`

## What did not go well

- `<gap stated without blame>`

## Where we got lucky

- `<factor that limited impact but was not a reliable control>`

## Corrective actions

| Action | Type | Owner | Due | Verification | Status |
|---|---|---|---|---|---|
| `<specific change>` | prevent / detect / mitigate / learn | `<owner>` | `<date>` | `<test or metric>` | planned |

Actions must be specific, prioritised, and verifiable. Close the postmortem only when accepted actions have owners and follow-up tracking.

## Lessons and follow-up

State what changed in the architecture, runbook, alerting, test, or team assumption. Link evidence and record rejected alternatives with rationale.
