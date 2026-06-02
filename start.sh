#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_ROOT="${PROJECT_ROOT:-/workspace/bank-account-system}"
MYSQL_DATABASE="${MYSQL_DATABASE:-bank_db}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-050130}"
RABBITMQ_DEFAULT_USER="${RABBITMQ_DEFAULT_USER:-bank}"
RABBITMQ_DEFAULT_PASS="${RABBITMQ_DEFAULT_PASS:-bank123}"

START_MYSQL="${START_MYSQL:-1}"
START_REDIS="${START_REDIS:-1}"
START_RABBITMQ="${START_RABBITMQ:-1}"
START_NGINX="${START_NGINX:-1}"
RENDER_NGINX_CONF="${RENDER_NGINX_CONF:-1}"

FRONTEND_DIST="${FRONTEND_DIST:-$PROJECT_ROOT/frontend/dist}"
AGENT_DIST="${AGENT_DIST:-$PROJECT_ROOT/agent/dist}"
NGINX_SITE_AVAILABLE="${NGINX_SITE_AVAILABLE:-/etc/nginx/sites-available/bank-account-system.conf}"
NGINX_SITE_ENABLED="${NGINX_SITE_ENABLED:-/etc/nginx/sites-enabled/bank-account-system.conf}"
SCHEMA_SQL="$PROJECT_ROOT/backend/src/main/resources/db/schema.sql"
DATA_SQL="$PROJECT_ROOT/backend/src/main/resources/db/data.sql"

log() {
  printf '[start] %s\n' "$*"
}

have() {
  command -v "$1" >/dev/null 2>&1
}

sql_escape() {
  printf '%s' "$1" | sed "s/'/''/g"
}

port_open() {
  local host="$1"
  local port="$2"
  timeout 1 bash -c ">/dev/tcp/$host/$port" >/dev/null 2>&1
}

wait_for_port() {
  local name="$1"
  local host="$2"
  local port="$3"
  local attempts="${4:-60}"

  for ((i = 1; i <= attempts; i++)); do
    if port_open "$host" "$port"; then
      log "$name is ready on $host:$port"
      return 0
    fi
    sleep 1
  done

  log "warning: $name did not open $host:$port within ${attempts}s"
  return 1
}

mysql_root() {
  mysql --protocol=socket -uroot "$@" 2>/dev/null \
    || MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -h127.0.0.1 -uroot "$@"
}

mysql_running() {
  mysqladmin ping --silent >/dev/null 2>&1 || port_open 127.0.0.1 3306
}

start_mysql() {
  [ "$START_MYSQL" = "1" ] || {
    log "MySQL startup skipped by START_MYSQL=$START_MYSQL"
    return 0
  }

  if ! have mysqld; then
    log "MySQL startup skipped: mysqld not installed"
    return 0
  fi

  if mysql_running; then
    log "MySQL is already running"
  else
    log "Starting MySQL"
    install -d -m 0755 -o mysql -g mysql /run/mysqld /var/run/mysqld
    if ! service mysql start >/dev/null 2>&1; then
      mysqld_safe >/var/log/mysqld_safe.log 2>&1 &
    fi
    wait_for_port "MySQL" 127.0.0.1 3306 90 || true
  fi

  if mysql_root -e "CREATE DATABASE IF NOT EXISTS \`$MYSQL_DATABASE\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" >/dev/null 2>&1; then
    local mysql_password_sql
    mysql_password_sql="$(sql_escape "$MYSQL_ROOT_PASSWORD")"
    mysql_root -e "CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED WITH mysql_native_password BY '$mysql_password_sql'; ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY '$mysql_password_sql'; GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION; FLUSH PRIVILEGES;" >/dev/null 2>&1 \
      || log "warning: failed to ensure remote MySQL root user"

    if [ -f "$SCHEMA_SQL" ] && ! mysql_root "$MYSQL_DATABASE" -NBe "SHOW TABLES LIKE 'bank_user';" | grep -q '^bank_user$'; then
      log "Importing MySQL schema into $MYSQL_DATABASE"
      mysql_root "$MYSQL_DATABASE" < "$SCHEMA_SQL" || log "warning: schema import failed"
    fi

    if [ -f "$DATA_SQL" ] && mysql_root "$MYSQL_DATABASE" -NBe "SHOW TABLES LIKE 'bank_product';" | grep -q '^bank_product$'; then
      local product_count
      product_count="$(mysql_root "$MYSQL_DATABASE" -NBe "SELECT COUNT(*) FROM bank_product;" 2>/dev/null || printf '0')"
      if [ "${product_count:-0}" = "0" ]; then
        log "Importing demo product data into $MYSQL_DATABASE"
        mysql_root "$MYSQL_DATABASE" < "$DATA_SQL" || log "warning: demo data import failed"
      fi
    fi
  else
    log "warning: MySQL is running, but root login failed; database initialization skipped"
  fi
}

