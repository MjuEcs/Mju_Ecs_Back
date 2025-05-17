#!/bin/bash

CONTAINER_ID=$1       # 사용자가 접근할 컨테이너 ID
PORT=$2               # 사용자가 접근할 포트
USER=$3
PASSWORD=$4

if [ -z "$CONTAINER_ID" ] || [ -z "$PORT" ]; then
  echo "Usage: $0 <container-id> <host-port>"
  exit 1
fi

docker rm -f ttyd-proxy-$PORT 2>/dev/null

docker run -d \
  --name ttyd-proxy-$PORT \
  -p $PORT:7681 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  --env LANG=ko_KR.UTF-8 \
  --env LC_ALL=ko_KR.UTF-8 \
  --env TERM=xterm-256color \
  my-ttyd-docker \
  ttyd \
    --writable \
    --credential $USER:$PASSWORD \
    env LANG=ko_KR.UTF-8 LC_ALL=ko_KR.UTF-8 TERM=xterm-256color docker exec -it "$CONTAINER_ID" bash
