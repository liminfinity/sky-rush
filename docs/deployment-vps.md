# Ubuntu VPS deployment

This is a single-server hackathon deployment, not a production identity/payment platform. Use Ubuntu 24.04 LTS (22.04 also supported), a non-root sudo user, and preferably 2 vCPU / 4 GB RAM for image builds. Only Docker runs Java, Node and PostgreSQL.

## First deployment

1. Create an **A record** `play.example.com → VPS_PUBLIC_IP`. Replace the example with your domain. Only create an AAAA record if IPv6 reaches this VPS. Allow incoming TCP **80, 443** and your existing SSH port (normally **22**) in the provider firewall. DNS must resolve directly to this VPS; the forwarded-header trust model assumes Caddy is the public edge.
2. SSH in as your sudo user and install Git if needed:

   ```sh
   sudo apt-get update
   sudo apt-get install -y git
   sudo install -d -o "$USER" -g "$(id -gn)" /opt/sky-rush
   git clone https://github.com/liminfinity/sky-rush.git /opt/sky-rush
   cd /opt/sky-rush
   sudo ./scripts/vps/bootstrap-ubuntu.sh
   sudo usermod -aG docker "$USER"
   ```

   Disconnect and reconnect over SSH so Docker group membership applies. Docker group membership is equivalent to root access. Bootstrap uses Docker's official apt repository, leaves an existing Docker installation alone, and does not change SSH or enable a firewall. If using UFW, allow your **actual SSH port first**, then 80/tcp and 443/tcp before enabling it. Never replace an existing firewall ruleset blindly. Docker-published ports can bypass UFW; this stack publishes only 80/443.
3. Prepare the ignored environment file:

   ```sh
   cd /opt/sky-rush
   cp .env.prod.example .env.prod
   chmod 600 .env.prod
   openssl rand -hex 32
   id -u
   id -g
   nano .env.prod
   ```

   Set `SKYRUSH_DOMAIN` to the hostname only, `POSTGRES_PASSWORD` to the generated random value, and `LOCAL_UID`/`LOCAL_GID` to the two IDs printed above. Keep database name/user stable. Do not reuse the example password. No `SESSION_SECRET` or `prod` Spring profile is needed: sessions already live on the server, and Compose sets the actual application variables.
4. Deploy:

   ```sh
   ./scripts/vps/deploy.sh
   ```

   This checks for a clean Git tree, pulls **fast-forward only**, seeds `runtime/config/` from `config/` only when missing, builds images, validates Caddy, waits for all health checks, and verifies HTTPS. It never deletes volumes. An invalid startup stops the application services to avoid an unattended restart loop; PostgreSQL remains intact. Fix the logged problem and rerun the script. Certificate issuance can take a few minutes; if only public verification times out, check DNS/ports and rerun `verify-deployment.sh`.
5. Open `https://YOUR_DOMAIN`. Login **demo / demo12345**, or register a separate player. These credentials intentionally grant public evaluator/admin access. They are not suitable for a private production admin. Do not collect sensitive information or operate real-money gameplay.

After the initial preparation, equivalent manual Compose commands are:

```sh
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml up --build -d --wait
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml ps
./scripts/vps/verify-deployment.sh
```

Compose **2.24.4+** is required for port/volume overrides. Use this exact file combination and project name `sky-rush-prod` consistently. Do not mix it with local/demo/E2E overrides.

## Routing and security

Internet HTTPS → Caddy → nginx:5173 → frontend or backend:8080 → PostgreSQL:5432. Only Caddy binds host ports **80/443 TCP**. nginx, backend, PostgreSQL and Caddy's admin API have no host bindings. Caddy obtains and renews public certificates automatically; its `/data` and `/config` use persistent named volumes. UDP 443/HTTP3 is intentionally not published; HTTPS HTTP/1.1 and HTTP/2 work normally.

The browser uses relative `/api`. Caddy replaces untrusted client forwarding headers; nginx normalizes protocol/host and removes `Forwarded`; only the internal backend enables Spring's forwarded-header processing. Do not expose nginx/backend directly or put an additional proxy/CDN in front without reviewing the trust chain. Cookies stay **HttpOnly, Secure, SameSite=Lax, Path=/**. CSRF remains enabled on all writes, including login/register. Sessions expire after two idle hours and on backend restart; persisted accounts and progress survive.

