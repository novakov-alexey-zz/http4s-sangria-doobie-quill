#!/usr/bin/env bash
docker run -d \
  -it \
  --name postgres \
  --mount type=bind,source="$(pwd)"/init,target=/docker-entrypoint-initdb.d,readonly \
  -p 5432:5432 \
  -e POSTGRES_PASSWORD=admin \
  -e POSTGRES_USER=admin \
  -e POSTGRES_DB=news \
  postgres
