# Tool spikes of 2026-11-06 — CHG-0001 supersede-2 (proposal provenance only)

> **Provenance only, not part of the frozen spec.** This file lives at `.harness/changes/CHG-0001/tool-spikes.md`,
> beside `clarifications.md`; it is not frozen and is not copied into `docs/specs/CHG-0001/`. ADR-0012 and ADR-0001
> cite it by file path only, as "tool spikes of 2026-11-06, recorded in `.harness/changes/CHG-0001/tool-spikes.md`".
>
> The facts below are recorded as they were reported to the orchestrator by the agents named, and relayed to
> spec-author (run `run-sup2-d2`). spec-author did not re-run any spike or command and did not re-read any source.
> The spike projects themselves are not in the repository. The README and FEATURES statements are WebFetch
> summaries, not verbatim quotes.

## 1. Primary-source read (requirements-analyst; WebFetch summaries, not verbatim)

- The project README (github.com/atlassian/swagger-request-validator), read 2026-11-06, says that prior to v3.0.0
  the project was known as swagger-request-validator.
- New artifacts are `com.atlassian.oai:openapi-request-validator-*`.
- The Spring WebMVC adapter is described as compatible with Spring 7+ and Spring Boot 4+, and requires JDK 17+.
- docs/FEATURES.md: OpenAPI 3.1 has partial support.
- The validator loads specifications with swagger-parser `OpenAPIParser`, with resolve and resolveFully on by
  default.
- Maven Central lists 3.0.0 (metadata `lastUpdated` 2026-05-21).

## 2. java-engineer spike A

Toolchain: JDK Temurin 23.0.2 with `release` 21, Maven 3.10.0, JUnit 5.10.2, surefire 3.2.5; command
`mvn -q -f <spike>/pom.xml test`.

- `com.atlassian.oai:openapi-request-validator-core:3.0.0` resolved.
- An OpenAPI 3.1 module file with relative `$ref`s into a sibling components-only `common-problem-v1.yaml`
  (parameter `ClientHeader`; response `NotFound` → `Problem`; security scheme `sessionCookie`, either as a `$ref` or
  local) was loaded with `OpenApiInteractionValidator.createForSpecificationUrl` from an absolute path, a `file:` URL
  and a classpath location (`contracts/todos-api-v1.yaml`): all six loads OK.
- Valid request: no errors.
- Missing header: `validation.request.parameter.header.missing`.
- 404 with `{"status":404,"code":"NOT_FOUND"}`: no errors. 404 with `{"status":404,"code":"BOGUS","extra":1}`:
  `validation.response.body.schema.enum` and (because the spike's Problem had `additionalProperties: false`)
  `validation.response.body.schema.additionalProperties`.
  [spec-author note, run-sup2-d2: the contract's `Problem` in `contracts/common-problem-v1.yaml` declares no
  `additionalProperties`, so the second error says nothing about extra properties against the contract.]
- One warning on first load: networknt "Unknown keyword components".

## 3. java-engineer spike B

Toolchain: as spike A.

- Public `com.atlassian.oai.validator.schema.SchemaValidator(OpenAPI, MessageResolver)` with
  `validate(String json, Schema schema, String keyPrefix)` returning `ValidationReport`.
- The `OpenAPI` was parsed with `io.swagger.parser.OpenAPIParser().readLocation(path, null, opts)` with
  `opts.setResolve(true)` (or with `OpenAPIV3Parser`); `Problem` was taken from
  `getComponents().getSchemas().get("Problem")`, from the common file or from a module file whose `Problem` is a
  `$ref`. A valid body gave no errors; a bad `code` gave an enum error.
- Cookie `apiKey` security (`todo_session`) is NOT enforced: a request with no cookie gave no error, whether the
  scheme was local or a `$ref`.

## 4. react-engineer spike

Toolchain: Node 26.12.0, pnpm 11.17.0, openapi-typescript 7.13.0, @redocly/cli 2.76.0.

- With a `redocly.yaml` listing `apis` `identity` and `todos`, each with `root` and `x-openapi-ts.output`,
  `openapi-typescript` wrote `out/identity.ts` and `out/todos.ts`.
- Cross-file `$ref`s resolved.
- Each output has its own, structurally identical `components.schemas.Problem` (`code` as a literal union).
- Security schemes are not emitted.
- Unreferenced schemas are dropped.
- When a `redocly.yaml` exists, a CLI path argument is ignored ("APIs are specified both in Redocly Config and CLI
  argument. Only using Redocly config.").
- `redocly bundle` produced one valid single file per module.
- `redocly join` worked, but added tags, `x-tagGroups` and `servers: []`, and kept only the first `info`.

## 5. react-engineer check of the supersede-2 draft at 8eefb35

Toolchain: as the react-engineer spike.

- `redocly lint` (rule set `recommended`) on `identity-api-v1.yaml`, the to-dos module's `todo-api-v1.yaml` (then
  named `todos-api-v1.yaml`; renamed per Q63) and the frozen `todo-api-v1.yaml`: 0 errors (only the warning kinds of the frozen file: `info-license`,
  `no-server-example-com`, `tag-description`).
- `common-problem-v1.yaml` linted on its own: `no-empty-servers` error plus `no-unused-components` warnings (it has
  no operations and no servers).
- Bundled identity + todos give exactly the frozen 11 operations, none in both. Parameters, request bodies, response
  codes and component schemas (including `Problem.code` with 11 values and `FieldError.code` with 10 values, in
  order) are identical to the frozen bundle; the differences are only `info.description`, per-file tags and the
  unreferenced `MethodNotAllowed` and `RequestRejected` responses (defined only in the common file, referenced by no
  operation, as in the frozen file, so absent from the module bundles and the generated types).
- Generated `identity.ts` and `todos.ts` contain `Problem` and `FieldError`.
