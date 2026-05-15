#!/usr/bin/env bash
set -euo pipefail

DB_USER="lexai"
DB_PASS="lexai_demo_2026"
DB_NAME="lexai"

mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "ALTER TABLE contracts MODIFY content LONGTEXT;"
mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "SHOW FULL COLUMNS FROM contracts WHERE Field = 'content';"
