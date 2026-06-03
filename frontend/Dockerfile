# =============================================================================
# Production Dockerfile for Vue.js (Vite) — Multi-stage build
# =============================================================================
#   1. Build Stage: Compiles the Vue application using Node.js
#   2. Runtime Stage: Serves the static assets via Nginx
# =============================================================================

# ---- Stage 1: Build ---------------------------------------------------------
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package configurations first to optimize npm caching
COPY package.json package-lock.json ./
RUN npm ci

# Define build arguments. Vite embeds VITE_ environment variables at build-time.
# You can pass this during build e.g. --build-arg VITE_API_BASE_URL=https://api.yourdomain.com
ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# Copy the rest of the frontend source files
COPY . .

# Build the application for production (results are generated in /app/dist)
RUN npm run build

# ---- Stage 2: Runtime -------------------------------------------------------
FROM nginx:1.25-alpine AS runtime

# Copy the custom Nginx config to support client-side routing (Vue Router) and performance
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy static assets from the builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Expose HTTP port
EXPOSE 80

# Standard healthcheck to verify Nginx is active
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
    CMD wget -qO- http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
