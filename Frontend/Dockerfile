# =============================================================================
# MULTI-STAGE DOCKERFILE — React (Vite) + Nginx
#
# Stage 1 (builder): Node 20 — installs dependencies and builds the React app.
#   Result: static HTML/CSS/JS in /app/dist
#
# Stage 2 (runtime): Nginx Alpine — serves the static files.
#   Result: a ~50MB image (vs ~800MB with Node + node_modules).
#
# Why Nginx for a React app?
#   React (Vite) is NOT a server — 'npm run build' produces static files.
#   We need a web server to serve them. Nginx is the standard choice:
#   - Extremely fast at serving static files (event-driven, no threads)
#   - Can proxy /api/ requests to our Spring Boot backend
#   - Handles SPA routing (serve index.html for all unknown paths)
#   - Adds gzip compression, cache headers, and security headers
# =============================================================================

# ── STAGE 1: BUILD ──────────────────────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files BEFORE source code.
# Docker layer caching: if package.json and package-lock.json haven't changed,
# the 'npm ci' layer is reused — no re-downloading 800MB of node_modules.
# This is the most important optimization for frontend Docker builds.
COPY package.json package-lock.json ./

# npm ci vs npm install:
#   npm ci: reads package-lock.json exactly, never updates it.
#           Fails if lock file is out of sync. Reproducible. Use in CI/Docker.
#   npm install: may update package-lock.json. Use during development.
RUN npm ci

# Copy source files and run the build.
# Vite output lands in /app/dist
COPY . .
RUN npm run build

# ── STAGE 2: RUNTIME ────────────────────────────────────────────────────────
FROM nginx:1.27-alpine AS runtime

# Remove Nginx's default placeholder page (the "Welcome to nginx!" page)
RUN rm -rf /usr/share/nginx/html/*

# Copy only the compiled dist/ output from the builder stage.
# No Node.js, no source code, no node_modules in the final image.
COPY --from=builder /app/dist /usr/share/nginx/html

# Replace Nginx's default config with our custom config.
# Our config handles:
#   - SPA routing (try_files → /index.html fallback)
#   - API reverse proxy (/api/ → backend:8080)
#   - Gzip compression
#   - Static asset caching
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

# daemon off: required for Docker — keeps Nginx as PID 1 in the foreground.
# If Nginx daemonizes, the container exits immediately (no foreground process).
CMD ["nginx", "-g", "daemon off;"]
