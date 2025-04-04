#!/bin/bash

CONTAINER_ID=$1
PORT=$2

if [ -z "$CONTAINER_ID" ] || [ -z "$PORT" ]; then
  echo "Usage: $0 <container-id> <host-port>"
  exit 1
fi

docker rm -f ttyd-proxy-$PORT 2>/dev/null

docker run -d \
  --name ttyd-proxy-$PORT \
  -p $PORT:7681 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  --env TERM=xterm-256color \
  my-ttyd-docker \
  ttyd --writable docker exec -it "$CONTAINER_ID" bash