Production nginx limits **POST login/register** to 10 requests/minute/IP with a burst of 10 (HTTP 429 on excess). Shared-NAT users share this allowance. The key is the client address supplied by Caddy, not browser-supplied identity. Other requests, including game polling, are not rate limited. Requests have a 64 KiB body limit. Caddy adds HSTS (one year, no subdomain/preload policy), nosniff, SAMEORIGIN framing and restricted device permissions. No file server or directory listing is exposed beyond the frontend's static assets.

Swagger is intentionally public at `/swagger-ui.html` and `/v3/api-docs` for judges. Only safe Actuator **health** is exposed at `/actuator/health`; env/config/debug endpoints are not enabled. `/api/admin/**` still requires the evaluator role; ordinary accounts receive 403. Public demo credentials mean anyone can become that evaluator: restrict sharing/access operationally if this is unsuitable. Rate limiting is basic abuse protection, not comprehensive account security.

## Updates, configuration and logs

```sh
cd /opt/sky-rush
./scripts/vps/backup-db.sh
./scripts/vps/deploy.sh
```

Deployment is manual, with no Actions/SSH deployment. The Git checkout must be clean; `.env.prod`, `runtime/` and `backups/` are ignored. Admin edits and manual YAML changes use **runtime/config/**, not the tracked defaults. These files are not overwritten on update. Review new default fields in Git when upgrading and merge them into runtime configuration deliberately. Active rounds keep their existing snapshots.

All services use `unless-stopped` for reboot recovery. Health checks report failures; they do not automatically repair unhealthy processes. Logs rotate at 10 MB × 3 files per container. To inspect them:

```sh
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml logs -f --tail 100 backend
# Replace backend with frontend, caddy or postgres.
```

Ordinary `down` followed by `up -d --wait` retains database/certificate volumes. **Never use `down -v` for deployment**: it deletes accounts, rounds and certificates. Changing POSTGRES_PASSWORD in the file does not rotate an existing database password; PostgreSQL initialization variables only initialize an empty volume. Coordinate database password rotation separately. This is one application instance with short redeploy downtime, not zero-downtime/high-availability hosting.

## Backup and restore

`./scripts/vps/backup-db.sh` creates a consistent custom-format `pg_dump` at `backups/skyrush-UTC_TIMESTAMP-PID.dump`, with owner-only permissions. A partial file is never advertised as a completed backup. Copy backups securely off the VPS; local disk copies do not survive VPS loss. Also securely back up `.env.prod` and `runtime/config/`. Database dumps do not contain TLS certificates or runtime YAML.

Restore is deliberately manual and destructive to the target database contents. Verify the selected file and take a fresh backup first. Use the same database/user as the dump; stop application writes:

```sh
cd /opt/sky-rush
./scripts/vps/backup-db.sh
# Choose the actual backup filename, never a wildcard.
backup=backups/skyrush-YYYYMMDDTHHMMSSZ-PID.dump
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml stop caddy frontend backend
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml exec -T postgres \
  sh -c 'pg_restore --exit-on-error --clean --if-exists --no-owner -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < "$backup"
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml up -d --wait
./scripts/vps/verify-deployment.sh
```

If restore fails, keep the application stopped and investigate; do not resume on a partially restored database. Test restores periodically. Never restore an older-schema backup into older application code without checking Flyway compatibility.

## Browser acceptance

Use the same HTTPS domain in normal and incognito windows. Register two distinct accounts. On both, check separate balances/history, play and cash out, then observe shared ranking, online presence and the real activity feed. Open profile/collection, daily progress and achievements. Reload during flight. Confirm an ordinary account cannot open admin, and demo can. Verify successful logout and re-login. Do not run fixture-generating E2E against a public player database; `scripts/run-e2e.sh` creates an isolated stack.

## Verification scope

See [VPS deployment verification](acceptance/vps.md) for actual local results. No VPS/domain credentials are assumed; local validation of Caddy with a local CA is **not** proof of public DNS, Let's Encrypt issuance or a deployed VPS.

References: [Docker Ubuntu installation](https://docs.docker.com/engine/install/ubuntu/), [Caddy reverse proxy and forwarded headers](https://caddyserver.com/docs/caddyfile/directives/reverse_proxy), [Caddy request limits](https://caddyserver.com/docs/caddyfile/directives/request_body).
