Machine:

- hoc-compass running on Exoscale
  Debian bookwork 12
  Standard Large = 8GB RAM / 4 CPUs
  Disk 50GB
  Block storage 100GB mounted at /data
- Transactor/nginx/etc, see cloud_init.sh
- A / AAAA records on DNSimple for compass.heartofclojure.eu
  159.100.242.109
  2a04:c43:e00:5d3b:4f6:2cff:fe00:612

Database
- We use a exoscale hosted postgresql instance as a backend for datomic
- Transactor runs on the same box
  
ssh accesss:
- Make sure your membership of the heartofclojure group is public on github
https://github.com/orgs/heartofclojure/people
- Wait up to ten minutes

```
ssh root@compass.heartofclojure.eu
systemctl status txor
systemctl status compass
journalctl -u compass -f
```

```
ssh compass@compass.heartofclojure.eu
vim ~/config.edn
```

Deploys
- we have a bare repo on the server
- in there there's a pre-receive hook (ops/pre-receive.bb)
- pushed to this repo will trigger a deploy
- the github actions does a push
