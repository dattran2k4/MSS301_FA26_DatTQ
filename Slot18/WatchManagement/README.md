# MSS301 Assignment 02 Watch Management

Four Maven projects for student code `de181019` implement watch CRUD through an API Gateway with a database per service and a two-service Saga. Java 21, Spring Boot 4.1.1, Spring Cloud 2025.1.3, SQL Server, JPA/Hibernate code-first, JWT and Postman are used. The source assignment is in the parent Slot18 directory.

| Project | Port | Database | Responsibility |
| --- | --- | --- | --- |
| `MSAccount_de181019` | 8081 | DBAccount | Accounts, login, JWT |
| `MSBrand_de181019` | 8082 | DBBrand | Brands and brand-watch assignments |
| `MSWatch_de181019` | 8083 | DBWatch | Watches, categories, Saga orchestration |
| `APIGateway_de181019` | 8080 | none | Client routes and JWT verification |

## Start locally

1. Install Java 21, Maven 3.9+ and SQL Server. Run [`database/create-databases.sql`](database/create-databases.sql) as a SQL Server administrator to create the three databases. Hibernate creates the tables at service startup. No service needs access to another service's database.
2. Set the variables in [`.env.example`](.env.example) in your shell. `DB_PASSWORD`, `JWT_SECRET` and `INTERNAL_API_KEY` are required. Use the same JWT key for Gateway, Account, Brand and Watch, and the same internal key for Brand and Watch. Do not commit your real values.
3. From this directory run `mvn -q clean package`. Then open four terminals and run the following in order:

```bash
java -jar MSAccount_de181019/target/ms-account-1.0.0.jar
java -jar MSBrand_de181019/target/ms-brand-1.0.0.jar
java -jar MSWatch_de181019/target/ms-watch-1.0.0.jar
java -jar APIGateway_de181019/target/api-gateway-1.0.0.jar
```

The initializer seeds 5 accounts, 5 categories, 5 brands and 10 watches only once. Login with `adminsys` / `@1` for administrator operations. The stored sample passwords are BCrypt hashes. `ReleaseDate` validation uses `Asia/Ho_Chi_Minh`.

## API

All client calls use `http://localhost:8080`. `GET /api/watches` is public and includes `categoryName` and `waterResistance`, newest watch ID first. Login is `POST /api/auth/login`. Create, update and delete are `POST /api/watches`, `PUT /api/watches/{id}`, and `DELETE /api/watches/{id}` with `Authorization: Bearer <accessToken>` from an administrator. Brand reads are `GET /api/brands`, `GET /api/brands/{id}` and `GET /api/brands/{id}/watches`. An administrator can inspect `GET /api/sagas/{id}` and recover an unfinished Saga with `POST /api/sagas/{id}/recover`.

Create and update require `name` longer than 10 characters, `price` at least 100, today's `releaseDate`, and values for all fields. This validation applies to new requests, not the supplied seed records. All business responses use `{success,message,data}`. The fault header `X-Saga-Fail-After-Step` is useful only when `SAGA_FAULT_INJECTION=true`.

See [Saga design](docs/SAGA_DESIGN.md) for step order, failure handling and current limits.

## Tests

Import [`WatchManagement_Saga.postman_collection.json`](WatchManagement_Saga.postman_collection.json) and [`WatchManagement_Local.postman_environment.json`](WatchManagement_Local.postman_environment.json) into Postman. Set `SAGA_FAULT_INJECTION=true` for this local test run and use Collection Runner in item order. The collection sets today's date in Vietnam time and saves JWTs and Saga IDs automatically. It creates and then deletes its own sample watch.

The automated end-to-end check is [`scripts/integration_test.py`](scripts/integration_test.py). After building, run `python3 scripts/integration_test.py --sql` for SQL Server or omit `--sql` for an isolated H2 smoke test. SQL mode expects three **dedicated test databases** on port 11433; do not point this script at application databases containing user data. The integration run verified over 80 HTTP assertions against a dedicated SQL Server 2022 container, including authentication, validation boundaries, the data seed, all CRUD Saga outcomes, compensation, Brand Service outage/recovery and restart. It launches the four services on ports 18080–18083 and stops them after the run. It cleans only its own recognizable test watch through the API if a prior run was interrupted.

## Docker Compose

With Docker available, set `DB_PASSWORD`, `JWT_SECRET` and `INTERNAL_API_KEY` in your shell or a local `.env` file, then run `docker compose up --build` from this directory. The Compose stack starts SQL Server, creates the three databases, builds each service and exposes only Gateway on `localhost:8080` (and SQL Server on `localhost:1433` by default). Set `SQL_HOST_PORT` or `GATEWAY_HOST_PORT` to use different local ports. A named volume preserves database data between restarts. Use `docker compose down` to stop it; the volume remains unless explicitly removed.
