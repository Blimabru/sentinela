#!/usr/bin/env bash
set -e

echo "========================================================"
echo "        PROJECT FADE TO BLACK - ARPG 2D & IA"
echo "========================================================"
echo ""

JAR_FILE="sentinela.jar"
if [ ! -f "$JAR_FILE" ]; then
    if [ -f "target/sentinela.jar" ]; then
        JAR_FILE="target/sentinela.jar"
    else
        echo "[!] O executável sentinela.jar não foi encontrado."
        echo "[*] Compilando e gerando o pacote standalone..."
        ./mvnw package -DskipTests
        JAR_FILE="target/sentinela.jar"
    fi
fi

echo "[*] Executando $JAR_FILE diretamente com Java JRE..."
java -jar "$JAR_FILE"
