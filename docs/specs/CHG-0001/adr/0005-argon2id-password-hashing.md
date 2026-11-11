# ADR-0005 Argon2id password hashing

Status: proposed (CHG-0001, N03-spec). Requirements: REQ-AUTH-003, REQ-AUTH-001 AC-5, AC-11, NFR-004 AC-1.

## Context

Passwords must be stored only as a salted one-way hash from a strong password-hashing algorithm, with a unique salt
per account (REQ-AUTH-003 AC-1, AC-2). Passwords may be up to 128 characters (REQ-AUTH-001 AC-11), which is up to
512 bytes in UTF-8. bcrypt reads only the first 72 bytes, so two long passwords sharing a 72-byte prefix would
verify against each other (critic finding 10).

## Decision

- Hash with **Argon2id** through Spring Security's `Argon2PasswordEncoder`, which needs **BouncyCastle**
  (`org.bouncycastle:bcprov-jdk18on`, MIT-style Bouncy Castle licence, actively maintained).
- Parameters, from the OWASP Password Storage Cheat Sheet minimum for Argon2id: memory 19 MiB (19456 KiB),
  iterations 2, parallelism 1, salt 16 random bytes per hash, hash length 32 bytes. The java-engineer re-checks the
  current OWASP figures when pinning versions and records any change in this ADR's successor.
- The whole password is hashed: no length truncation and no pre-hash. Passwords are **not trimmed and not
  Unicode-normalised**; lengths are counted in code points.
- The stored value is the encoder's self-describing string (`$argon2id$v=19$m=19456,t=2,p=1$<salt>$<hash>`), so the
  parameters can be raised later and old hashes still verify.
- At login, when the identifier is unknown the API still verifies the password against a fixed dummy Argon2id hash,
  so response time does not reveal whether the identifier exists (REQ-AUTH-002 AC-3). A password longer than
  128 characters is refused as invalid credentials without hashing, for every identifier alike.

## Consequences

- No 72-byte limit; every character of a 128-character password counts.
- About 19 MiB of memory per hash in flight; with a local single-user app this is negligible.
- One new runtime dependency (BouncyCastle).

## Alternatives

- bcrypt: silently ignores bytes beyond 72.
- bcrypt with a documented SHA-256/HMAC pre-hash: works, but adds a custom construction that must be got right
  (base64 encoding to avoid NUL bytes).
- PBKDF2-HMAC-SHA256 (no extra dependency): acceptable for FIPS contexts but not memory-hard; needs 600,000
  iterations for comparable strength.
- scrypt: memory-hard and also in Spring Security, but Argon2id is the current first recommendation.
