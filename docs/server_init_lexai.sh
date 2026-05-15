#!/usr/bin/env bash
set -euo pipefail

systemctl enable --now mysql nginx

mysql <<'SQL'
CREATE DATABASE IF NOT EXISTS lexai
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'lexai'@'localhost' IDENTIFIED BY 'lexai_demo_2026';
ALTER USER 'lexai'@'localhost' IDENTIFIED BY 'lexai_demo_2026';
GRANT ALL PRIVILEGES ON lexai.* TO 'lexai'@'localhost';
FLUSH PRIVILEGES;
SQL

echo "mysql_status=$(systemctl is-active mysql)"
echo "nginx_status=$(systemctl is-active nginx)"
echo "databases:"
mysql -e "SHOW DATABASES LIKE 'lexai';"
echo "users:"
mysql -e "SELECT user, host FROM mysql.user WHERE user='lexai';"
