#!/usr/bin/env bash
# =====================================================================================
# SICARES - Respaldo automático de la base de datos y de los documentos subidos
# Auditoría - Plan de mejoramiento #1 (Ítems 6.5.7, 6.7.8 y 6.10.3; Riesgo #1).
#
# Qué hace:
#   1. mysqldump consistente (--single-transaction) de sicares_db, comprimido (.sql.gz)
#   2. .tar.gz de los documentos de postulación (/var/lib/sicares/uploads)
#   3. Verifica que el respaldo no esté vacío ni dañado (gzip -t)
#   4. Borra respaldos con más de DIAS_RETENCION días (por defecto 7)
#   5. (Opcional) copia el respaldo FUERA del servidor, a un bucket S3, si S3_BUCKET está configurado
#
# Lo ejecuta el cron /etc/cron.d/sicares todos los días a las 2:30 a. m. hora Colombia (7:30 UTC).
# Manual:   sudo bash /opt/sicares/bin/respaldar-bd.sh
# Registro: /var/log/sicares/respaldo.log
# =====================================================================================
set -euo pipefail

CONF=/etc/sicares/respaldo.env            # opcional: DIAS_RETENCION=7, S3_BUCKET=nombre-del-bucket
[[ -f "$CONF" ]] && source "$CONF"
DIAS_RETENCION="${DIAS_RETENCION:-7}"
S3_BUCKET="${S3_BUCKET:-}"
BD=sicares_db
DESTINO=/var/backups/sicares/diarios
UPLOADS=/var/lib/sicares/uploads
LOG=/var/log/sicares/respaldo.log
FECHA=$(date +%F_%H%M)

if [[ $EUID -ne 0 ]]; then echo "Ejecútalo con sudo"; exit 1; fi
mkdir -p "$DESTINO" "$(dirname "$LOG")"
chmod 700 "$DESTINO"
exec >> "$LOG" 2>&1
echo "===== $(date '+%F %T') Inicio del respaldo ====="

# 1. Base de datos (root entra a MySQL por socket, sin contraseña, solo dentro del servidor)
ARCH_BD="$DESTINO/sicares_db_$FECHA.sql.gz"
mysqldump --single-transaction --routines --triggers --set-gtid-purged=OFF "$BD" | gzip -9 > "$ARCH_BD"
gzip -t "$ARCH_BD"
TAM=$(stat -c %s "$ARCH_BD")
if (( TAM < 200 )); then
  rm -f "$ARCH_BD"
  echo "ERROR: el respaldo de la BD salió vacío ($TAM bytes); revisar MySQL"; exit 1
fi
echo "BD OK: $ARCH_BD ($(du -h "$ARCH_BD" | cut -f1))"

# 2. Documentos subidos por los beneficiarios
if [[ -d "$UPLOADS" ]]; then
  ARCH_DOCS="$DESTINO/documentos_$FECHA.tar.gz"
  tar -czf "$ARCH_DOCS" -C "$(dirname "$UPLOADS")" "$(basename "$UPLOADS")"
  echo "Documentos OK: $ARCH_DOCS ($(du -h "$ARCH_DOCS" | cut -f1))"
fi
chmod 600 "$DESTINO"/*

# 3. Rotación: conserva solo los últimos N días
find "$DESTINO" -type f -name '*.gz' -mtime +"$DIAS_RETENCION" -print -delete

# 4. Copia fuera del servidor (opcional)
if [[ -n "$S3_BUCKET" ]] && command -v aws >/dev/null 2>&1; then
  aws s3 cp "$ARCH_BD" "s3://$S3_BUCKET/bd/" --only-show-errors
  [[ -n "${ARCH_DOCS:-}" ]] && aws s3 cp "$ARCH_DOCS" "s3://$S3_BUCKET/documentos/" --only-show-errors
  echo "Copia en S3 OK: s3://$S3_BUCKET/"
else
  echo "(Sin S3_BUCKET: el respaldo queda solo en el servidor; descárguenlo con scp semanalmente)"
fi

echo "===== $(date '+%F %T') Respaldo terminado ====="