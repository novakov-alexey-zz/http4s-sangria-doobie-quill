## How to run

Prerequisites:

- Docker
- JDK
- SBT

### Step 1. Run Database

First start local database in Docker by running:

```bash
sh start-dev-db.sh
```

### Step 2. Run API Server

Run below command, then choose 1 or 2 to run Cats or Monix IO Monad implementation. 

```bash
sbt run
```

### Step 3. Access API

#### Option 1.

Open your browser at http://localhost:8080/playground.html and run test query:

```text
{
  news {
    title
    link
  }
}
```

#### Option 2.

Use any HTTP client to send POST request with JSON payload of GraphQL request.
Example using curl tool: 

```bash
curl -XPOST -H "Content-Type: application/json" http://localhost:8080/graphql \
 -d '{"operationName":null,"variables":{},"query":"{news {title link }}"}'
```

## Run Tests

There are a couple non-unit tests using Docker container for Postgres. Run them:
```bash
sbt test
``` 