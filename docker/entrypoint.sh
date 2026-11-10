#!/bin/bash
# ==============================================================================
# Script: entrypoint.sh - Orquestrador de Runtime Gráfico HiDPI e Web-Streaming
# Ambiente: Contêiner Ubuntu Jammy (JRE 17 + Xvfb + Openbox + x11vnc + noVNC)
# Descrição: Inicializa o servidor X virtual em resolução HiDPI (1600x1200),
#            aplica regras de janela sem bordas, configura qualidade máxima no
#            cliente noVNC (quality=9) e executa a JVM em escala vetorial 2x.
# ==============================================================================

# Interrompe imediatamente a execução do script caso qualquer comando retorne erro.
set -e

# ── 1. Parâmetros de Resolução HiDPI do Display Virtual ───────────────────────
# Lê as dimensões informadas pelas variáveis de ambiente ou aplica o padrão HiDPI
# 1600x1200 (2x a resolução lógica nativa 800x600 para máxima nitidez em monitores HD/4K).
WIDTH=${DISPLAY_WIDTH:-1600}
HEIGHT=${DISPLAY_HEIGHT:-1200}

# Calcula automaticamente o fator de escala proporcional do Canvas JavaFX
# (ex: 1600x1200 -> 2.00x; 800x600 -> 1.00x), salvo override via RENDER_SCALE.
RENDER_SCALE=${RENDER_SCALE:-$(python3 -c "print(f'{max(1.0, min(${WIDTH}/800.0, ${HEIGHT}/600.0)):.2f}')")}

echo "========================================================"
echo "    PROJECT FADE TO BLACK - DOCKER WEB RUNTIME (HiDPI)"
echo "========================================================"

# ── 2. Configuração de Auto-Conexão e Qualidade Máxima (noVNC) ────────────────
# Sobrescreve a página inicial padrão do noVNC para redirecionar o navegador
# instantaneamente ao visualizador com:
# - autoconnect=true & reconnect=true: conexão imediata e recuperação de queda;
# - resize=scale: ajusta a tela HiDPI (1600x1200) à janela do navegador sem cortes;
# - quality=9: qualidade máxima de imagem (reduz borramento JPEG em textos da HUD);
# - compression=1: compressão leve com baixíssima latência de CPU/rede.
if [ -d "/usr/share/novnc" ]; then
    cat <<'EOF' > /usr/share/novnc/index.html
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>Project Fade to Black</title>
    <meta http-equiv="refresh" content="0; url=vnc.html?autoconnect=true&resize=scale&reconnect=true&quality=9&compression=1">
    <script>window.location.replace("vnc.html?autoconnect=true&resize=scale&reconnect=true&quality=9&compression=1");</script>
    <style>body { background-color: #050505; color: #ffd166; font-family: monospace; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }</style>
</head>
<body>
    <h2>Iniciando Project Fade to Black...</h2>
</body>
</html>
EOF
fi

# ── 3. Configuração do Gerenciador de Janelas (Openbox Sem Bordas) ────────────
# Remove barras de título e bordas decorativas da janela JavaFX, fixando-a na
# coordenada (0,0) com foco automático de entrada para ocupar 100% do framebuffer.
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

# ── 4. Inicialização Sequencial da Pilha Gráfica X11 / VNC / WebSockets ───────

# [1/4] Sobe o servidor X virtual em memória na tela :0 com profundidade de cor 24-bit
# e extensões GLX/RENDER habilitadas; '-ac' desativa controle de acesso local X11.
echo "[1/4] Iniciando Display Virtual Xvfb (:0) ${WIDTH}x${HEIGHT}x24 (Escala ${RENDER_SCALE}x)..."
Xvfb :0 -screen 0 "${WIDTH}x${HEIGHT}x24" -ac +extension GLX +render -noreset &
sleep 1

# Exporta a variável DISPLAY para que todos os processos filhos renderizem na tela :0.
export DISPLAY=:0

# [2/4] Inicializa o gerenciador Openbox para aplicar foco e regras sem bordas ao Stage JavaFX.
echo "[2/4] Iniciando Gerenciador de Janelas Leve (Openbox)..."
openbox &
sleep 1

# [3/4] Inicia o servidor VNC atrelado ao display :0.
# NOTA DE SEGURANÇA: '-listen 127.0.0.1' restringe o protocolo VNC bruto (sem senha)
# exclusivamente à interface de loopback interna do contêiner, garantindo que o tráfego
# externo trafegue apenas via túnel WebSocket/HTTPS pelo proxy reverso NGINX UI.
echo "[3/4] Iniciando Servidor VNC local (x11vnc)..."
x11vnc -display :0 -nopw -listen 127.0.0.1 -xkb -forever -shared -bg

# [4/4] Abre a ponte WebSocket/HTTP na porta 8080, servindo os arquivos estáticos do
# noVNC e encaminhando os quadros gráficos do socket VNC local (127.0.0.1:5900).
echo "[4/4] Iniciando Ponte WebSockets / noVNC na porta 8080 (quality=9)..."
websockify --web /usr/share/novnc 8080 127.0.0.1:5900 &
sleep 1

# ── 5. Execução Principal da Aplicação JavaFX (JVM) ───────────────────────────
# - 'exec' substitui o shell atual pela JVM como PID 1 (recebe SIGTERM limpo).
# - '-Dprism.order=sw' ativa renderização por software compatível com Xvfb.
# - '-Dglass.gtk.uiScale=1.0' evita escalonamento bitmap duplo pelo GTK Linux.
# - '-Drender.scale' instrui o Canvas a rasterizar vetores e fontes diretamente em HiDPI.
echo "[*] Inicializando fade-to-black.jar em escala nativa ${RENDER_SCALE}x..."
exec java -Dprism.order=sw -Dglass.gtk.uiScale=1.0 -Drender.scale="${RENDER_SCALE}" -jar /app/fade-to-black.jar