start_redis() {
  [ "$START_REDIS" = "1" ] || {
    log "Redis startup skipped by START_REDIS=$START_REDIS"
    return 0
  }

  if ! have redis-server; then
    log "Redis startup skipped: redis-server not installed"
    return 0
  fi

  if pgrep -x redis-server >/dev/null 2>&1 || port_open 127.0.0.1 6379; then
    log "Redis is already running"
    return 0
  fi

  log "Starting Redis"
  service redis-server start >/dev/null 2>&1 || redis-server --daemonize yes
  wait_for_port "Redis" 127.0.0.1 6379 30 || true
}

rabbitmq_running() {
  rabbitmqctl status >/dev/null 2>&1 || port_open 127.0.0.1 5672
}

ensure_rabbitmq_user() {
  if ! have rabbitmqctl; then
    return 0
  fi

  rabbitmqctl add_user "$RABBITMQ_DEFAULT_USER" "$RABBITMQ_DEFAULT_PASS" >/dev/null 2>&1 || true
  rabbitmqctl change_password "$RABBITMQ_DEFAULT_USER" "$RABBITMQ_DEFAULT_PASS" >/dev/null 2>&1 || true
  rabbitmqctl set_user_tags "$RABBITMQ_DEFAULT_USER" administrator >/dev/null 2>&1 || true
  rabbitmqctl set_permissions -p / "$RABBITMQ_DEFAULT_USER" '.*' '.*' '.*' >/dev/null 2>&1 || true
}

start_rabbitmq() {
  [ "$START_RABBITMQ" = "1" ] || {
    log "RabbitMQ startup skipped by START_RABBITMQ=$START_RABBITMQ"
    return 0
  }

  if ! have rabbitmq-server; then
    log "RabbitMQ startup skipped: rabbitmq-server not installed"
    return 0
  fi

  if rabbitmq_running; then
    log "RabbitMQ is already running"
  else
    log "Starting RabbitMQ"
    rabbitmq-plugins enable rabbitmq_management >/dev/null 2>&1 || true
    service rabbitmq-server start >/dev/null 2>&1 || rabbitmq-server -detached
    wait_for_port "RabbitMQ" 127.0.0.1 5672 90 || true
  fi

  ensure_rabbitmq_user
}

render_nginx_conf() {
  [ "$RENDER_NGINX_CONF" = "1" ] || return 0

  install -d /etc/nginx/sites-available /etc/nginx/sites-enabled
  cat > "$NGINX_SITE_AVAILABLE" <<NGINX
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    listen 5173;
    listen [::]:5173;

    server_name _;
    root "$FRONTEND_DIST";
    index index.html;

    access_log /var/log/nginx/bank-frontend.access.log;
    error_log /var/log/nginx/bank-frontend.error.log;

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location / {
        try_files \$uri \$uri/ /index.html;
    }
}

server {
    listen 5174;
    listen [::]:5174;

    server_name _;
    root "$AGENT_DIST";
    index index.html;

    access_log /var/log/nginx/bank-agent.access.log;
    error_log /var/log/nginx/bank-agent.error.log;

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location / {
        try_files \$uri \$uri/ /index.html;
    }
}
NGINX

  rm -f /etc/nginx/sites-enabled/default
  ln -sf "$NGINX_SITE_AVAILABLE" "$NGINX_SITE_ENABLED"
}

start_nginx() {
  [ "$START_NGINX" = "1" ] || {
    log "nginx startup skipped by START_NGINX=$START_NGINX"
    return 0
  }

  if ! have nginx; then
    log "nginx startup skipped: nginx not installed"
    return 0
  fi

  [ -f "$FRONTEND_DIST/index.html" ] || log "warning: frontend dist not found at $FRONTEND_DIST"
  [ -f "$AGENT_DIST/index.html" ] || log "warning: agent dist not found at $AGENT_DIST"

  render_nginx_conf
  nginx -t
  log "Starting nginx: frontend on 80/5173, agent on 5174"

  if pgrep -x nginx >/dev/null 2>&1; then
    nginx -s reload
    tail -F /var/log/nginx/*.log
  else
    exec nginx -g 'daemon off;'
  fi
}

main() {
  cd "$PROJECT_ROOT"
  log "Starting non-backend runtime services from $PROJECT_ROOT"
  start_mysql
  start_redis
  start_rabbitmq
  start_nginx
}

main "$@"
