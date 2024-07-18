#!/bin/sh

DATA_DISK_DEV="/dev/vdb"
BABASHKA_VERSION="1.3.190"
DATOMIC_VERSION="1.0.7075"

# Format persistent disk if it hasn't been already
blkid --match-token TYPE=ext4 "$DATA_DISK_DEV" || mkfs.ext4 -m 0 -F -E lazy_itable_init=0,lazy_journal_init=0,discard "$DATA_DISK_DEV"

# Mount persistent disk, also when it's reattached to a new machine
mkdir -p /data
grep -q "$DATA_DISK_DEV" /etc/fstab || echo "$DATA_DISK_DEV /data ext4 defaults 0 0" >> /etc/fstab
systemctl daemon-reload
mountpoint -q /data || mount /data

# Install babashka
curl -sL "https://github.com/babashka/babashka/releases/download/v$BABASHKA_VERSION/babashka-$BABASHKA_VERSION-linux-amd64.tar.gz" \
  | tar -xz -C /usr/local/bin

# Install packages
apt-get update && apt-get install -yq \
  docker.io \
  openjdk-17-jre \
  netcat-openbsd \
  rlwrap \
  unzip \
  bind9-dnsutils \
  htop \
  tree \
  certbot \
  python3-certbot-nginx \
  nginx \
  jq \
  cron

# Install Datomic
curl -sL "https://datomic-pro-downloads.s3.amazonaws.com/${DATOMIC_VERSION}/datomic-pro-${DATOMIC_VERSION}.zip" > datomic.zip
unzip datomic.zip -d /opt/

# Set up Datomic txor as a systemd unit
cat <<-EOF > /etc/systemd/system/txor.service
[Unit]
Description=Datomic Txor
Wants=network.target
After=network-online.target
After=systemd-resolved.service

[Service]
Restart=always
RestartSec=1
ExecStart=/opt/datomic-pro-1.0.7075/bin/transactor -Xmx -Xmx2g -Xms -Xms2g /data/datomic-postgres.properties

[Install]
WantedBy=multi-user.target
EOF

systemctl enable txor
systemctl start txor

# Configure nginx to proxy to our app
cat <<-'EOF' > /etc/nginx/sites-available/default
server {
    listen       80;
    listen  [::]:80;
    server_name  compass.heartofclojure.eu;

    root /data/public_html;

    # App
    location / {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
     }

    # Static files
    location /_static {
      try_files $uri $uri/ =404;
    }
}
EOF

mkdir -p /data/public_html/_static
systemctl restart nginx

# Install SSH keys of people in the heartofclojure group
cat <<-'EOF' > /usr/local/bin/install_ssh_keys
#!/usr/bin/env bash

# Import SSH keys from Github to ~/.ssh/authorize_keys for all members of a
# given Github organization.
#
# Will replace authorized_keys, if it looks like authorized_keys was not
# previously created by this script then a backup copy is made.
#
# Depends on jq, will download it if not available (assumes Linux) to ~/bin/jq
#
# GITHUB_ORG can be set, defaults to GaiwanTeam
# SSH_DIR and/or KEYS_FILE can be set, default to ~/.ssh and ~/.ssh/authorized_keys
#
# Will create the SSH_DIR if it does not exist, and set permissions on dir and
# file (700 and 600 respectively).
#
# Will exit early if anything goes wrong, so authorized_keys is only touched if
# all Github API/HTTP calls succeed.

GITHUB_ORG=${GITHUB_ORG:-"heartofclojure"}
JQ="$(command -v jq)"

set -ex

# Follow links, no extraneous output, fail (non-zero exit) on non-200 responses
CURL="curl -Ls --fail"

MEMBERS=$($CURL "https://api.github.com/orgs/${GITHUB_ORG}/public_members")

AUTHORIZED_KEYS="# $(date)\n# Created by: ${0}\n# Imported keys for: https://github.com/${GITHUB_ORG}"

for keys_link in $(echo $MEMBERS | "$JQ" -r '.[].html_url+".keys"'); do
    KEYS=$($CURL $keys_link)
    AUTHORIZED_KEYS="${AUTHORIZED_KEYS}\n\n# ${keys_link}\n${KEYS}"
done

SSH_DIR=${SSH_DIR:-"$HOME/.ssh"}
mkdir -p $SSH_DIR
chmod 700 $SSH_DIR

KEYS_FILE=${KEYS_FILE:-"$SSH_DIR/authorized_keys"}

if [[ -f "$KEYS_FILE" ]] && ! grep 'Imported keys for' "$KEYS_FILE" >/dev/null; then
    cp "$KEYS_FILE" "${KEYS_FILE}.$(date +'%Y%m%d_%H%M%S')"
fi

echo -e "$AUTHORIZED_KEYS" > "$KEYS_FILE"
chmod 600 "$KEYS_FILE"
EOF

chmod +x /usr/local/bin/install_ssh_keys
mkdir -p ~/.ssh/
touch ~/.ssh/authorized_keys
chmod 700 ~/.ssh
chmod 600 ~/.ssh/authorized_keys

(crontab -l 2>/dev/null; echo "* */10 * * * /usr/local/bin/install_ssh_keys") | crontab -

# Letsencrypt (SSL cert)
mkdir /data/letsencrypt
ln -s /data/letsencrypt /etc/
echo 'Y' | certbot --nginx -d compass.heartofclojure.eu -m arne@gaiwan.co

cat <<-EOF > /etc/motd
Welcome to the Compass server

   https://compass.heartofclojure.eu

Services

   systemctl status txor
   systemctl status compass

Locations

   Bare git repo: /home/compass/repo
   Checkouts:     /home/compass/app
   Config:        /home/compass/config.env

EOF

EOF

# Set up app, run as low privilege user
useradd -m -s /bin/bash compass
sudo -u compass git clone --bare https://github.com/GaiwanTeam/compass /home/compass/repo
sudo -u compass mkdir /home/compass/app
SHA="$(sudo -u compass git -C /home/compass/repo rev-parse HEAD)"
sudo -u compass echo git -C /home/compass/repo checkout HEAD --work-tree=/home/compass/app/"$SHA"
sudo -u compass ln -sf /home/compass/app/"$SHA" /home/compass/app/current
sudo -u compass ln -sf /home/compass/app/current/pre-receive.bb /home/compass/repo/hooks/pre-receive
