#!/bin/bash
set -e

WIDTH=${DISPLAY_WIDTH:-800}
HEIGHT=${DISPLAY_HEIGHT:-600}

echo "========================================================"
echo "    PROJECT FADE TO BLACK - DOCKER WEB RUNTIME"
echo "========================================================"

# 1. Configura a página inicial do noVNC para auto-conexão e ajuste de tela automático
if [ -d "/usr/share/novnc" ]; then
    cat <<'EOF' > /usr/share/novnc/index.html
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>Project Fade to Black</title>
    <meta http-equiv="refresh" content="0; url=vnc.html?autoconnect=true&resize=scale&reconnect=true">
    <script>window.location.replace("vnc.html?autoconnect=true&resize=scale&reconnect=true");</script>
    <style>body { background-color: #050505; color: #ffd166; font-family: monospace; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }</style>
</head>
<body>
    <h2>Iniciando Project Fade to Black...</h2>
</body>
</html>
EOF
fi

# 2. Configura o Openbox para exibir o jogo sem bordas/título e preencher 100% da tela 800x600
mkdir -p /root/.config/openbox
cat <<'EOF' > /root/.config/openbox/rc.xml
<?xml version="1.0" encoding="UTF-8"?>
<openbox_config xmlns="http://openbox.org/3.4/rc">
  <applications>
    <application class="*">
      <decor>no</decor>
      <position force="yes">
        <x>0</x>
        <y>0</y>
      </position>
      <focus>yes</focus>
    </application>
  </applications>
</openbox_config>
EOF

echo "[1/4] Iniciando Display Virtual Xvfb (:0) ${WIDTH}x${HEIGHT}x24..."
Xvfb :0 -screen 0 "${WIDTH}x${HEIGHT}x24" -ac +extension GLX +render -noreset &
sleep 1

export DISPLAY=:0

echo "[2/4] Iniciando Gerenciador de Janelas Leve (Openbox)..."
openbox &
sleep 1

echo "[3/4] Iniciando Servidor VNC local (x11vnc)..."
x11vnc -display :0 -nopw -listen 127.0.0.1 -xkb -forever -shared -bg

echo "[4/4] Iniciando Ponte WebSockets / noVNC na porta 8080..."
websockify --web /usr/share/novnc 8080 127.0.0.1:5900 &
sleep 1

echo "[*] Inicializando o jogo (sentinela.jar) com renderizador de software Prism..."
exec java -Dprism.order=sw -jar /app/sentinela.jar
