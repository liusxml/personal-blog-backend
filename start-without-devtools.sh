#!/bin/bash

# 停止所有正在运行的实例
pkill -f blog-application

# 等待进程完全停止
sleep 2

# 设置环境变量禁用 DevTools
export SPRING_DEVTOOLS_RESTART_ENABLED=false

# 启动应用（不使用 DevTools）
java -jar blog-application/target/blog-application-1.0-SNAPSHOT.jar \
  --spring.devtools.restart.enabled=false \
  --spring.devtools.livereload.enabled=false

echo "Application started. Check logs/personal-blog-backend.log for output"
