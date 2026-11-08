#!/usr/bin/env bash
# =====================================================================================
# SICARES - Monitoreo automático del servidor (cada 5 minutos por cron)
# Auditoría - Plan de mejoramiento #4 (Ítem 6.10.6; Riesgo #4). Complementa las alarmas de AWS CloudWatch.
#
# Revisa:  - salud del backend (/actuator/health de Spring Boot Actuator)
#          - que MySQL y Nginx estén activos
#          - uso de disco, memoria RAM y carga de CPU
# Si el backend o MySQL están caídos, los reinicia solos y deja constancia.
# Registro: /var/log/sicares/monitor.log   ·  Resumen rápido:  sudo bash /opt/sicares/bin/monitorear.sh --estado
# =====================================================================================
set -uo pipefail
LOG=/var/log/sicares/monitor.log
LIMITE_DISCO=85     # % de disco usado para alertar
LIMITE_RAM=90       # % de RAM usada para alertar
mkdir -p "$(dirname "$LOG")"

ahora() { date '+%F %T'; }
DISCO=$(df --output=pcent / | tail -1 | tr -dc '0-9')
RAM=$(free | awk '/^Mem:/{printf "%d", ($3/$2)*100}')
CARGA=$(cut -d' ' -f1 /proc/loadavg)
SALUD=$(curl -s -m 10 http://127.0.0.1:8080/actuator/health || echo '')
if echo "$SALUD" | grep -q '"UP"'; then BACKEND=UP; else BACKEND=DOWN; fi

if [[ "${1:-}" == "--estado" ]]; then
  echo "Backend: $BACKEND | MySQL: $(systemctl is-active mysql) | Nginx: $(systemctl is-active nginx)"
  echo "Disco: ${DISCO}% | RAM: ${RAM}% | Carga CPU (1 min): $CARGA"
  echo "Últimos eventos:"; grep -E 'ALERTA|REINICIO' "$LOG" 2>/dev/null | tail -10 || true
  exit 0
fi

exec >> "$LOG" 2>&1
echo "$(ahora) estado: backend=$BACKEND disco=${DISCO}% ram=${RAM}% carga=$CARGA"

for svc in mysql nginx; do
  if ! systemctl is-active --quiet "$svc"; then
    echo "$(ahora) ALERTA: $svc estaba detenido -> REINICIO"
    systemctl restart "$svc"
  fi
done

if [[ "$BACKEND" == "DOWN" ]]; then
  # Si el servicio está arrancando (menos de 3 min activo) no se reinicia todavía
  INICIO=$(systemctl show sicares -p ActiveEnterTimestampMonotonic --value)
  AHORA_MONO=$(awk '{printf "%d", $1*1000000}' /proc/uptime)
  if (( INICIO == 0 || (AHORA_MONO - INICIO) > 180000000 )); then
    echo "$(ahora) ALERTA: backend sin respuesta en /actuator/health -> REINICIO de sicares"
    journalctl -u sicares -n 20 --no-pager | sed 's/^/    /'
    systemctl restart sicares
  fi
fi

(( DISCO >= LIMITE_DISCO )) && echo "$(ahora) ALERTA: disco al ${DISCO}% (revisar /var/backups y /var/log)"
(( RAM >= LIMITE_RAM )) && echo "$(ahora) ALERTA: RAM al ${RAM}%"

# El log no crece sin control: se queda con las últimas 5000 líneas
tail -n 5000 "$LOG" > "$LOG.tmp" && mv "$LOG.tmp" "$LOG"
exit 0