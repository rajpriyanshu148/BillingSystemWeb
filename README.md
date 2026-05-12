# BillingSystem Pro 🏪

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)

> 🟢 **Live Demo / Deployed Application:** [https://billing-system-pro-p3kp.onrender.com](https://billing-system-pro-p3kp.onrender.com)

BillingSystem Pro is a **robust, multi-tenant enterprise billing platform** developed in Java Spring Boot. Built from the ground up for academic and real-world business use, this system securely manages products, customers, and invoice generation for multiple distinct businesses simultaneously. 

Each business ("tenant") has strictly isolated data, their own admin and staff accounts, and fully customizable dashboard metrics.

---

## ✨ Key Features
- **🔐 Multi-Tenant Architecture:** Strict data isolation between different businesses logging into the same application instance.
- **🛡️ Secure Authentication:** Role-based access control (Admin vs Staff) with an OTP-based password recovery flow.
- **📄 Advanced PDF Generation:** Export beautiful, standardized invoices directly to PDF.
- **📈 Real-Time Dashboard:** Business metrics and KPI reporting (Daily/Monthly revenue, stock levels).
- **📦 Inventory Management:** Add, edit, and track products, complete with dynamic GST calculation logic.
- **👥 Customer Profiles:** Manage a customer database linked strictly to your specific business profile.
- **📚 Automatic API Documentation:** Fully integrated Swagger UI for comprehensive API visibility.

---

## 🛠️ Technology Stack
### Backend
- **Java 21:** Modern language features and optimal performance.
- **Spring Boot 3.2.5:** Core web framework, Spring Security, Spring Data JPA.
- **Hibernate / JPA:** ORM layer with AOP-driven `@Filter` architecture for multi-tenancy.
- **H2 Database / MySQL:** Supports both lightweight in-memory testing and persistent production deployment.
- **iText PDF:** Fast and reliable PDF document generation.
- **Spring Boot Mail:** SMTP integration for password recovery.

### Frontend
- **Vanilla JS & CSS:** Clean, ultra-fast frontend built without heavy reactive frameworks.
- **Dynamic DOM Manipulation:** SPA-like experience using asynchronous `fetch` calls.

---

## 🚀 Setup & Installation Guide

### Prerequisites
Before you begin, ensure you have the following installed on your machine:
- **Java Development Kit (JDK) 21**
- **Maven 3.8+**
- **MySQL Server 8.0+** (Optional, falls back to H2 for local dev)

### 1. Clone the Repository
```bash
git clone https://github.com/rajpriyanshu148/BillingSystemWeb.git
cd BillingSystemWeb
```

### 2. Configure Database & Environment
By default, the application runs using an **in-memory H2 database**, which is perfect for testing. 

If you want to use **MySQL** for production, configure your environment variables or update `src/main/resources/application-prod.properties`:
```properties
DATABASE_URL=jdbc:mysql://localhost:3306/billing_system
DATABASE_USERNAME=root
DATABASE_PASSWORD=yourpassword
```

For **Forgot Password (OTP)** functionality to work, you must configure your SMTP details in `application.properties`:
```properties
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=your_email@gmail.com
SMTP_PASS=your_app_password
```

### 3. Build the Application
Compile the project and download all dependencies using Maven:
```bash
mvn clean install
```

### 4. Run the Application
Run the Spring Boot application using the default dev profile:
```bash
mvn spring-boot:run
```
To run it in **production mode** (MySQL):
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### 5. Access the Application
- **Web UI:** Open your browser and navigate to `http://localhost:8080`
- **Swagger API Docs:** `http://localhost:8080/swagger-ui.html`

*Note: On your first boot, the system will automatically seed demo data for a mock business named "Spice Garden Restaurant" if the database is empty.*

---

## 📂 Folder Structure
```text
BillingSystemWeb/
├── src/main/java/com/billing/
│   ├── config/       # Security, AOP filters, Data Seeding, OpenAPI config
│   ├── controller/   # REST APIs and Page routing
│   ├── model/        # JPA Entities (User, Product, BusinessProfile, etc.)
│   ├── repository/   # Spring Data JPA Interfaces
│   └── service/      # Business logic (Billing, PDF, Emails)
├── src/main/resources/
│   ├── static/       # CSS, JS, HTML templates
│   ├── application.properties       # Default configuration (H2)
│   └── application-prod.properties  # Production configuration (MySQL)
└── pom.xml           # Maven dependencies
```

---

## 🔌 API Endpoints (Quick Reference)
Full interactive documentation is available via **Swagger UI** at `/swagger-ui.html`.

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/auth/forgot-password` | `POST` | Request an OTP for password reset |
| `/api/auth/reset-password` | `POST` | Reset password using the received OTP |
| `/api/info` | `GET` | Retrieve the active tenant's business profile |
| `/api/info` | `PUT` | Update the business profile |
| `/api/dashboard` | `GET` | Get total revenue, bill counts, low stock alerts |
| `/api/products/all` | `GET` | List all products for the tenant |
| `/api/products/save` | `POST` | Create or update a product |
| `/api/bills/generate` | `POST` | Generate a new invoice |
| `/api/bills/{id}/pdf` | `GET` | Download the invoice as a PDF |

---

## 📸 Screenshots
*(Insert placeholders for screenshots below)*

- **Login Screen**  
  `![Login](/docs/login.png)`
- **Admin Dashboard**  
  `![Dashboard](/docs/dashboard.png)`
- **PDF Invoice Preview**  
  `![Invoice](/docs/invoice.png)`

---

## 🔧 Troubleshooting Common Issues
- **`Port 8080 already in use`:** Close the service occupying port 8080 or change `server.port` in `application.properties`.
- **`LazyInitializationException`:** This occurs when JSON serialization touches a lazily-loaded JPA relation. Ensure `@JsonIgnore` is placed on the `businessProfile` mapping in your Entities.
- **`Access Denied (403)`:** Ensure your JWT/Session is active. If you wiped the DB, register a new account via `/register.html`.

---

## 🔮 Future Improvements
- Integrate Stripe/Razorpay for online invoice payment links.
- Add dynamic Chart.js visualizations to the Dashboard.
- Provide Excel/CSV bulk import for Inventory Products.
- Full offline PWA (Progressive Web App) support.

---
*Developed for academic demonstration and enterprise billing solutions.*
