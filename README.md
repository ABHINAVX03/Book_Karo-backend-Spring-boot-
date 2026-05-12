# UberApp - Spring Boot Backend

A ride-booking backend application built with Spring Boot 3.3.1, featuring JWT authentication, PostGIS spatial data support, and RESTful APIs for riders and drivers.

---

## Tech Stack

- **Java 21**
- **Spring Boot 3.3.1**
- **Spring Security** (JWT Authentication)
- **Spring Data JPA** + **Hibernate Spatial**
- **PostgreSQL** with **PostGIS** extension
- **ModelMapper**
- **Lombok**
- **SpringDoc OpenAPI** (Swagger UI)
- **JavaMail** (SMTP email support)

---

## Project Structure

```
src/main/java/com/codingshuttle/project/uber/uberApp/
├── advices/          # Global exception handler & API response wrapper
├── configs/          # Security, CORS, and mapper configuration
├── controllers/      # REST controllers (Auth, Rider, Driver)
├── dto/              # Data Transfer Objects
├── entities/         # JPA entities
│   └── enums/        # Enums (RideStatus, PaymentMethod, Role, etc.)
├── security/         # JWT filter and utilities
└── services/         # Business logic
```

---

## Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL with PostGIS extension (or use Neon cloud DB)

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/your-username/uberApp.git
cd uberApp
```

### 2. Configure the database

Enable PostGIS on your PostgreSQL database:

```sql
CREATE EXTENSION IF NOT EXISTS postgis;
```

### 3. Set up environment variables

For the **prod** profile, set the following environment variable:

```bash
JWT_SECRET_KEY=your_jwt_secret_key_here
```

### 4. Configure application properties

**Dev profile** (`application-dev.properties`):
```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/postgres
spring.datasource.username=admin
spring.datasource.password=secret
spring.jpa.hibernate.ddl-auto=create
jwt.secretKey=your_dev_secret_key
```

**Prod profile** (`application-prod.properties`):
```properties
spring.datasource.url=jdbc:postgresql://<your-neon-host>/<dbname>?sslmode=require
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
jwt.secretKey=${JWT_SECRET_KEY}
```

### 5. Run the application

```bash
# Dev mode
./mvnw spring-boot:run

# Or build and run the JAR
./mvnw clean package -DskipTests
java -jar target/uberApp-0.0.1-SNAPSHOT.jar
```

---

## Running with Docker

A `Dockerfile` is included for containerized deployment.

### Build the image

```bash
docker build -t uberapp-backend .
```

### Run with Docker Compose

From the project root (where `docker-compose.yml` is located):

```bash
docker compose up --build
```

This starts:
- **Backend** on `http://localhost:8080`
- **Frontend** on `http://localhost`

---

## API Endpoints

### Auth
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/auth/signup` | Register a new user | No |
| POST | `/auth/login` | Login and get JWT token | No |

### Rider
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/riders/requestRide` | Request a new ride | Yes |
| POST | `/riders/cancelRide/{rideRequestId}` | Cancel a ride request | Yes |
| POST | `/riders/rateDriver` | Rate a driver | Yes |
| GET | `/riders/getMyRides` | Get all rides for rider | Yes |
| GET | `/riders/getMyProfile` | Get rider profile | Yes |

### Driver
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/drivers/acceptRide/{rideRequestId}` | Accept a ride request | Yes |
| POST | `/drivers/startRide/{rideRequestId}` | Start a ride | Yes |
| POST | `/drivers/endRide/{rideId}` | End a ride | Yes |
| POST | `/drivers/cancelRide/{rideId}` | Cancel a ride | Yes |
| POST | `/drivers/rateRider` | Rate a rider | Yes |
| GET | `/drivers/getMyRides` | Get all rides for driver | Yes |
| GET | `/drivers/getMyProfile` | Get driver profile | Yes |

---

## Authentication

The API uses **JWT Bearer Token** authentication.

1. Call `/auth/login` with your credentials
2. Copy the token from the response
3. Add it to all subsequent requests as a header:

```
Authorization: Bearer <your_token_here>
```

---

## API Documentation (Swagger UI)

Once the app is running, visit:

```
http://localhost:8080/swagger-ui/index.html
```

---

## Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `JWT_SECRET_KEY` | Secret key for signing JWT tokens | Yes (prod) |
| `SPRING_PROFILES_ACTIVE` | Active profile (`dev` or `prod`) | No (defaults to dev) |

---

## Database Schema

The application uses **Hibernate** with `ddl-auto=update` in production. Key entities:

- `app_user` — Users (riders and drivers)
- `driver` — Driver profiles with location (PostGIS Point)
- `ride_request` — Ride requests with pickup/dropoff points
- `ride` — Active and completed rides
- `wallet` — User wallets
- `wallet_transaction` — Transaction history
- `rating` — Driver and rider ratings

---

## Sample Data

The application loads sample users on startup via `data.sql` (dev profile only).

Sample login credentials:
```
Email: aarav@gmail.com
Password: Password
```

---

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -m 'Add your feature'`)
4. Push to the branch (`git push origin feature/your-feature`)
5. Open a Pull Request

---

## License

This project is for educational purposes.