# ============================================================
# VOLTARAS Frontend — production image
#
# Stage 1: build the Vite app (npm ci + npm run build)
# Stage 2: serve dist/ with nginx (SPA fallback + /api proxy)
# ============================================================

FROM node:22-alpine AS build

WORKDIR /app

# Install dependencies from the lockfile first (better layer caching).
COPY package.json package-lock.json ./
RUN npm ci

# Copy the rest of the source.
COPY . .

# Same-origin API base: the SPA requests "/api/..." on its own origin and
# nginx proxies those to the api-gateway container. An empty string keeps the
# existing "/api/..." prefixes in the frontend service calls intact.
ARG VITE_API_BASE_URL=
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build

# ----------------------------------------------------------------------------

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
