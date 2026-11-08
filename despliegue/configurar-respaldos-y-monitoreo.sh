#!/usr/bin/env bash
# =====================================================================================
# SICARES - Instala los respaldos automáticos y el monitoreo (ejecutar UNA vez en el servidor)
# Auditoría - Plan de mejoramiento #1 (respaldos) y #4 (monitoreo).
# Antes: subir a la carpeta de inicio (~) respaldar-bd.sh, restaurar-bd.sh y monitorear.sh
# Uso:   sudo bash configurar-respaldos-y-monitoreo.sh
# =====================================================================================
set -euo pipefail
ORIGEN="/home/${SUDO_USER:-$USER}"
if [[ $EUID -ne 0 ]]; then echo "Ejecútalo con sudo"; exit 1; fi

echo "== 1/5 Copiando scripts a /opt/sicares/bin"
mkdir -p /opt/sicares/bin /var/log/sicares /var/backups/sicares/diarios
for s in respaldar-bd.sh restaurar-bd.sh monitorear.sh; do
  [[ -f "$ORIGEN/$s" ]] || { echo "Falta $ORIGEN/$s (súbelo con scp)"; exit 1; }
  sed 's/\r$//' "$ORIGEN/$s" > "/opt/sicares/bin/$s"     # quita los saltos de línea de Windows
  chmod 750 "/opt/sicares/bin/$s"
done
chmod 700 /var/backups/sicares /var/backups/sicares/diarios

echo "== 2/5 Configuración de respaldos (/etc/sicares/respaldo.env)"
if [[ ! -f /etc/sicares/respaldo.env ]]; then
  cat > /etc/sicares/respaldo.env <<'ENV'
# Días que se conservan los respaldos diarios en el servidor
DIAS_RETENCION=7
# Opcional: nombre de un bucket S3 para guardar una copia FUERA del servidor (dejar vacío si no se usa)
S3_BUCKET=
ENV
  chmod 600 /etc/sicares/respaldo.env
fi

echo "== 3/5 Tareas programadas (cron). El servidor está en hora UTC: 7:30 UTC = 2:30 a. m. en Colombia"
cat > /etc/cron.d/sicares <<'CRON'
# SICARES - Auditoría, plan de mejoramiento #1 y #4
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
# Respaldo diario de la BD y documentos: 7:30 UTC = 2:30 a. m. hora Colombia
30 7 * * * root /opt/sicares/bin/respaldar-bd.sh
# Monitoreo de salud cada 5 minutos
*/5 * * * * root /opt/sicares/bin/monitorear.sh
CRON
chmod 644 /etc/cron.d/sicares
systemctl restart cron

echo "== 4/5 Primer respaldo de prueba"
bash /opt/sicares/bin/respaldar-bd.sh && tail -n 6 /var/log/sicares/respaldo.log
ls -lh /var/backups/sicares/diarios

echo "== 5/5 Primera revisión de monitoreo"
bash /opt/sicares/bin/monitorear.sh
bash /opt/sicares/bin/monitorear.sh --estado
echo
echo "Listo ✅  Respaldos: /var/backups/sicares/diarios · Logs: /var/log/sicares/"