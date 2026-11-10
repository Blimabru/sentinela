#!/usr/bin/env bash
set -e

echo "========================================================"
echo "        PROJECT FADE TO BLACK - ARPG 2D & IA"
echo "========================================================"
echo ""

JAR_FILE="fade-to-black.jar"
if [ ! -f "$JAR_FILE" ]; then
    if [ -f "target/fade-to-black.jar" ]; then
        JAR_FILE="target/fade-to-black.jar"
    else
        echo "[!] O executável fade-to-black.jar não foi encontrado."
        echo "[*] Compilando e gerando o pacote standalone..."
        ./mvnw package -DskipTests
        JAR_FILE="target/fade-to-black.jar"
    fi
fi

echo "[*] Executando $JAR_FILE diretamente com Java JRE..."
java -jar "$JAR_FILE"
