
# Prepare postgresql db/table

cat <<-EOF | sudo -u postgres psql
CREATE ROLE datomic LOGIN PASSWORD 'datomic';

CREATE DATABASE datomic
 WITH owner = datomic
      TEMPLATE template0
      ENCODING = 'UTF8'
      LC_COLLATE = 'en_US.UTF-8'
      LC_CTYPE = 'en_US.UTF-8'
      CONNECTION LIMIT = -1;

\c datomic

CREATE TABLE datomic_kvs
(
 id text NOT NULL,
 rev integer,
 map text,
 val bytea,
 CONSTRAINT pk_id PRIMARY KEY (id )
);

GRANT ALL ON TABLE datomic_kvs TO datomic;

EOF
