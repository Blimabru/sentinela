@echo off
title Project Fade to Black - Operacao Sentinela
cls
echo ========================================================
echo         PROJECT FADE TO BLACK - ARPG 2D & IA
echo ========================================================
echo.

set JAR_FILE=fade-to-black.jar
if not exist "%JAR_FILE%" (
    if exist "target\fade-to-black.jar" (
        set JAR_FILE=target\fade-to-black.jar
    ) else (
        echo [!] O executavel fade-to-black.jar nao foi encontrado.
        echo [*] Compilando e gerando o pacote standalone...
        call .\mvnw.cmd package -DskipTests
        if errorlevel 1 (
            echo [ERRO] Falha ao compilar o projeto. Verifique o Java 17+.
            pause
            exit /b 1
        )
        set JAR_FILE=target\fade-to-black.jar
    )
)

echo [*] Executando %JAR_FILE% diretamente com Java JRE...
java -jar "%JAR_FILE%"
if errorlevel 1 (
    echo.
    echo [ERRO] Nao foi possivel iniciar o jogo.
    echo Certifique-se de que o Java 17 ou superior esta instalado e no PATH.
    pause
)
