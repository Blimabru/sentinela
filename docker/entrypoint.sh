#!/bin/bash
# ==============================================================================
# Script: entrypoint.sh - Orquestrador de Runtime Gráfico e Web-Streaming
# Ambiente: Contêiner Ubuntu Jammy (JRE 17 + Xvfb + Openbox + x11vnc + noVNC)
# Descrição: Inicializa o servidor X virtual em memória, aplica regras de janela
#            sem bordas para preenchimento integral do canvas, configura conexão
#            automática no cliente noVNC e inicia a JVM em modo software Prism.
# ==============================================================================

# Interrompe imediatamente a execução do script caso qualquer comando retorne erro.
set -e

# ── 1. Parâmetros de Resolução do Display Virtual ─────────────────────────────
# Lê as dimensões informadas pelas variáveis de ambiente ou aplica o padrão 800x600.
WIDTH=${DISPLAY_WIDTH:-800}
HEIGHT=${DISPLAY_HEIGHT:-600}

echo "========================================================"
echo "    PROJECT FADE TO BLACK - DOCKER WEB RUNTIME"
echo "========================================================"

# ── 2. Configuração de Auto-Conexão do Cliente Web (noVNC) ────────────────────
# Sobrescreve a página inicial padrão do noVNC para redirecionar o navegador
# instantaneamente ao visualizador com conexão automática, redimensionamento
# proporcional ao viewport (resize=scale) e reconexão automática ativa.
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

# ── 3. Configuração do Gerenciador de Janelas (Openbox Sem Bordas) ────────────
# Remove barras de título e bordas decorativas da janela JavaFX, fixando-a na
# coordenada (0,0) com foco automático de entrada para que o jogo ocupe 100% da
# tela virtual sem margens visuais no navegador Web.
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
echo "[1/4] Iniciando Display Virtual Xvfb (:0) ${WIDTH}x${HEIGHT}x24..."
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
echo "[4/4] Iniciando Ponte WebSockets / noVNC na porta 8080..."
websockify --web /usr/share/novnc 8080 127.0.0.1:5900 &
sleep 1

# ── 5. Execução Principal da Aplicação JavaFX (JVM) ───────────────────────────
# O comando 'exec' substitui o shell atual pelo processo da JVM como PID 1, garantindo
# o recebimento correto de sinais de encerramento (SIGTERM) pelo Docker/Portainer.
# O parâmetro '-Dprism.order=sw' força o motor Prism do JavaFX a utilizar renderização
# por software compatível com o framebuffer Xvfb na ausência de aceleração GPU física.
echo "[*] Inicializando o jogo (sentinela.jar) com renderizador de software Prism..."
exec java -Dprism.order=sw -jar /app/sentinela.jar
