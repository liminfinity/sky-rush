#!/usr/bin/env bash
set -euo pipefail
[[ $(id -u) = 0 ]] || { echo 'Run with sudo on the Ubuntu VPS.' >&2; exit 1; }
[[ -r /etc/os-release ]] || { echo 'Ubuntu 22.04/24.04 LTS required.' >&2; exit 1; }
source /etc/os-release
[[ "$ID" = ubuntu && ( "$VERSION_ID" = 22.04 || "$VERSION_ID" = 24.04 ) ]] || {
  echo 'This bootstrap supports Ubuntu 22.04 and 24.04 LTS only.' >&2; exit 1;
}
apt-get update
apt-get install -y ca-certificates curl git python3 openssl
if ! command -v docker >/dev/null; then
  for package in docker.io docker-compose docker-compose-v2 podman-docker containerd runc; do
    if dpkg-query -W -f='${Status}' "$package" 2>/dev/null | grep -q 'install ok installed'; then
      echo "Existing $package detected. Resolve Docker package conflicts using the official Docker guide first." >&2
      exit 1
    fi
  done
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc
  cat > /etc/apt/sources.list.d/docker.sources <<REPO
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $VERSION_CODENAME
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
REPO
  apt-get update
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi
docker compose version
systemctl enable --now docker
printf '%s\n' 'Docker ready. SSH settings and firewall were not modified.' \
  'Allow TCP 80/443 and your existing SSH port (normally 22) in the VPS firewall.' \
  'Use a non-root deployment user. Docker group membership grants root-equivalent access.' \
  'If needed: sudo usermod -aG docker YOUR_USER, then reconnect over SSH.'
