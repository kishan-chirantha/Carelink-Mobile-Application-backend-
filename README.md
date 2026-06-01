# ⚙️ CareLink - RESTful API (Backend)

> The Spring Boot backend infrastructure for CareLink, providing secure data persistence, authentication management, and business logic for the Android client.

## ✨ Backend Features

* **REST API:** Fully structured RESTful endpoints for customers, pharmacies, products, and orders.
* **Database Management:** Relational data handling using **Hibernate ORM** and **MySQL**.
* **Secure Access:** Endpoints secured with **Spring Security** and **JWT (JSON Web Tokens)**.
* **Automated Invoicing:** Dynamic PDF generation for completed medical orders.
* **Multi-repo Architecture:** Designed to work independently from the frontend client for better scalability and maintainability.

## 🛠️ Built With

* **Framework:** Java Spring Boot
* **Security:** Spring Security, JWT
* **Database:** MySQL, Spring Data JPA / Hibernate
* **Build Tool:** Maven

## ⚙️ Getting Started (IntelliJ / Eclipse)

### 1. Clone this repository

```bash
git clone https://github.com/your-username/carelink-backend.git
cd carelink-backend
```

### 2. Create a MySQL database

```sql
CREATE DATABASE carelink_db;
```

### 3. Configure database credentials

Open:

```text
src/main/resources/application.properties
```

Configure your database connection:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/carelink_db
spring.datasource.username=root
spring.datasource.password=your_password
```

### 4. Firebase Configuration (Optional)

Place your `firebase-service-account.json` file inside:

```text
src/main/resources/
```

This is required only if Firebase Admin SDK functionality is enabled.

### 5. Run the application

Run the following file:

```text
CareLinkApplication.java
```

The backend server will start on:

```text
http://localhost:8080
```

## 📌 Note

This REST API is designed to serve the CareLink Android application and is maintained as a separate backend repository.
