#!/usr/bin/env bash
# =====================================================================================
# SICARES - Preparación del servidor (AWS EC2 t3.small / t3.micro con Ubuntu 24.04 LTS)
# Ejecutar UNA sola vez, dentro de la VM:   sudo bash instalar-servidor.sh
# Instala: swap 2 GB, MySQL 8, Java 21, Nginx; crea la base de datos, el usuario,
# el archivo de secretos y el servicio "sicares" que arranca el backend solo.
# =====================================================================================
set -euo pipefail

if [[ $EUID -ne 0 ]]; then echo "Ejecútalo con sudo: sudo bash instalar-servidor.sh"; exit 1; fi

echo "== 1/7 Datos de configuración"
read -rsp "Contraseña para el usuario MySQL 'sicares_app' (solo letras, números y * - _ ; anótala): " DB_PASS; echo
read -rsp "Contraseña para el administrador inicial admin@sicares.co (solo letras, números y * - _ ; anótala): " ADMIN_PASS; echo
TOKEN=$(curl -s -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 300" || true)
IP_PUBLICA=$(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" "http://169.254.169.254/latest/meta-data/public-ipv4" || true)
RAM_MB=$(free -m | awk '/^Mem:/{print $2}')
if (( RAM_MB > 1500 )); then HEAP=640m; POOL=192M; else HEAP=320m; POOL=64M; fi
echo "RAM: ${RAM_MB} MB -> heap Java ${HEAP}, buffer MySQL ${POOL}"
echo "IP pública detectada: ${IP_PUBLICA:-no detectada}"

echo "== 2/7 Memoria swap de 2 GB (colchón para la poca RAM)"
if ! swapon --show | grep -q /swapfile; then
  fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

echo "== 3/7 Paquetes: MySQL 8, Java 21, Nginx, unzip"
export DEBIAN_FRONTEND=noninteractive
apt-get update -y
apt-get install -y mysql-server openjdk-21-jre-headless nginx unzip

echo "== 4/7 MySQL: ajustes para poca memoria + base de datos y usuario"
cat > /etc/mysql/mysql.conf.d/zz-sicares.cnf <<CNF
[mysqld]
bind-address            = 127.0.0.1
performance_schema      = OFF
innodb_buffer_pool_size = ${POOL}
max_connections         = 30
default-time-zone       = '-05:00'
CNF
systemctl restart mysql
mysql <<SQL
CREATE DATABASE IF NOT EXISTS sicares_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'sicares_app'@'localhost' IDENTIFIED BY '${DB_PASS}';
ALTER USER 'sicares_app'@'localhost' IDENTIFIED BY '${DB_PASS}';
GRANT ALL PRIVILEGES ON sicares_db.* TO 'sicares_app'@'localhost';
FLUSH PRIVILEGES;
SQL

echo "== 5/7 Usuario de sistema, carpetas y secretos"
id sicares &>/dev/null || useradd --system --no-create-home --shell /usr/sbin/nologin sicares
mkdir -p /opt/sicares /var/www/sicares /etc/sicares
cat > /etc/sicares/sicares.env <<ENV
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
PORT=8080
DB_URL=jdbc:mysql://localhost:3306/sicares_db?sslMode=DISABLED&allowPublicKeyRetrieval=true&serverTimezone=America/Bogota
DB_USER=sicares_app
DB_PASSWORD=${DB_PASS}
JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')
CORS_ORIGINS=http://${IP_PUBLICA:-localhost}
ADMIN_CONTRASENA=${ADMIN_PASS}
ENV
chmod 600 /etc/sicares/sicares.env
chown -R sicares:sicares /opt/sicares

echo "== 6/7 Servicio systemd 'sicares' (backend Spring Boot)"
cat > /etc/systemd/system/sicares.service <<UNIT
[Unit]
Description=SICARES backend (Spring Boot)
After=network.target mysql.service
Requires=mysql.service

[Service]
User=sicares
EnvironmentFile=/etc/sicares/sicares.env
ExecStart=/usr/bin/java -Xms128m -Xmx${HEAP} -XX:+UseSerialGC -Duser.timezone=America/Bogota -jar /opt/sicares/sicares.jar
Restart=on-failure
RestartSec=10
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
UNIT
systemctl daemon-reload
systemctl enable sicares

echo "== 7/7 Nginx: sirve el frontend y reenvía /api al backend"
cat > /etc/nginx/sites-available/sicares <<'NGX'
server {
    listen 80 default_server;
    server_name _;
    root /var/www/sicares;
    index index.html;
    client_max_body_size 10M;

    # Backend Spring Boot
    location ~ ^/(api|swagger-ui|v3/api-docs)(/|\.html|$) {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Port  $server_port;
        proxy_read_timeout 120s;
    }

    # Rutas de Angular (al recargar /login, /admin/... devuelve index.html)
    location / {
        try_files $uri $uri/ /index.html;
    }
}
NGX
ln -sf /etc/nginx/sites-available/sicares /etc/nginx/sites-enabled/sicares
rm -f /etc/nginx/sites-enabled/default
nginx -t && systemctl restart nginx

echo
echo "Servidor listo. Siguiente paso: subir sicares.jar y frontend.zip y correr: sudo bash actualizar.sh"
