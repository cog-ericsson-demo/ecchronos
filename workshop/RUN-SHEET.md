# Run sheet: Ericsson security workshop

Wednesday 11 November, 13:00 London / 14:00 CET, 90 minutes. Everything below was prepared on 10 and 11 November on `cog-ericsson-demo/ecchronos` (branch `workshop/6.1.4`, release 6.1.4) and `cog-ericsson-demo/ecaudit` (`master`). Nothing has been merged.

Status at 12:46 London on 11 November. One thing is still not working and is marked as such below: the Automation opening a PR by itself (retested at 12:44, still HTTP 403 on git). Added since 12:25: the scan CSV import, the live scan on `ecaudit-workshop-2`, a chained-vulnerability example and a daily scan schedule.

## 1. Presenter actions

| Deadline | Action |
|---|---|
| Optional | Let the Automation's sessions push. Retested at 12:44 with a [diagnostic run](https://ericssondemo.devinenterprise.com/sessions/ef0057725cc946c8a4316e7554fdf386): `git ls-remote`, `git fetch` and a dry-run push all still get HTTP 403, with `git-manager.devin.ai` on the session's network allowlist. The cause is not confirmed. A session cannot read or change security profiles; check [Settings > Security profiles](https://ericssondemo.devinenterprise.com/settings/devin#security-profile-automations-default) and the Automation's own security profile and network policy. If it is skipped, the demo is unchanged: fire only the `medium` payload live. |
| Before 12:30 | Archive the first Automation run from 10 November: [Fix CVE-2026-50010 in ecchronos](https://ericssondemo.devinenterprise.com/sessions/d5c99671073f41a8b10ef9d9d1ccd81b). It belongs to the org service user, so the preparation session is not allowed to archive it. It was superseded by the run in tab 9; do not open it on screen. |
| 12:30 (T-30) | Open the tabs in section 2, in order. Check that each PR still shows "Open". |
| Done at 12:44 | The live scan on `cog-ericsson-demo/ecaudit-workshop-2` (a plain copy of ecaudit `master`, never scanned before) was started in interactive mode at Deep effort: [scan](https://ericssondemo.devinenterprise.com/code-scan/cff2224221b9496eac580618b31c4f8e), [session](https://ericssondemo.devinenterprise.com/sessions/d57aa308fdfc40d0b4d5993979018e84). It pauses at its scan model. Do not reply to it before the call. |
| Area 1, live | Start a new session and paste the batch prompt from section 2 (tab 3). The audience only needs to see ten sessions start, each on its own machine. Results are in tab 4. |
| Area 2, live | Fire the Automation with the `medium` payload to show the gate turning it away, then open tabs 8 and 9. See "Firing the Automation" below. |
| Area 3, live | Open the paused scan's session. It shows its scan model and asks whether to go ahead or change it. Type the suggested answers below as the reply, with the audience. If it is not paused, use the paused rehearsal scan on `ecaudit-workshop` (section 4) or show the finished ecaudit scan (tab 10) and say the answers aloud. |
| Optional | Scan CSV import into Devin Security. Started at 12:43 with the ingest profile "Scanner CSV import - Trivy, Grype, PMD" on `ecchronos` at the `workshop/6.1.4` commit, Normal effort: [import scan](https://ericssondemo.devinenterprise.com/code-scan/d9724c1b37d7428d971141273d3f8daa). It may still be running during the call; show it only if it has findings, otherwise the triage session (tab 6) tells the same story. |
| Not needed | Changing the default branch of `cog-ericsson-demo/ecchronos`. The existing ecchronos scan is pinned to the 6.1.4 commit, and the live scan runs on the ecaudit copy. |

### Firing the Automation

The Automation takes a webhook with a JSON body. Its gate reads `triaged_bucket` and starts a session only for `critical`. It runs one session at a time.

- The webhook needs a secret in the `X-Webhook-Secret` header. It is stored on the preparation session's machine and is deliberately not written in this file.
- To fire it live, message the preparation session "fire the medium payload". It sends the request below, and the event then shows in the Automation's run history as skipped.
- The request, for reference:

```bash
curl -X POST '<webhook URL shown on the Automation page>' \
  -H 'Content-Type: application/json' -H 'X-Webhook-Secret: <secret>' \
  -d '{"finding_id":"CVE-2026-24880","triaged_bucket":"medium","package":"org.apache.tomcat.embed:tomcat-embed-core","installed_version":"10.1.48","fixed_version":"10.1.53","evidence":"Relabelled with evidence (high -> medium): needs a front proxy that parses requests differently."}'
```

- Critical payload: `{"finding_id":"CVE-2026-50010","triaged_bucket":"critical","package":"io.netty:netty-handler","installed_version":"4.1.128.Final","fixed_version":"4.1.135.Final","evidence":"<evidence text from triaged-findings.csv>"}`. For reference only: do not fire it live (see section 4).
- Do not use "Run now" on the Automation page for this: it does not run the gate. The session prompt also refuses anything that is not critical, but the gate is the part to show.

### Suggested threat-model answers (ecaudit)

The scan does not ask these as separate questions. It pauses with its scan model and asks whether to change it; give these as the reply.

- **What it is:** a plugin that runs inside Cassandra nodes.
- **What matters most:** the audit log is a compliance control, so tampering with it or bypassing it is the worst case.
- **Attackers:** insiders with CQL access, and the supply chain.
- **Trust boundary:** CQL client to Cassandra node.
- **Exposure:** Cassandra is reachable only from the internal network.

Do not describe ecChronos as internet-facing. Its REST server binds to `localhost:8080` by default.

## 2. Tabs to open, in order

### Area 1: Devin as a cloud agent

1. [DeepWiki, ecchronos](https://ericssondemo.devinenterprise.com/search/api_4f70f39e-216a-4bff-bb34-8f309a8ebf76): answer to "How does ecchronos schedule and lock repairs across nodes?"
2. [DeepWiki, ecaudit](https://ericssondemo.devinenterprise.com/search/api_61659d4f-4ac4-4953-ae9a-8a6eed6029fd): answer to "Where does ecaudit decide which Cassandra queries to audit, and how is that configured?"
3. A new session with this prompt ready to paste (do not start it before the call):

```text
Raise unit test coverage on @cog-ericsson-demo/ecchronos, branch workshop/6.1.4, using 10 parallel sessions.

1. Build the project and run the unit tests with coverage: `mvn -B install -Pbuild-cassandra-test-jar -DskipTests=true`, then `mvn -B test jacoco:report`.
2. From the JaCoCo reports, pick the 10 production classes with the most uncovered lines. Skip generated code, plain data holders and constants classes. Show me the list with module, class, line coverage and missed lines.
3. Start one child session per class, all 10 at once, each titled "Add tests: <Class>". Give every child the repository, the branch workshop/6.1.4, its fully qualified class name, its current coverage, and tell it to follow @skills:workshop-parallel-test-coverage.
4. Rules for every child: unit tests only, no production code changes, branch name workshop/tests-<class>, one PR against cog-ericsson-demo/ecchronos with base workshop/6.1.4, never against the upstream repository, and do not merge.
5. When the children finish, give me one table: class, session link, PR link, line and branch coverage before and after.
```

4. [ecaudit pull requests](https://github.com/cog-ericsson-demo/ecaudit/pulls): the finished batch. Ten sessions, ten PRs, all open, test code only.

| Class | Session | PR | Line coverage | Branch coverage | Tests added |
|---|---|---|---|---|---|
| AuditEntryBuilderFactory | [session](https://ericssondemo.devinenterprise.com/sessions/082e8c0ba8ae4b559280a99dc1f5e32b) | [#11](https://github.com/cog-ericsson-demo/ecaudit/pull/11) | 3.6% to 98.0% | 0% to 98.2% | 112 |
| WhitelistDataAccess | [session](https://ericssondemo.devinenterprise.com/sessions/f62f9172dae747bd90dfec7f9848c0d8) | [#10](https://github.com/cog-ericsson-demo/ecaudit/pull/10) | 13% to 100% | 0% to 100% | 34 |
| StatementResourceAdapter | [session](https://ericssondemo.devinenterprise.com/sessions/a267da5ee62e4325a36f93dd11ebe199) | [#8](https://github.com/cog-ericsson-demo/ecaudit/pull/8) | 1% to 98% | 0% to 100% | 60 |
| AuditWhitelistManager | [session](https://ericssondemo.devinenterprise.com/sessions/ee8680c5778443aa8f430f75749ff4da) | [#2](https://github.com/cog-ericsson-demo/ecaudit/pull/2) | 75.4% to 100% | 76.9% to 100% | 12 |
| AuditAdapter | [session](https://ericssondemo.devinenterprise.com/sessions/983da33cc8314760a5ef7bc11fae70cb) | [#3](https://github.com/cog-ericsson-demo/ecaudit/pull/3) | 86.3% to 100% | 77.3% to 100% | 17 |
| AuditQueryHandler | [session](https://ericssondemo.devinenterprise.com/sessions/144b4fbf5d90470cb4e19a65052bdf2e) | [#6](https://github.com/cog-ericsson-demo/ecaudit/pull/6) | 84% to 100% | 100% (unchanged) | 13 |
| EcLog | [session](https://ericssondemo.devinenterprise.com/sessions/8e002b0cdcd943e793afb347fba33c8b) | [#5](https://github.com/cog-ericsson-demo/ecaudit/pull/5) | 52% to 100% | 50% to 75% | 10 |
| QueueReader | [session](https://ericssondemo.devinenterprise.com/sessions/f2dc040aded04ae392f3b1582ab6a145) | [#4](https://github.com/cog-ericsson-demo/ecaudit/pull/4) | 70.6% to 88.2% | 100% (unchanged) | 12 |
| AuditRoleManager | [session](https://ericssondemo.devinenterprise.com/sessions/8268caa69f904bcd86fd064aedbeb3f6) | [#9](https://github.com/cog-ericsson-demo/ecaudit/pull/9) | 86.4% to 100% | 75% to 100% | 16 |
| Slf4jAuditLoggerConfig | [session](https://ericssondemo.devinenterprise.com/sessions/59a1442dac5945069020dec3f360ec5b) | [#1](https://github.com/cog-ericsson-demo/ecaudit/pull/1) | 76.7% to 100% | 12.5% to 100% | 15 |

Coverage is JaCoCo on unit tests, as reported by each session. PRs worth opening for the architect: #8 and #11 both report that the raw `CREATE AGGREGATE` mapping reads a field that was renamed in Cassandra 5.0 and so always fails; #4 reports that `tail` can return no records across a roll-cycle boundary. The sessions reported these in the PR description and did not change production code.

### Area 2: vulnerability management

5. [`workshop/scanner-export.csv`](https://github.com/cog-ericsson-demo/ecchronos/blob/workshop/6.1.4/workshop/scanner-export.csv): 1,089 rows from Trivy 0.75.0, Grype 0.120.1 and PMD.
6. [Triage session](https://ericssondemo.devinenterprise.com/sessions/061f0038df0441c492ffbd4ee4da647d), at its summary table. The full report is [`triage-report.md`](https://github.com/cog-ericsson-demo/ecchronos/blob/workshop/6.1.4/workshop/triage-report.md) and the per-issue list is [`triaged-findings.csv`](https://github.com/cog-ericsson-demo/ecchronos/blob/workshop/6.1.4/workshop/triaged-findings.csv).
7. [Automation "Fix critical security findings"](https://ericssondemo.devinenterprise.com/automations/05ee36951df14f33818649596e19f4dc): webhook trigger, gate on `triaged_bucket == critical`, one run at a time, opens a PR, never merges.
8. The Automation's run history (same page as tab 7): the `medium` finding CVE-2026-24880 was accepted by the webhook and skipped by the gate; the `critical` finding CVE-2026-50010 started a session.
9. [Critical-fix session](https://ericssondemo.devinenterprise.com/sessions/aa445040ec7542598164e27bf86c2151), started by the Automation, and the PR with its fix, [ecchronos #3](https://github.com/cog-ericsson-demo/ecchronos/pull/3). Open, not merged. The session confirmed the triage evidence in the code, added a test that reproduces the issue, and raised `io.netty.version` from 4.1.128.Final to 4.1.135.Final, the lowest fixed version (it also tried 4.1.134.Final, which still fails the test). Reported by the session: `mvn -B verify` passes before (840 tests) and after (843 tests, 3 new), with 0 failures and 1 skipped in both runs. **How the PR was opened:** the Automation's session could not push (every git operation returned HTTP 403), so it handed over its two commits as a patch. The preparation session applied the patch unchanged, repeated the checks, pushed the branch and opened the PR. Repeated checks: the new test fails on 4.1.128.Final (1 of 3) and passes on 4.1.135.Final (3 of 3); `mvn -B verify` passes with 843 tests, 0 failures, 1 skipped; the built distribution contains seven Netty jars, all 4.1.135.Final. The PR description states this under "Provenance". Devin Review found no issues. The PR lists the other Netty advisories as not in scope; the triage report recommends 4.1.137.Final to cover all of them.

### Area 3: code scanning and deep analysis

10. Devin Security overview: [ecaudit scan](https://ericssondemo.devinenterprise.com/code-scan/2e65f24c1a834d9ca135a472edbba931) (14 open findings: 4 medium, 10 low) and [ecchronos scan](https://ericssondemo.devinenterprise.com/code-scan/0fd0f58bc2494232b8d2b052a87d182d) (14 findings, all low). The ecchronos scan now has a daily schedule (02:00) that scans new commits, shown as "Auto-scan" on its page. For the SLA point: new commits are scanned within 24 hours, and the one critical went from webhook to open PR in about 31 minutes.
11. The live scan on `cog-ericsson-demo/ecaudit-workshop-2`: [scan](https://ericssondemo.devinenterprise.com/code-scan/cff2224221b9496eac580618b31c4f8e) and [session](https://ericssondemo.devinenterprise.com/sessions/d57aa308fdfc40d0b4d5993979018e84), started at 12:44 in interactive mode. Rehearsed on `ecaudit-workshop`: [scan](https://ericssondemo.devinenterprise.com/code-scan/e17c73ecb4fe430680f75d58c6d3093e) and [session](https://ericssondemo.devinenterprise.com/sessions/e98f0152a92248a68a62142e7af8cfbb), also paused and usable as a stand-in.
    - Chained vulnerabilities: the second [scan on `ecaudit-workshop`](https://ericssondemo.devinenterprise.com/code-scan/4d60888e18bd417f9e431c31516c3a8c) (the copy with the presenter's commit; 19 findings at 12:40, still running and not yet deduplicated). Three findings reference each other and form a chain:
        1. "Any client can log in as the hardcoded "ecaudit_support" account without a password" (critical): an attacker with no account gets a session, where that role exists.
        2. "Whitelist option value is parsed as unrestricted YAML, letting any logged-in user run code on the database node" (critical): any session becomes code execution on the node. The finding names the login bypass as the way in for an attacker with no account.
        3. "Any client can hide any statement from the audit log by adding a health-check comment" (medium): the finding says the same comment hides the `ALTER ROLE` statement used in step 2.
      Say: "Each finding names the others it combines with." They are three separate findings, not one finding labelled as a chain.
12. Findings tab of the [ecaudit scan](https://ericssondemo.devinenterprise.com/code-scan/2e65f24c1a834d9ca135a472edbba931). Findings that match the threat model (audit log bypass or tampering by an insider with CQL access):
    - "Passwords with spaces or $$ quoting are written to the audit log in plain text" (medium, fixed in tab 13)
    - "Failed logins against role-whitelisted accounts are dropped from the audit log, so password guessing leaves no trace" (medium)
    - "Unauthenticated clients can forge SLF4J audit records with newlines in the login username or query" (medium)
    - "Audit whitelist checks the session keyspace, not the keyspace the query runs in" (low)
13. [Remediation session](https://ericssondemo.devinenterprise.com/sessions/4c3e9c3dab5b41e88a7675b604d83ed2), started from the finding's Remediate action, and its PR [ecaudit #7](https://github.com/cog-ericsson-demo/ecaudit/pull/7). Open, not merged. Proof reported by the session: the new tests fail before the fix and pass after; `mvn -B test` goes from 476 to 484 tests with 0 failures and 1 skipped in both runs; the audit record changes from `ALTER ROLE app_admin WITH PASSWORD = 'correct horse battery staple';` to `ALTER ROLE app_admin WITH PASSWORD = '*****';`. The first fix was pushed about 8 minutes after the session started. Devin Review then found three real gaps in the fix (statements Cassandra rejects, and a comment that could blank out part of an executed statement); each is fixed in its own commit with a test, and the session explains each one. `mvn -B verify` passes on the latest commit, including 238 integration tests. This is a good PR for the human-review discussion: four commits, each with a reason.

### Extras

14. Migration: [ecchronos #2, "Upgrade to Spring Boot 4.0"](https://github.com/cog-ericsson-demo/ecchronos/pull/2) and its [session](https://ericssondemo.devinenterprise.com/sessions/9579c4dc5b764bb5b8205d0ba04cee56). Open, not merged. Spring Boot 3.5.7 to 4.0.8, Spring Framework 6.2.12 to 7.0.9, Tomcat 10.1.48 to 11.0.24, Java 17 unchanged. The PR's "What broke and how it was fixed" table has six entries: relocated Spring Boot packages, a renamed Tomcat factory method, removed `@MockBean`/`@SpyBean`, a Spring Framework 7 change that rejects a null bean (18 tests), REST JSON output that changed with Jackson 3 defaults (no existing test covered it; the session found it by comparing output before and after), and dependency versions that had to move together. `mvn -B verify` runs the same 97 test classes with the same counts before and after. It clears the two spring-webmvc scanner-critical CVEs (CVE-2026-47884, CVE-2026-47890), which have no fix on the Spring Boot 3.5 line. PR checks: 11 passed, 2 failed. The two failures are the Python integration jobs, which fail the same way on the base branch (see section 4). The session states one limit in its report: because those jobs cannot start their Cassandra cluster, the running application has not been exercised end to end through the REST API on Spring Boot 4.
15. [`workshop/cve-baseline.md`](https://github.com/cog-ericsson-demo/ecchronos/blob/workshop/6.1.4/workshop/cve-baseline.md): every critical and high dependency CVE on 6.1.4 with installed and fixed version, CVSS from NVD and reachability evidence.

## 3. Numbers to quote

**Baseline on a clean checkout (Java 17, Maven 3.9.11)**

| Repository | Command | Result | Duration |
|---|---|---|---|
| ecchronos `workshop/6.1.4` | `mvn -B verify` | pass | 169 s |
| ecaudit `master` | `mvn -B test` | pass | 28 s |
| ecaudit `master` | `mvn -B verify` | pass | 221 s |

**Scanner export to triage**

| | |
|---|---|
| Rows in | 1,089 (Trivy 829, Grype 129, PMD 131) |
| Rows by scanner severity | critical 158, high 480, medium 355, low 96 |
| Unique issues out | 108 (93 dependency issues, 15 PMD rule groups) |
| After triage | critical 1, high 0, medium 2, low 105 |
| Relabelled with evidence | 96 of 108 (1 raised, 95 lowered) |
| Scanner-critical | 158 rows, 15 CVEs (18 issues, because three Thymeleaf CVEs are reported on two artifacts each), all relabelled to low with evidence |
| The one critical | CVE-2026-50010, Netty: scanners say high (CVSS 7.5), raised to critical because it was reproduced with the shipped jars |
| Criticals fixed | 1 of 1: [ecchronos #3](https://github.com/cog-ericsson-demo/ecchronos/pull/3), Netty 4.1.128.Final to 4.1.135.Final with a test that fails before and passes after. Open, waiting for human review |

- The 15 scanner-critical CVEs each need a feature ecChronos does not use: Tomcat DIGEST or FORM authentication, servlet security constraints, HTTP/2, XSLT views, Server-Sent Event fragments, user-supplied template expressions, Netty as a TLS server.
- CVE-2026-50010: with CQL TLS, endpoint verification and CRL checking all enabled, a certificate issued to `wrong.example` was accepted for `localhost`. All three settings are off by default.

**Sessions**

| Work | Sessions | Elapsed | ACUs |
|---|---|---|---|
| ecaudit test batch | 10 in parallel | about 19 minutes from first start to last PR | about 53 in total |
| Scanner triage | 1 | about 21 minutes | about 18 |
| Password-masking fix (ecaudit #7) | 1 | about 8 minutes to the first fix; about 55 minutes including three review rounds | about 7 |
| Spring Boot 4 migration (ecchronos #2) | 1 | about 38 minutes to the PR | about 17 |

The batch added 301 unit tests across ten classes.

## 4. Known issues and what to say

- **The Automation's sessions cannot push yet.** Two rehearsal runs, the first as the org service user and the second as the presenter, and a diagnostic run at 12:44 on 11 November, got HTTP 403 on every git operation, while the preparation session and its child sessions push to the same repository. The cause is not confirmed. The network allowlist includes `git-manager.devin.ai`, and an unauthenticated request to the git proxy gets 401, so the network is not blocking it; the candidates are the security profile the Automation inherits and its own network policy (see the optional action in section 1). So the trigger, the gate, the session and the fix are rehearsed, and the PR was opened from the preparation session with the Automation session's commits. Fire only the `medium` payload live. Say: "The Automation started this session from the triaged finding, and the session reproduced the issue, fixed it and ran the full suite. In this workspace the Automation's sessions have no write access to the repository, so the commits were pushed from a second session; the PR says so. With write access the session opens the PR itself, and a person reviews and merges."
- **Threat-model step.** Rehearsed at 12:15 on 11 November on `ecaudit-workshop` (the copy with the presenter's commit): [scan](https://ericssondemo.devinenterprise.com/code-scan/e17c73ecb4fe430680f75d58c6d3093e), [session](https://ericssondemo.devinenterprise.com/sessions/e98f0152a92248a68a62142e7af8cfbb). The scan paused about 3 minutes after it started. It does not ask a list of questions. It shows its scan model (7 matchers, 47 signals in 22 files) and asks: "Should I go ahead with the scan, or do you want to change the scan model first?" The suggested answers from section 1 are typed as the reply. It pauses only when the scan is started in interactive mode; the rehearsal was started that way by the preparation session. The rehearsal scan was left paused and unanswered, and it picked up the four issues in the presenter's commit (the list is in the scan session's message). `ecaudit-workshop-2` holds a plain copy of ecaudit `master` (commit `3abdd4e`) and has never been scanned, so its scan model has not been seen. Fallback if the 12:50 scan is not paused in time: show the paused rehearsal scan instead, or the finished ecaudit scan (tab 10) with the answers read aloud. Dependabot opened four PRs on `ecaudit-workshop-2` when the copy was pushed; they do not affect the scan.
- **Only one finding is critical after triage.** That is the result, not a gap. Say: "158 scanner-critical rows, and none of them is reachable in this codebase. The one issue that is critical here is one both scanners rate high." The Automation therefore has one finding to fix; the live step shows the gate turning a medium finding away.
- **Two red checks on ecchronos PRs.** On the base branch and on ecchronos #2 the style, unit-test and standalone integration jobs pass (Java 17 and 21). The two "Python integration" jobs fail on both with `Nodes did not go up after 60000ms`: the test fixture's Cassandra cluster does not start within a minute on GitHub's runners, before any ecChronos code runs. Say: "Those two jobs fail the same way on the base branch, at cluster start-up in the test fixture. Everything else is green, and the session ran `mvn -B verify` locally; the results are in the PR description." GitHub Actions has since been disabled on the ecchronos fork, so ecchronos #3 shows no workflow checks, only Devin Review (no issues found); its tests were run locally and the results are in the PR description. ecchronos #2 still shows the results of its earlier run.
- **No CI checks on ecaudit PRs.** The fork runs no workflows on pull requests. Each session ran the module's tests locally on Java 17 (not Java 11, which the project also supports), and Devin Review ran on the PRs. No reviewer was assigned automatically because CODEOWNERS names a team that does not exist in the fork.
- **ecaudit PRs target `master`.** That is the branch the scan and DeepWiki use. Only ecchronos has a `workshop/6.1.4` base branch.
- **Scanner counts move.** The CSV was frozen on 10 November. A scan on the day returns different counts as the vulnerability databases update. Quote the frozen numbers.
- **PMD.** The 131 PMD rows come from a broader standard ruleset than the one the project enforces in its own build, run on the seven production modules. All 15 rule groups were triaged as low after every location was read. Say that the project's own PMD and CPD checks pass.
- **Thymeleaf is used.** ecChronos has one Thymeleaf error page, so do not say the library is unused. The three Thymeleaf criticals are relabelled because no user-supplied template expression reaches the engine. The cleanest relabel examples are the two spring-webmvc criticals: XSLT views and Server-Sent Event fragments do not exist in the code.
- **Actuator is partly used.** `/actuator/health` responds, so do not use Actuator as the relabel example.
- **Devin Security findings on ecchronos are all low.** The remediation demo therefore uses the ecaudit medium finding.
- **The migration does not clear everything.** Spring Boot 4.0.8 manages Tomcat 11.0.24, which the advisories list as affected by four of the scanner-critical Tomcat CVEs (fixed in 11.0.25). All four are relabelled to low in the triage. Spring Boot 4.0.9 was 19 days old on 10 November and was held back on purpose, the same idea as a minimum release age in Renovate. The PR says so.
- **ecaudit #7 went through three review rounds.** Present it as the review loop working, not as a fix that needed rework: automated review found gaps, the session confirmed each with a failing test and fixed it, and a person still decides on the merge. One integration test that checks log file size on disk failed once during those runs and passed on the rerun of the full suite. The session had finished at 22:35 on 10 November; read its last commit and comment at 12:30 in case a later review added something.
- **Scan CSV import was started at 12:43 and may not be finished.** See the optional presenter action. Nothing in the sheet depends on it. Its counts can differ from the triage session's, because it is a separate run on the same file.
- **An older PR is open on ecchronos.** [#1](https://github.com/cog-ericsson-demo/ecchronos/pull/1) comes from earlier preparation and targets `demo-6.1.4`. It is not part of the workshop. Open ecchronos PRs by direct link, not through the PR list, or close #1 without merging beforehand. It was left open because the presenter asked for the PRs to stay open.
- **Environment.** Sessions started before 21:40 on 10 November reported "Environment build incomplete" for ecchronos and set up their own build. The environment was rebuilt successfully afterwards, so the live batch should not show that message. If it does, the sessions still work; they take longer to start.
- **Session list.** The earlier preparation sessions were archived on 10 November and no longer show in the list; they can be unarchived. Still listed: the workshop sessions from section 2, the preparation session ("Review Attachment and Suggest Improvements"), the Automation run from tab 9, and the first Automation run until it is archived (section 1).

## 5. Clean-up after the call

Do not run these before the call.

1. Close, without merging, every PR from a `workshop/` branch: ecaudit #1 to #11, ecchronos #2 and ecchronos #3. Delete the `workshop/tests-*`, `workshop/fix-*` and `workshop/spring-boot-4` branches if the repositories are reused.
2. Archive the workshop sessions: the ten test sessions, the triage, remediation and migration sessions, the Automation run, the live batch and its children, and the preparation session.
3. Disable or delete the Automation "Fix critical security findings". Remove the daily schedule from the ecchronos scan ("Auto-scan" on its page) and delete the scan profile "Scanner CSV import - Trivy, Grype, PMD" if it is not reused.
4. A repository that has been scanned already has its threat-model answers. The next demo of the threat-model questions needs a repository copy that has not been scanned.
5. Close ecchronos #1 without merging if it is still open.
6. Delete `ecaudit-workshop-2`. `ecaudit-workshop` contains the presenter's own commit: delete it only if it is no longer needed.
7. The three org Skills (`workshop-parallel-test-coverage`, `workshop-scanner-csv-triage`, `workshop-fix-critical-finding`) and the ecchronos environment blueprint can stay; delete the Skills if they should not be reused.
