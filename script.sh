#!/bin/bash

set -e

while true; do
  DBNAME="db-$(openssl rand -hex 8)"

  echo ""
  echo ""
  echo "Sending root request to create database to coordinator2: $DBNAME"
  curl -u root:test "http://coordinator2:8529/_api/database" -d "{\"name\": \"$DBNAME\", \"users\": [{\"username\": \"user\"}]}"

  echo ""
  echo ""
  echo "Sending user request to create collection to coordinator1 ..."
  curl -f -u user:test "http://coordinator1:8529/_db/$DBNAME/_api/collection" -d "{\"name\": \"myCol\"}"
done
