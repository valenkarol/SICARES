#!/usr/bin/env bash
# =====================================================================================
# SICARES - Publicar una versión nueva (primera vez y cada vez que cambien el código)
# Antes: subir a la carpeta de inicio de la VM (~) los archivos:
#   - sicares.jar   (de Backend/build/libs/, renombrado)
#   - frontend.zip  (el CONTENIDO de frontend/dist/frontend/browser comprimido)
# Ejecutar dentro de la VM:   sudo bash actualizar.sh
# =====================================================================================
set -euo pipefail
ORIGEN="${1:-/home/${SUDO_USER:-$USER}}"

if [[ -f "$ORIGEN/sicares.jar" ]]; then
  echo "== Backend: instalando sicares.jar"
  cp "$ORIGEN/sicares.jar" /opt/sicares/sicares.jar
  chown sicares:sicares /opt/sicares/sicares.jar
  systemctl restart sicares
else
  echo "(no hay $ORIGEN/sicares.jar, el backend no cambia)"
fi

if [[ -f "$ORIGEN/frontend.zip" ]]; then
  echo "== Frontend: instalando frontend.zip"
  rm -rf /var/www/sicares/*
  unzip -oq "$ORIGEN/frontend.zip" -d /var/www/sicares
  # Si el zip traía la carpeta "browser" adentro, se sube su contenido un nivel
  if [[ -d /var/www/sicares/browser && ! -f /var/www/sicares/index.html ]]; then
    mv /var/www/sicares/browser/* /var/www/sicares/ && rmdir /var/www/sicares/browser
  fi
  chown -R www-data:www-data /var/www/sicares
else
  echo "(no hay $ORIGEN/frontend.zip, el frontend no cambia)"
fi

echo "== Esperando a que arranque el backend (puede tardar 1-2 minutos en la VM gratuita)..."
for i in $(seq 1 36); do
  if curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/api/catalogos/ 2>/dev/null | grep -qv '000'; then
    echo "Backend arriba ✅"; exit 0
  fi
  sleep 5
done
echo "El backend no respondió. Revisa el error con:  sudo journalctl -u sicares -n 80 --no-pager"
exit 1
