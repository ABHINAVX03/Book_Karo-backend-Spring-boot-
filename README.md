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

For the **prod** profile, set the following environment variables:

```bash
export JWT_SECRET_KEY="your_jwt_secret_key_here_minimum_32_characters_long"
export SPRING_DATASOURCE_URL="jdbc:postgresql://<host>:5432/<dbname>?sslmode=require"
export SPRING_DATASOURCE_USERNAME="your_db_user"
export SPRING_DATASOURCE_PASSWORD="your_db_password"
export APP_CORS_ALLOWED_ORIGINS="https://your-frontend.example.com"

# Optional services
export SPRING_MAIL_HOST="smtp.gmail.com"
export SPRING_MAIL_PORT="587"
export SPRING_MAIL_USERNAME="your_email_user"
export SPRING_MAIL_PASSWORD="your_email_password"
export SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH="true"
export SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE="true"
export CLOUDINARY_CLOUD_NAME="your_cloudinary_cloud_name"
export CLOUDINARY_API_KEY="your_cloudinary_api_key"
export CLOUDINARY_API_SECRET="your_cloudinary_api_secret"
export RAZORPAY_KEY_ID="your_razorpay_key_id"
export RAZORPAY_KEY_SECRET="your_razorpay_key_secret"
export TWILIO_ACCOUNT_SID="your_twilio_sid"
export TWILIO_AUTH_TOKEN="your_twilio_auth_token"
export TWILIO_PHONE_NUMBER="+1234567890"
```

If some optional services are not used, leave those variables blank or unset.

### 4. Configure application properties

**Dev profile** (`application-dev.properties`):
```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/postgres
spring.datasource.username=admin
spring.datasource.password=secret
spring.jpa.hibernate.ddl-auto=create
app.security.jwt-secret=your_dev_secret_key
```

**Prod profile** (`application-prod.properties`):
```properties
spring.datasource.url=jdbc:postgresql://<your-neon-host>/<dbname>?sslmode=require
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=validate
app.security.jwt-secret=${JWT_SECRET_KEY}
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
| `JWT_SECRET_KEY` | Secret key for signing JWT tokens (min 32 characters) | Yes |
| `SPRING_DATASOURCE_URL` | JDBC URL for PostgreSQL with PostGIS | Yes |
| `SPRING_DATASOURCE_USERNAME` | PostgreSQL username | Yes |
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL password | Yes |
| `APP_CORS_ALLOWED_ORIGINS` | Allowed frontend origins | Yes |
| `SPRING_MAIL_HOST` | SMTP host | No |
| `SPRING_MAIL_PORT` | SMTP port | No |
| `SPRING_MAIL_USERNAME` | SMTP username | No |
| `SPRING_MAIL_PASSWORD` | SMTP password | No |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH` | Enable SMTP auth | No |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE` | Enable STARTTLS | No |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary cloud name | No |
| `CLOUDINARY_API_KEY` | Cloudinary API key | No |
| `CLOUDINARY_API_SECRET` | Cloudinary API secret | No |
| `RAZORPAY_KEY_ID` | Razorpay API key | No |
| `RAZORPAY_KEY_SECRET` | Razorpay API secret | No |
| `TWILIO_ACCOUNT_SID` | Twilio account SID | No |
| `TWILIO_AUTH_TOKEN` | Twilio auth token | No |
| `TWILIO_PHONE_NUMBER` | Twilio sender phone number | No |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile (`dev` or `prod`) | No |

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