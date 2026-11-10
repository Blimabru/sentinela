# ==============================================================================
# Dockerfile: Project Fade to Black - Multi-stage Web Runtime
# Base Build: Maven 3.9 + Eclipse Temurin JDK 17 (Compilação Nativa Linux)
# Base Runtime: Eclipse Temurin JRE 17 (Ubuntu Jammy) + Xvfb + noVNC
# Descrição: Compila os artefatos Java/JavaFX com bibliotecas nativas Linux (.so)
#            e provisiona um ambiente gráfico virtual sem cabeça (headless) com
#            transmissão interativa para navegadores Web via WebSockets.
# ==============================================================================

# ── 1. Estágio de Compilação (Builder) ────────────────────────────────────────
# Utiliza imagem oficial com Maven e JDK 17 para isolar ferramentas de compilação
# e evitar que SDKs pesados permaneçam na imagem final de produção.
FROM maven:3.9-eclipse-temurin-17 AS builder

# Define o diretório de trabalho temporário para resolução e build do projeto.
WORKDIR /build

# Copia o descritor de dependências Maven e todo o código-fonte Java da aplicação.
COPY pom.xml .
COPY src ./src

# Executa a compilação completa e o empacotamento do Fat JAR (fade-to-black.jar).
# Como o build ocorre em sistema Linux x86_64/ARM64, o Maven resolve e embute
# automaticamente os binários gráficos nativos (.so) do JavaFX para Linux.
RUN mvn clean package -DskipTests


# ── 2. Estágio de Execução (Runtime Gráfico e Web-Streaming) ──────────────────
# Parte de uma imagem enxuta contendo apenas o JRE 17, reduzindo o consumo de
# disco, a superfície de ataque e o tempo de inicialização em produção.
FROM eclipse-temurin:17-jre-jammy

# Metadados de autoria e identificação para catalogação no registro e Portainer.
LABEL maintainer="Bruno Lima <bru.no@outlook.com.br>"
LABEL description="Project Fade to Black - Web Runtime via Docker, Xvfb e noVNC"

# Suprime prompts interativos durante a instalação de pacotes Debian/Ubuntu (APT).
ENV DEBIAN_FRONTEND=noninteractive
# Define as dimensões padrão do servidor de exibição virtual (alinhadas ao canvas 800x600).
ENV DISPLAY_WIDTH=800
ENV DISPLAY_HEIGHT=600

# Instala a pilha mínima de infraestrutura gráfica X11, transmissão Web e fontes:
# - xvfb: Servidor X virtual em memória (dispensa monitor ou GPU física conectada).
# - openbox: Gerenciador de janelas minimalista utilizado para remover bordas e focar o canvas.
# - x11vnc: Servidor VNC encarregado de capturar o framebuffer do display virtual :0.
# - novnc e websockify: Cliente HTML5 Canvas e ponte TCP->WebSocket para o navegador.
# - libgtk-3-0, libgl1-mesa-glx/dri e libx*: Bibliotecas nativas exigidas pelo OpenJFX.
# - fonts-dejavu-core e fonts-liberation: Fontes TrueType para renderização da HUD de telemetria.
# - procps: Fornece utilitários de inspeção de processos (pgrep) usados no healthcheck.
# Ao final, remove as listas em cache do APT para minimizar o tamanho da camada.
RUN apt-get update && apt-get install -y --no-install-recommends \
    xvfb \
    openbox \
    x11vnc \
    novnc \
    websockify \
    libgtk-3-0 \
    libgl1-mesa-glx \
    libgl1-mesa-dri \
    libasound2 \
    libxtst6 \
    libxrender1 \
    libxi6 \
    libxrandr2 \
    fonts-dejavu-core \
    fonts-liberation \
    procps \
    && rm -rf /var/lib/apt/lists/*

# Define o diretório raiz de execução do aplicativo no sistema de arquivos final.
WORKDIR /app

# Transfere exclusivamente o executável compilado do estágio 'builder', descartando
# código-fonte, arquivos intermediários e repositórios de cache do Maven.
COPY --from=builder /build/target/fade-to-black.jar /app/fade-to-black.jar

# Copia o script orquestrador de inicialização dos serviços gráficos e da JVM.
COPY docker/entrypoint.sh /app/entrypoint.sh

# Normaliza eventuais quebras de linha no formato Windows (CRLF -> LF) para evitar
# erro de interpretador '/bin/bash^M' no Linux e atribui permissão de execução.
RUN sed -i 's/\r$//' /app/entrypoint.sh && chmod +x /app/entrypoint.sh

# Documenta a porta TCP interna exposta pela ponte WebSocket/HTTP do noVNC.
EXPOSE 8080

# Estabelece o script de entrada responsável por orquestrar o X11, VNC, noVNC e o jogo.
ENTRYPOINT ["/app/entrypoint.sh"]
