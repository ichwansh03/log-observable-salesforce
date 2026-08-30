# Apexium.log

a Salesforce developer productivity tool designed to simplify Debug Log management, automate Trace Flag scheduling, monitor Apex code coverage, track metadata changes, and retain debugging history beyond Salesforce's native limitations. It helps developers spend less time managing logs and more time building reliable applications.

## Features

* 🚀 **Centralized Debug Log Management** to manage, search, download, and delete Salesforce Debug Logs from a single interface, making debugging more efficient.
* ⏰ **Automated Recurring Trace Flags** to automatically extend user trace sessions beyond Salesforce's 24-hour Trace Flag limitation.
* 📊 **Apex Code Coverage Reports** to view code coverage for all Apex Classes and Triggers in a centralized dashboard, helping ensure deployment readiness.
* 🔍 **Metadata Change Tracking** to detect changes in Apex Classes and Triggers through scheduled polling, with a timeline view and body diff comparison via MinIO-stored snapshots. History records include who made the change and when.
* ♻️ **Reusable Debug Sessions** to quickly reuse previously configured Trace Flags and debugging settings without repeating manual configuration.
* 🗃️ **Extended Debug Log Retention** to retain Debug Logs for up to 30 days, even after they have expired or been removed from Salesforce.
* ⚡ **Faster Root Cause Analysis** by combining Debug Log history, metadata changes, and trace information in a single application for quicker issue investigation.
* 📈 **Improved Developer Productivity** by automating repetitive tasks such as Trace Flag scheduling, Debug Log cleanup, and code coverage monitoring.
* 🛡️ **Reduced Debug Log Storage Issues** through selective or bulk Debug Log cleanup, helping prevent Salesforce storage limit issues.
* 🎯 **Built for Salesforce Developers** who need an all-in-one solution for debugging, monitoring, and tracking Apex code changes.

## Architecture Design

The system periodically fetches logs and metadata from Salesforce via REST and Tooling APIs, stores them in PostgreSQL and MinIO, and exposes everything through a React frontend.

### Visual Workflow
```text
+----------------+      Tooling / REST API      +-------------------+
|   Salesforce   | <----------------------------> |  Kotlin Backend  |
| (ApexLogs,     |   (Poll logs & metadata)      |  (Spring Boot)   |
|  ApexClass,    |                                |                   |
|  ApexTrigger,  |                                +---------+---------+
|  TraceFlag)    |                                          |
+----------------+                          +---------------+---------------+
                                            |               |               |
                                            v               v               v
                                    +-----------+   +-----------+   +---------------+
                                    | PostgreSQL |   |   MinIO   |   |    Redis      |
                                    | (Metadata, |   | (Log Body |   |  (Cache,      |
                                    |  History,  |   |  Storage) |   |   Sessions)   |
                                    |  Jobs)     |   |           |   |               |
                                    +-----------+   +-----------+   +---------------+
                                            |
                                            v
                                    +---------------+
                                    | React Frontend|
                                    | (TypeScript)  |
                                    +---------------+
```

### Technical Flow (Mermaid)
```mermaid
graph TD
    SFDC[Salesforce] -- Tooling/REST API --> Backend[Kotlin Backend]
    Backend -- Poll Logs & Store --> PG[(PostgreSQL)]
    Backend -- Store Log Body --> MinIO[(MinIO)]
    Backend -- Cache --> Redis[(Redis)]
    Backend -- Serve Data --> FE[React Frontend]
    FE -- User Actions --> Backend
```

### Components

