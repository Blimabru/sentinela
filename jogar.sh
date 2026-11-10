#!/usr/bin/env bash
# ==============================================================================
# Launcher Linux/macOS (jogar.sh) - Project Fade to Black
# ==============================================================================
# Resolve o caminho do Fat JAR executável ('fade-to-black.jar'), compila o
# projeto automaticamente pelo Maven Wrapper caso o binário não exista e lança
# o cliente gráfico desktop na JRE do sistema.
# ==============================================================================

# Aborta imediatamente a execução caso o build ou o lançamento retornem erro.
set -e

echo "========================================================"
echo "        PROJECT FADE TO BLACK - ARPG 2D & IA"
echo "========================================================"
echo ""

# ── 1. Resolução do Artefato Executável e Build Sob Demanda ───────────────────
# Busca primeiramente na raiz para distribuição portátil, depois no diretório de
# saída do Maven e, por último, dispara o empacotamento automatizado.
JAR_FILE="fade-to-black.jar"
if [ ! -f "$JAR_FILE" ]; then
    if [ -f "target/fade-to-black.jar" ]; then
        JAR_FILE="target/fade-to-black.jar"
    else
        echo "[!] O executável fade-to-black.jar não foi encontrado."
        echo "[*] Compilando e gerando o pacote standalone..."
        # Pula testes unitários para reduzir o tempo de abertura ao jogador final
        ./mvnw package -DskipTests
        JAR_FILE="target/fade-to-black.jar"
    fi
fi

# ── 2. Execução na JVM ────────────────────────────────────────────────────────
# Executa o pacote standalone invocando a classe Launcher definida no Manifesto.
echo "[*] Executando $JAR_FILE diretamente com Java JRE..."
java -jar "$JAR_FILE"
