#!/bin/bash

echo "Writing table schema to DB"
docker run -it --rm -e PGPASSWORD='postgres' -v "$PWD"/../database:/sql --link postgres-db:postgres postgres psql -h postgres -U postgres -f /sql/tables.sql
