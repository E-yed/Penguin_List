# Penguin List

Penguin List is a small Spring Boot web app for managing todo items for yesterday, today, and tomorrow. It serves a browser UI at the root URL and also exposes the same data as JSON through the API.

## What it uses

- Spring Boot 3
- Java 21
- Maven Wrapper
- SQLite

## How to run it

From the project root, start the app with the Maven wrapper:

```bash
./mvnw spring-boot:run
```

On Windows `cmd`, use:

```bat
mvnw.cmd spring-boot:run
```

If you already have Maven installed globally, this also works:

```bash
mvn spring-boot:run
```

## How to open it

Once Spring Boot finishes starting, open:

- `http://localhost:8080/` for the web app
- `http://localhost:8080/api` for the JSON API

You can also visit these app routes directly:

- `http://localhost:8080/today`
- `http://localhost:8080/yesterday`
- `http://localhost:8080/tomorrow`

## Notes

- The app creates and uses a local SQLite database file named `penguin-list.db`.
- The schema is initialized automatically on startup.
