# SmartPharma 💊

**Automated Inventory and Predictive Ordering for Pharmacies**

A web application for independent pharmacies: inventory tracking and machine learning forecast of purchase orders.

## 🚀 Tech Stack

- **Frontend:** React (Vite), Tailwind CSS
- **Backend:** Java 17, Spring Boot, Spring Security + JWT, MySQL
- **ML service:** Python 3, FastAPI, scikit-learn, pandas

## 📁 Project Structure

- `frontend-react/` — user interface
- `backend-java/` — core REST API and business logic
- `ml-service-python/` — predictive analytics microservice

## 🛠️ Running locally

### Backend

```bash
cd backend-java
# Windows: set DB_PASSWORD=your_mysql_password
# macOS/Linux: export DB_PASSWORD=your_mysql_password
mvn spring-boot:run
```

Requires a local MySQL server with a `smartpharma` database. Connection settings live in
`backend-java/src/main/resources/application.properties`; the password defaults to a
placeholder and is overridden by the `DB_PASSWORD` environment variable — never commit a
real password into that file.

### Frontend

```bash
cd frontend-react
npm install
npm run dev
```

Opens on `http://localhost:5173` and expects the backend on `http://localhost:8080`.

## 🎓 Academic Project

National College of Ireland, HDSDEV_SEP25
**Author:** Vitalii Nikitenko (24158852)
