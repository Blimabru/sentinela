# ==============================================================
# ESTÁGIO 1: COMPILAÇÃO COM MAVEN (Linux Nativo)
# ==============================================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copia arquivos de build e código-fonte
COPY pom.xml .
COPY src ./src

# Compila e gera o Fat JAR com os binários nativos do JavaFX para Linux
RUN mvn clean package -DskipTests

# ==============================================================
# ESTÁGIO 2: RUNTIME LEVE COM DISPLAY VIRTUAL E WEB-STREAMING (noVNC)
# ==============================================================
FROM eclipse-temurin:17-jre-jammy

LABEL maintainer="Bruno Lima <bru.no@outlook.com.br>"
LABEL description="Project Fade to Black - Web Runtime via Docker & noVNC"

ENV DEBIAN_FRONTEND=noninteractive
ENV DISPLAY_WIDTH=800
ENV DISPLAY_HEIGHT=600

# Instala o servidor X virtual (Xvfb), gerenciador de janelas, x11vnc, noVNC e bibliotecas gráficas do Linux
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

WORKDIR /app

# Copia o executável compilado do primeiro estágio
COPY --from=builder /build/target/sentinela.jar /app/sentinela.jar

# Copia o script de inicialização do ambiente gráfico e web
COPY docker/entrypoint.sh /app/entrypoint.sh
RUN sed -i 's/\r$//' /app/entrypoint.sh && chmod +x /app/entrypoint.sh

# Porta HTTP do noVNC
EXPOSE 8080

ENTRYPOINT ["/app/entrypoint.sh"]