1.  **Salesforce (Source)**: Source of ApexLogs, ApexClass, ApexTrigger, TraceFlag, and DebugLevel metadata. Accessed via REST and Tooling APIs.
2.  **Kotlin Backend (Spring Boot)**: Polls Salesforce periodically for new logs and metadata changes. Stores processed data in PostgreSQL and MinIO. Exposes REST APIs for the frontend.
3.  **PostgreSQL**: Stores log metadata, Apex class/trigger info, code coverage, metadata history timeline (who changed, when), trace jobs, and debug levels.
4.  **MinIO**: Stores full debug log body files and Apex class/trigger body snapshots (current + historical) for long-term retention and diff comparison.
5.  **Redis**: Caching layer for Salesforce access tokens and metadata queries.
6.  **React Frontend**: TypeScript-based dashboard for viewing logs, managing traces, comparing metadata, and monitoring coverage.

## Tech Stack

- **Backend**: Kotlin 2.1.0, Spring Boot 4.0.6
- **Frontend**: React 19, TypeScript, Vite
- **Database**: PostgreSQL 17, Redis (Alpine)
- **Storage**: MinIO (S3-compatible object storage)
- **Build Tools**: Maven (backend), npm (frontend)
- **Communication**: Salesforce REST API & Tooling API

## Getting Started

### Prerequisites

- Java 21+
- Node.js (Latest LTS)
- Docker & Docker Compose (for PostgreSQL, Redis, MinIO)

### Setup

1.  **Salesforce Setup**: Create a Connected App in your Salesforce org with OAuth 2.0 Client Credentials flow enabled. Note the Consumer Key (`client_id`) and Consumer Secret (`client_secret`).

2.  **Environment Configuration**: Create a `.env` file in the root directory:
    ```bash
    # Salesforce Configuration
    SALESFORCE_INSTANCE_ORG=https://your-instance.sandbox.my.salesforce.com
    SALESFORCE_CLIENT_ID=your_client_id
    SALESFORCE_CLIENT_SECRET=your_client_secret
    SALESFORCE_GRANT_TYPE=client_credentials
    SALESFORCE_API_VERSION=v61.0

    # MinIO Configuration
    MINIO_URL=http://minio:9000
    MINIO_ACCESS_KEY=your_access_key
    MINIO_SECRET_KEY=your_secret_key
    MINIO_ROOT_USER=your_root_user
    MINIO_ROOT_PASSWORD=your_root_password
    MINIO_BUCKET_NAME=sfdc-bucket

    # Database Configuration
    DB_HOST=db
    DB_PORT=5432
    DB_NAME=sfdc_logs
    DB_USER=your_db_user
    DB_PASSWORD=your_db_pw
    ```

3.  **Start Infrastructure** (PostgreSQL, Redis, MinIO):
    ```bash
    docker compose up -d --build
    ```

4.  **Run Backend**:
    ```bash
    ./mvnw spring-boot:run
    ```

5.  **Run Frontend** (in a separate terminal):
    ```bash
    cd frontend
    npm install
    npm run dev
    ```

6.  Open [http://localhost:5173](http://localhost:5173) to access the dashboard.

## Monitoring & API Documentation

The project includes built-in observability and interactive documentation tools.

### Monitoring Stack
When running via Docker Compose, the following monitoring services are available:

- **Grafana**: [http://localhost:3000](http://localhost:3000) (Credentials: `admin` / `admin`)
    - Used for visualizing JVM metrics, Spring Boot statistics, and system health.
    - Pre-configured dashboards are recommended in [MONITORING.md](./MONITORING.md).
- **Prometheus**: [http://localhost:9090](http://localhost:9090)
    - Time-series database that scrapes metrics from the application.
- **Spring Actuator**: [http://localhost:8080/actuator](http://localhost:8080/actuator)
    - Provides raw metric data, health status, and info endpoints.

### API Documentation (Swagger)
Interactive API documentation is automatically generated from the Spring Boot controllers:

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
    - Explore and test the REST API endpoints directly from your browser.
- **OpenAPI Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
    - Raw JSON/YAML specification for the API.

For detailed configuration of Prometheus and custom dashboards, refer to [MONITORING.md](./MONITORING.md).

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](./CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## License

MIT License

Copyright (c) 2026 Ichwan Sholihin

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

---
*Maintained by the Observability Engineering Team.*
