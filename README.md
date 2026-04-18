# Project Setup Instructions

Follow these steps to get the backend server up and running for local development.

## 1. Configure Environment Variables

The project uses a `.env` file for managing environment-specific configurations, such as database credentials.

First, locate the example environment file, `.env.example`, in the root of the project. Make a copy of this file and name it `.env`:

```bash
# For Windows (Command Prompt)
copy .env.example .env.example

# For Windows (PowerShell)
cp .env.example .env.example

# For Linux / macOS
cp .env.example .env.example
```

Next, open the newly created `.env` file and modify the database connection variables (`DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`, etc.) to match your local database setup.

**Example `.env` configuration:**
```env
DB_URL=localhost
DB_PORT=3306
DB_USER=your_username
DB_PASSWORD=your_password
DB_NAME=your_database
```

## 2. Run the Application

Once the environment variables and database are configured, you can start the application using Maven:

```bash
./mvnw spring-boot:run
```
Execute the `sql.sql` script located in the project's root directory. This will create the necessary tables and data.
The server should now be running locally.