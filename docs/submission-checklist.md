# Submission Checklist

- [ ] `docker compose up --build` starts PostgreSQL and the application.
- [ ] `/actuator/health` is UP.
- [ ] URL creation returns a short code.
- [ ] Reusing the same Idempotency-Key does not create a second short URL.
- [ ] Redirect records a click.
- [ ] Analytics returns click counts.
- [ ] Greenfield run reaches approval state.
- [ ] Approval resumes the run and allows release.
- [ ] Brownfield run includes codebase reasoning and impact analysis.
- [ ] Ambiguous run stops for clarification.
- [ ] Clarification increments plan version and resumes.
- [ ] Replan endpoint changes the plan version and invalidates downstream work.
- [ ] Safe-stop transitions the run to STOPPED.
- [ ] Rollback transitions the run to ROLLED_BACK.
- [ ] Agent control endpoints reject missing control tokens.
- [ ] Audit endpoint shows lifecycle events.
- [ ] Actuator exposes metrics.
- [ ] `./mvnw test` passes on a machine with network access or cached Maven dependencies.
