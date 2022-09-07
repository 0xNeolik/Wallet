#!/bin/bash

echo "Writing sample data to DB"
docker run -it --rm -e PGPASSWORD='postgres' -v "$PWD"/../database:/sql --link postgres-db:postgres postgres psql -h postgres -U postgres -f /sql/sample-data.sql
