#!/bin/sh

DATA_DISK_DEV="/dev/vdb"
BABASHKA_VERSION="1.3.191"
DATOMIC_VERSION="1.0.7180"

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
  openjdk-17-jre \
  docker.io \
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
  cron \
  tmux \
  locales-all \
  ed

# Install Clojure
curl -sL https://github.com/clojure/brew-install/releases/latest/download/linux-install.sh | bash

# Install Datomic
curl -sL "https://datomic-pro-downloads.s3.amazonaws.com/${DATOMIC_VERSION}/datomic-pro-${DATOMIC_VERSION}.zip" > datomic.zip
unzip datomic.zip -d /opt/
ln -sf /opt/datomic-pro-${DATOMIC_VERSION} /opt/datomic

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
ExecStart=/opt/datomic/bin/transactor -Xmx -Xmx2g -Xms -Xms2g /data/datomic-postgres.properties

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

    location /uploads/ {
      alias /home/compass/uploads/;
    }

    # App
    location / {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
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

# More helpful login message
cat <<-EOF > /etc/motd

⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣀⠀⠀⠀⠀⠀⠀⣀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢹⣄⠀⠀⠀⠀⣠⡟⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⣤⡀⠀⢀⣤⣶⣿⣿⣿⣿⣿⣿⣿⣿⣶⣤⡀⣀⣤⣶⡟⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠈⣻⣾⣿⣿⣿⡿⠟⠛⠛⠛⠛⠻⢿⣿⣿⣿⡿⣻⡟⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⣴⣿⣿⣿⠟⠁⠀⠀⠀⠀⢀⣠⣴⣿⣿⡿⠋⣼⣿⣦⠀⠀⠀⠀⠀
⠀⢠⣄⣀⣼⣿⣿⡿⠁⠀⠀⠀⣀⣤⣾⣿⣿⣿⡿⠋⢀⣼⢿⣿⣿⣧⣀⣠⡄⠀
⠀⠀⠀⠙⣿⣿⣿⠁⠀⠀⠀⣼⠛⢿⣿⣿⡿⠋⠀⢀⡾⠃⠈⣿⣿⣿⠋⠀⠀⠀
⠀⠀⠀⠀⣿⣿⣿⠀⠀⢀⣾⠃⠀⠀⢙⡋⠀⠀⢠⡿⠁⠀⠀⣿⣿⣿⠀⠀⠀⠀
⠀⠀⠀⣠⣿⣿⣿⡀⢀⡾⠁⠀⢀⣴⣿⣿⣦⣠⡟⠁⠀⠀⢀⣿⣿⣿⣄⠀⠀⠀
⠀⠘⠋⠉⢻⣿⣿⣷⡿⠁⢀⣴⣿⣿⣿⡿⠟⠋⠀⠀⠀⢀⣾⣿⣿⡟⠉⠙⠃⠀
⠀⠀⠀⠀⠀⢻⣿⡟⢀⣴⣿⣿⠿⠋⠁⠀⠀⠀⠀⢀⣴⣿⣿⣿⡟⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⣼⢟⣴⣿⣿⣿⣷⣦⣤⣤⣤⣤⣴⣶⣿⣿⣿⡿⣯⡀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⣼⠿⠛⠉⠉⠛⠿⣿⣿⣿⣿⣿⣿⣿⣿⠿⠛⠉⠀⠈⠛⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣸⠋⠀⠉⠉⠀⠙⣧⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠉⠀⠀⠀⠀⠀⠀⠉⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀

   C · O · M · P · A · S · S
https://compass.heartofclojure.eu

-= Services =-

   systemctl status txor
   systemctl status compass

-= Logs =-

   journalctl -u compass --follow
   journalctl -u compass --since '10 minutes ago'

-= REPL =-

   rlwrap nc localhost 5555

-= Locations =-

   Bare git repo: /home/compass/repo
   Checkouts:     /home/compass/app
   Config:        /home/compass/config.edn
   Nginx config:  /etc/nginx/sites-enabled/default

-= Database =-

   Exoscale Hosted: hoc-compass-db-exoscale-35f30fc9-004b-4593-a18d-533dfbe262d7.e.aivencloud.com:21699

EOF

# Set up app, run as low privilege user
if [[ ! -f /home/compass/config.edn ]]; then
  echo "{}" > /home/compass/config.edn
fi

cat <<-EOF > /etc/systemd/system/compass.service
[Unit]
Description=Compass Clojure App
Wants=network.target
After=network-online.target
After=txor.service

[Service]
Restart=always
RestartSec=1
WorkingDirectory=/home/compass/app/current
ExecStart=/usr/local/bin/clojure -J-Dclojure.main.report=stderr -J-Dclojure.server.repl='{:port 5555 :accept clojure.core.server/repl}' -A:prod -M -m co.gaiwan.compass run --env prod --config /home/compass/config.edn
User=compass

[Install]
WantedBy=multi-user.target
EOF

systemctl enable compass

useradd -m -s /bin/bash compass
sudo -u compass git clone --bare https://github.com/GaiwanTeam/compass /home/compass/repo
sudo -u compass mkdir /home/compass/app
sudo -u compass git -C /home/compass/repo cat-file blob HEAD:ops/pre-receive.bb > /tmp/pre-receive.bb

SHA="$(sudo -u compass git -C /home/compass/repo rev-parse HEAD)"
pushd /home/compass/repo
sudo -u compass bb /tmp/pre-receive.bb <<< "000 $SHA refs/heads/main"
popd
sudo -u compass ln -sf /home/compass/app/current/ops/pre-receive.bb /home/compass/repo/hooks/pre-receive
