# FUCinemaBookingSystem

Assignment 1 MSS301: 3 microservices, API Gateway, SQL Server 2022, MongoDB 7, MySQL 8.

## Run

Requirements: Java 21, Maven, Docker Compose. Run these commands from this directory.

```bash
docker compose up -d
docker compose ps -a
mvn -f customer-service/pom.xml spring-boot:run
mvn -f movie-service/pom.xml spring-boot:run
mvn -f booking-service/pom.xml spring-boot:run
mvn -f api-gateway/pom.xml spring-boot:run
```

Run each Maven command in a separate terminal. Wait for SQL Server to be healthy and `cinema-sqlserver-init` to exit with code 0 before starting customer-service. The public entry point is `http://localhost:9000`.

| Service | Port | Database |
|---|---:|---|
| customer-service | 8081 | SQL Server `cinema_customer` |
| movie-service | 8082 | MongoDB `cinema_movie` |
| booking-service | 8083 | MySQL `cinema_booking` |
| api-gateway | 9000 | none |

Test accounts from the assignment: Admin `admin@fucinema.com` / `@@abc123@@`; customers `an@gmail.com`, `binh@gmail.com` / `123456`; inactive customer `chi@gmail.com` / `123456`.

## Postman

Import `postman/FUCinema-Assignment1.postman_collection.json` and `postman/FUCinema-Local.postman_environment.json`. Select the environment and run the collection in order. It creates new genre, room, movie and showtime records, then tests booking and reporting. Run with all four services and databases active. The manual BR14 outage check requires stopping movie-service, sending a booking request, and confirming HTTP 503.

## Verification

Compile each service:

```bash
for service in customer-service movie-service booking-service api-gateway; do
  mvn -q -f "$service/pom.xml" -DskipTests compile
done
```

On the current Apple Silicon machine, SQL Server 2022 exits inside the x86 emulation layer before it becomes healthy. MongoDB, MySQL, movie-service, booking-service and api-gateway were started; 21 gateway integration checks passed. Customer-service and the full Postman run still need verification on a host that can run the required SQL Server 2022 image. The local machine also has another MySQL daemon on port 3306; to test booking-service against the Docker container without changing the submitted configuration, set `SPRING_DATASOURCE_URL` to `jdbc:mysql://<container-ip>:3306/cinema_booking` when launching it.

## Evidence for the Word report

Capture one screenshot for each TODO's output or relevant code, then paste it into the marked box in `../Assignment1_Report.docx`. Also capture the Postman Collection Runner and the database checks described in the assignment guide. The report includes the actual commit messages and leaves personal details for the student to fill in.
