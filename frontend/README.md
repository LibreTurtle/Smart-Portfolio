# Smart Portfolio Frontend

Astro + TypeScript frontend for the Smart Portfolio backend. The project is compatible with Node.js and npm for local development and CI.

## Prerequisites

- Node.js >= 22.12
- npm
- Go backend running on `http://localhost:8080` (see [`../backend/README.md`](../backend/README.md))

## Setup

```bash
cp .env.example .env
npm ci
```

## Development

```bash
npm run dev
```

Opens at `http://localhost:5173`. API calls are proxied to `http://localhost:8080`.

## Type Checking

```bash
npm run check
```

## Production Build

```bash
npm run build
npm run preview
```

## Configuration

| Variable | Default | Description |
|---|---|---|
| `PUBLIC_API_URL` | `""` (proxied) | Backend API base URL for production |
| `PUBLIC_DEV_API_PROXY` | `http://localhost:8080` | Dev-only proxy target for `/api` requests |
| `FRONTEND_PORT` | `5173` | Astro dev server port |

Create `.env` for overrides:

```env
PUBLIC_API_URL=https://api.example.com
PUBLIC_DEV_API_PROXY=http://localhost:8080
```

## Deployment

- Set `PUBLIC_API_URL` to your deployed backend origin, for example `https://api.example.com`
- The frontend is static, so you can deploy `dist/` to any static host after:

```bash
npm run build
```

## Architecture

- **Astro** — static-first framework with island architecture
- **TypeScript (strict)** — all code is strictly typed
- **Node.js + npm** — runtime and package manager used by CI
- Content is dynamically loaded from the backend AI chat endpoint, reflecting the admin-ingested resume
