# Backend Starter

This folder contains the optional backend starter for Pupumap.

The backend is currently used for local development and future map API experiments. It provides a simple Go-based HTTP API foundation for map-related features.

---

## Purpose

This backend can be used for:

* Health check API
* Sample places API
* OpenStreetMap / PostGIS data experiments
* Vector tile endpoint experiments
* Map style serving
* Local backend testing
* Future map data APIs

---

## Tech Stack

* Go
* HTTP API
* PostgreSQL
* PostGIS
* MapLibre vector tile workflow
* OpenStreetMap data through `osm2pgsql`

---

## Environment Variables

Create a local `.env` file for your real development values.

Do not commit the real `.env` file to GitHub.

Use `.env.example` as the public safe template.

Example:

```env
SERVER_PORT=8080
APP_ENV=development

DB_HOST=localhost
DB_PORT=5433
DB_NAME=pupumap
DB_USER=postgres
DB_PASSWORD=change_me
DB_SSLMODE=disable
```

---

## Running Locally

First, navigate to the backend project folder:

```bash
cd pupumap-api
```

Then install dependencies:

```bash
go mod tidy
```

After that, run the API server:

```bash
go run ./cmd/api
```

The backend should start at:

```text
http://localhost:8080
```

---

## Example Endpoints

### Health Check

```http
GET /health
```

Example response:

```json
{
  "service": "pupumap-api",
  "status": "ok"
}
```

### Places

```http
GET /places
```

### OSM Places

```http
GET /osm/places
```

### Search

```http
GET /api/search?q=hospital
```

### Vector Tiles

```http
GET /tiles-vector/{z}/{x}/{y}.pbf
```

### Style JSON

```http
GET /styles/style.json
```

---

## Android Device Note

If running the Android app on a real phone, `localhost` inside the app will point to the phone itself, not your computer.

For real Android device testing, replace `localhost` in the style JSON or Android config with your development machine LAN IP, for example:

```text
http://192.168.x.x:8080
```

For Android emulator testing, you may need:

```text
http://10.0.2.2:8080
```

---

## Safety Notes

Before making this repository public, make sure the backend does not contain:

* Real `.env` files
* Database passwords
* API keys
* JWT secrets
* SSH keys
* Private server IPs
* Personal location data
* Private map data
* Large raw OSM dump files

Only commit safe example files such as:

```text
.env.example
```

Never commit:

```text
.env
```

---

## Status

This backend is experimental and intended for local development, learning, and open-source map API experiments.

It is not a production-ready backend yet.
