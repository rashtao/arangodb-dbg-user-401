#!/bin/bash

COORDINATOR1="coordinator1:8529"
COORDINATOR2="coordinator2:8529"

## create db granting user permissions
create_db() {
  echo ""
  echo ""
  echo "Sending root request to create database to coordinator2: $DBNAME"
  curl -u root:test "http://$1/_api/database" -d "{\"name\": \"$DBNAME\", \"users\": [{\"username\": \"user\"}]}"
}

## create collection with user credentials
create_collection() {
  echo ""
  echo ""
  echo "Sending user request to create collection to coordinator1 ..."
  curl -f -u user:test "http://$1/_db/$DBNAME/_api/collection" -d "{\"name\": \"myCol\"}"
}

## if previous command failed, retry creating collection with user credentials after 5s
retry_create_collection() {
  if [ $? -ne 0 ]; then
    echo ""
    echo ""
    echo "retrying in 5 seconds..."
    sleep 5
    create_collection $1
  fi
}

# create user
curl -u root:test "http://$COORDINATOR2/_api/user" -d '{"user": "user", "passwd": "test"}'

while true; do

  echo ""
  echo "---------"
  DBNAME="db-$(openssl rand -hex 8)"

  create_db $COORDINATOR2
  create_collection $COORDINATOR1
  retry_create_collection $COORDINATOR1
  if [ $? -ne 0 ]; then
    echo ""
    echo ""
    echo "Cannot create collection on $COORDINATOR1, trying on $COORDINATOR2 now ..."
    create_collection $COORDINATOR2
    if [ $? -ne 0 ]; then
      echo "Cannot create collection neither on $COORDINATOR1 nor on $COORDINATOR2, giving up..."
      echo "dbname for further dbg: $DBNAME"
      exit 1
    fi
  fi
done
