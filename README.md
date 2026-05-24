# Gmail Excel Exporter

Spring Boot application that reads Gmail messages for a selected date range and downloads them as an Excel file.

## Import Into Eclipse

1. Open Eclipse.
2. Go to `File` > `Import`.
3. Select `Maven` > `Existing Maven Projects`.
4. Click `Next`.
5. Browse to this project folder:

   ```text
   C:\Users\DELL\Documents\Codex\2026-05-23\we-have-emails-from-clients-received
   ```

6. Select the `pom.xml`.
7. Click `Finish`.
8. Wait for Eclipse to download Maven dependencies.

## PostgreSQL Setup

Create a PostgreSQL database named:

```sql
CREATE DATABASE gmail_exporter;
```

Update credentials in:

```text
src/main/resources/application.properties
```

Current default values:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5431/gmail_exporter?sslmode=disable
spring.datasource.username=postgres
spring.datasource.password=your_postgresql_password
```

If Eclipse shows `Unable to open JDBC Connection` or `The connection attempt failed`, check:

1. PostgreSQL service is running.
2. Database `gmail_exporter` exists.
3. Username and password in `application.properties` match your PostgreSQL login.
4. PostgreSQL is listening on the same port used in the JDBC URL.
5. For a local PostgreSQL setup, keep `?sslmode=disable` at the end of the JDBC URL.

## Gmail Setup

1. Open Google Cloud Console.
2. Enable Gmail API.
3. Create OAuth Client ID credentials for a Desktop app.
4. Download the JSON credentials file.
5. Rename it to:

   ```text
   credentials.json
   ```

6. Place it here:

   ```text
   src/main/resources/credentials.json
   ```

The Gmail properties in `application.properties` should be:

```properties
gmail.credentials-path=classpath:credentials.json
gmail.tokens-directory=tokens
gmail.application-name=Gmail Excel Exporter
```

## Run In Eclipse

1. Open `GmailExcelExporterApplication.java`.
2. Right-click the file.
3. Select `Run As` > `Spring Boot App`.

If `Spring Boot App` is not shown, use:

```text
Run As > Java Application
```

## Export Emails API

Send a POST request:

```http
POST http://localhost:8080/api/emails/export
Content-Type: application/json
```

Request body:

```json
{
  "startDate": "2026-05-01",
  "endDate": "2026-05-23"
}
```

The response downloads an Excel `.xlsx` file.

## View Export Logs

```http
GET http://localhost:8080/api/email-export-logs
```
