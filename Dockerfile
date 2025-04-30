# Dockerfile
FROM tsl0922/ttyd:latest

# 한글 locale 및 docker CLI 설치
RUN apt-get update && \
    apt-get install -y docker.io locales && \
    locale-gen ko_KR.UTF-8 && \
    update-locale LANG=ko_KR.UTF-8 LC_ALL=ko_KR.UTF-8 && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 기본 locale 설정
ENV LANG=ko_KR.UTF-8
ENV LC_ALL=ko_KR.UTF-8
ENV TERM=xterm-256color