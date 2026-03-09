# CI/CD Setup Guide

This guide explains how to configure the CI/CD pipeline for the Discord-like project.

## CI Workflow

The CI workflow (`.github/workflows/ci.yml`) runs automatically on:
- Push to `main` or `develop` branches
- Pull requests to `main` branch

### What the CI Does

1. **Sets up the environment:**
   - JDK 21 (Temurin distribution)
   - PostgreSQL 15-alpine
   - Redis 7.2-alpine
   - Gradle dependencies caching

2. **Runs tests:**
   - Excludes tests tagged with `@Tag("integration")` (tests that require Kafka or full Spring context)
   - Uses `application-ci.yaml` profile which excludes `KafkaAutoConfiguration`
   - Tests are run with: `./gradlew test -PexcludeTags="integration"`

3. **Builds the application:**
   - Runs `./gradlew build -x test` (tests already ran)

4. **Uploads test reports:**
   - Test reports are available as artifacts in GitHub Actions

### Running Integration Tests Locally

To run integration tests (including those with `@Tag("integration")`) locally with Kafka:

```bash
# Start services with docker-compose
docker-compose up -d postgres redis kafka zookeeper

# Run all tests
./gradlew test

# Run only integration tests
./gradlew test -PincludeTags="integration"

# Stop services when done
docker-compose down
```

### Running Tests with Specific Profiles

```bash
# Run tests with CI profile (no Kafka)
./gradlew test -Dspring.profiles.active=ci -PexcludeTags="integration"

# Run tests with test profile (uses Testcontainers)
./gradlew test -Dspring.profiles.active=test
```

## CD Workflow

The CD workflow (`.github/workflows/cd.yml`) is currently **disabled** and commented out.

### Enabling CD

To enable automatic Docker image publishing to Docker Hub:

1. **Create Docker Hub access token:**
   - Go to [Docker Hub Security Settings](https://hub.docker.com/settings/security)
   - Click "New Access Token"
   - Give it a descriptive name (e.g., "GitHub Actions")
   - Select permissions: Read, Write, Delete
   - Copy the generated token

2. **Add secrets to GitHub repository:**
   - Go to repository → Settings → Secrets and variables → Actions
   - Click "New repository secret"
   - Add `DOCKERHUB_USERNAME`: Your Docker Hub username
   - Add `DOCKERHUB_TOKEN`: The access token created above

3. **Uncomment the CD workflow:**
   - Edit `.github/workflows/cd.yml`
   - Uncomment the login, build, and push steps
   - Comment out or remove the "Build Docker image (no push)" step
   - Commit and push to `main` branch

4. **Update image name if needed:**
   - The workflow currently uses `luishbarros/discord-like:latest`
   - Update to your Docker Hub username if different

### Docker Image Tags

When CD is enabled, images are pushed with two tags:
- `latest`: Always points to the most recent build
- `{commit-sha}`: Specific commit hash for reproducible builds

Example: `luishbarros/discord-like:abc123def456`

## Branch Protection Rules

Configure branch protection rules in GitHub for the `main` branch:

1. Go to repository → Settings → Branches
2. Click "Add rule" for `main`
3. Configure:
   - ✅ Require pull request reviews before merging
   - ✅ Require approvals (at least 1)
   - ✅ Dismiss stale PR approvals when new commits are pushed
   - ✅ Require status checks to pass before merging
   - ✅ Require branches to be up to date before merging
   - ✅ Do not allow bypassing the above settings

### Required Status Checks

- `build-and-test` (from CI workflow)

## Profiles

### Available Profiles

- **default**: Development environment with local services
- **test**: Test environment using Testcontainers (PostgreSQL, Redis, Kafka)
- **ci**: CI environment excluding Kafka (PostgreSQL, Redis via GitHub Actions)

### Profile Configurations

- `application.yaml`: Default configuration
- `application-test.yaml`: Test configuration with Testcontainers
- `application-ci.yaml`: CI configuration without Kafka

## Troubleshooting

### CI Tests Failing

If CI tests fail but local tests pass:

1. Check if the test has `@Tag("integration")` - it's excluded from CI
2. Verify environment variables match `application-test.yaml` secrets
3. Check service health in CI logs (PostgreSQL, Redis)

### CD Build Failing

If Docker build fails:

1. Verify Dockerfile syntax: `docker build -t test .`
2. Check for missing dependencies or incorrect paths
3. Review build logs in GitHub Actions

### Kafka-Related Test Failures

If tests fail with Kafka connection errors:

1. Ensure test has `@Tag("integration")` to exclude from CI
2. For local runs, verify Kafka is running: `docker-compose ps kafka`
3. Check `application-test.yaml` for correct Kafka bootstrap servers

## Best Practices

1. **Always run tests locally before pushing:** Use `./gradlew test`
2. **Use feature branches:** Create branches from `develop` for new features
3. **PR to `develop`**: For ongoing development work
4. **PR to `main`**: For production-ready code (requires passing CI)
5. **Tag your tests:** Use `@Tag("integration")` for tests requiring Kafka
6. **Keep CD disabled until Docker Hub is ready:** Don't waste CI time building images

## Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Docker Hub Documentation](https://docs.docker.com/docker-hub/)
- [Spring Boot Profiles](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.profiles)
- [JUnit 5 Tagging](https://junit.org/junit5/docs/current/user-guide/#writing-tests-tagging-and-filtering)
