#!/usr/bin/env bash
# =====================================================================================
# SICARES - Restaurar la base de datos (y opcionalmente los documentos) desde un respaldo
# Auditoría - Plan de mejoramiento #1: procedimiento de recuperación ante fallos.
#
# Uso:
#   sudo bash /opt/sicares/bin/restaurar-bd.sh                       -> lista los respaldos disponibles
#   sudo bash /opt/sicares/bin/restaurar-bd.sh <archivo.sql.gz>       -> restaura solo la BD
#   sudo bash /opt/sicares/bin/restaurar-bd.sh <archivo.sql.gz> <documentos.tar.gz>
#
# Antes de restaurar se hace un respaldo de seguridad del estado actual (por si se elige el archivo equivocado).
# =====================================================================================
set -euo pipefail
DESTINO=/var/backups/sicares/diarios
BD=sicares_db

if [[ $EUID -ne 0 ]]; then echo "Ejecútalo con sudo"; exit 1; fi

if [[ $# -lt 1 ]]; then
  echo "Respaldos disponibles (más reciente al final):"
  ls -lh --time-style=+%F_%H:%M "$DESTINO" 2>/dev/null | awk 'NR>1{print $6, $5, $7}'
  echo
  echo "Uso: sudo bash $0 $DESTINO/sicares_db_AAAA-MM-DD_HHMM.sql.gz [documentos_....tar.gz]"
  exit 0
fi

ARCH_BD="$1"; ARCH_DOCS="${2:-}"
[[ -f "$ARCH_BD" ]] || { echo "No existe $ARCH_BD"; exit 1; }
gzip -t "$ARCH_BD" || { echo "El archivo está dañado"; exit 1; }

read -rp "Esto REEMPLAZA los datos actuales de $BD con $ARCH_BD. Escribe SI para continuar: " OK
[[ "$OK" == "SI" ]] || { echo "Cancelado"; exit 0; }

echo "== 1/4 Respaldo de seguridad del estado actual"
mkdir -p /var/backups/sicares
mysqldump --single-transaction --set-gtid-purged=OFF "$BD" | gzip > "/var/backups/sicares/antes-de-restaurar_$(date +%F_%H%M).sql.gz"

echo "== 2/4 Deteniendo el backend"
systemctl stop sicares

echo "== 3/4 Restaurando la base de datos"
mysql -e "DROP DATABASE IF EXISTS $BD; CREATE DATABASE $BD CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
gunzip -c "$ARCH_BD" | mysql "$BD"
if [[ -n "$ARCH_DOCS" && -f "$ARCH_DOCS" ]]; then
  rm -rf /var/lib/sicares/uploads
  tar -xzf "$ARCH_DOCS" -C /var/lib/sicares
  chown -R sicares:sicares /var/lib/sicares
  echo "Documentos restaurados"
fi

echo "== 4/4 Arrancando el backend"
systemctl start sicares
for i in $(seq 1 36); do
  if curl -s http://127.0.0.1:8080/actuator/health | grep -q '"UP"'; then echo "Restauración completa ✅"; exit 0; fi
  sleep 5
done
echo "El backend no arrancó: sudo journalctl -u sicares -n 80 --no-pager"; exit 1