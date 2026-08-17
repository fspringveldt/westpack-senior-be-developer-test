# Development Setup

Quick guide for setting up and contributing to the Savings Account API.

## Prerequisites

- Java 21+ (Temurin LTS)
- Gradle 8+ (or use `./gradlew`)
- Docker & Docker Compose (for running tests)

## Getting Started

```bash
docker-compose up -d          # Start PostgreSQL and Redis
./gradlew clean build         # Build the project
./gradlew bootRun             # Run the application
./gradlew test                # Run all tests
```

Application runs on `http://localhost:8080`.

## Code Style

Follow Google Java Style Guide. Key points:

- Line length: 100 characters
- Indentation: 4 spaces
- Naming: `camelCase` for methods, `UPPER_CASE` for constants, `PascalCase` for classes
- Comments explain _why_, not _what_

## Testing

```bash
./gradlew test                              # All tests
./gradlew test --tests AccountServiceTest   # Specific test
```

Tests use Testcontainers for PostgreSQL and Redis (requires Docker).

## Common Tasks

### Add a New Endpoint

1. Create request/response DTO in `api/`
2. Add service method in `service/AccountService.java`
3. Add controller method in `api/AccountController.java`
4. Add unit tests in `service/*Test.java`
5. Add controller tests in `api/*ControllerTest.java`

### Database Changes

1. Create migration: `touch src/main/resources/db/migration/V2__description.sql`
2. Write SQL migration
3. Run: `./gradlew bootRun` (Flyway applies migrations automatically)

### Configuration

Environment variables override `application.yml`:

```bash
DB_URL=jdbc:postgresql://localhost:5432/savings
DB_USERNAME=savings
DB_PASSWORD=savings
REDIS_HOST=localhost
REDIS_PORT=6379
```

## Commit Messages

Use format: `[Feature|Fix|Refactor|Test|Docs] Brief description`

Example:

```
[Fix] Race condition in account limit check

Moved validation to database level for atomicity.

Fixes #123
```

## Pull Requests

Before submitting:

1. Run tests: `./gradlew test`
2. Build: `./gradlew clean build`
3. Ensure code style is followed

Include in PR:

- Clear description of changes
- Any assumptions or trade-offs
- Related issue numbers

## Performance Notes

- Account GET operations are cached for 10 minutes in Redis
- Database queries use indexes on `customer_name` and `account_number`
- Connection pool: 10 max, 2 min idle (fail-fast on database unavailability)

## Troubleshooting

**"Connection refused" to PostgreSQL**:

```bash
docker-compose ps              # Check if postgres is running
docker-compose restart postgres
```

**Cache misses on every request**:

```bash
docker-compose exec redis redis-cli ping  # Should respond "PONG"
```

**Tests failing**:

- Ensure Docker is running
- Clear caches: `./gradlew clean`
- Check Testcontainers output for issues

## Questions?

See the README for full API documentation and design decisions.
