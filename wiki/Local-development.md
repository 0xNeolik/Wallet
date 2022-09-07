## Local development:

**Download the repo**

**Install docker**

It can be downloaded from [Here](https://store.docker.com/editions/community/docker-ce-desktop-mac)

**Create a new docker container with the PostgreSQL image**

```
docker run --name postgres-db -p 5432:5432 -e POSTGRES_PASSWORD=postgres -e POSTGRES_USER=postgres -d postgres
```

**Add tables to PostgreSQL**

The tables located at `database/tables.sql` have to be added.

PostgreSQL terminal can be accessed using the following command:

```
docker run -it --rm -e PGPASSWORD="postgres" --link postgres-db:postgres postgres psql -h postgres -U postgres
```

**Twitter callback**

In addition, the twitter callbackURL has to point to localhost in the `silhouette.conf` file.

```
callbackURL = "http://localhost:9000/v1/mobileauth/twitter"
```

**Start the app**

Go to the web folder and launch the following command:

```
sbt run
```