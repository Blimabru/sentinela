@echo off
:: ==============================================================================
:: Launcher Windows (jogar.bat) - Project Fade to Black
:: ==============================================================================
:: Localiza automaticamente o pacote Fat JAR distribuível ('fade-to-black.jar'),
:: realiza a compilação reproduzível sob demanda via Maven Wrapper caso ausente
:: e inicializa a aplicação JavaFX diretamente pela JRE instalada no sistema.
:: ==============================================================================

title Project Fade to Black - Operacao Sentinela
cls
echo ========================================================
echo         PROJECT FADE TO BLACK - ARPG 2D & IA
echo ========================================================
echo.

:: ── 1. Resolução do Artefato Executável e Compilação Sob Demanda ──────────────
:: Prioriza o JAR já empacotado na raiz; em seguida verifica a pasta 'target\'
:: produzida pelo Maven e, se ambos inexistirem, executa 'mvnw.cmd package'.
set JAR_FILE=fade-to-black.jar
if not exist "%JAR_FILE%" (
    if exist "target\fade-to-black.jar" (
        set JAR_FILE=target\fade-to-black.jar
    ) else (
        echo [!] O executavel fade-to-black.jar nao foi encontrado.
        echo [*] Compilando e gerando o pacote standalone...
        :: 'call' é obrigatório em scripts Batch para retornar o controle após mvnw.cmd;
        :: '-DskipTests' agiliza a inicialização final para o usuário.
        call .\mvnw.cmd package -DskipTests
        if errorlevel 1 (
            echo [ERRO] Falha ao compilar o projeto. Verifique o Java 17+.
            pause
            exit /b 1
        )
        set JAR_FILE=target\fade-to-black.jar
    )
)

:: ── 2. Execução na JVM (Via Classe Bootstrap Launcher) ────────────────────────
:: Inicia o executável autônomo em classpath padrão; caso o Java não esteja no
:: PATH ou seja incompatível (< 17), mantém a janela aberta para diagnóstico.
echo [*] Executando %JAR_FILE% diretamente com Java JRE...
java -jar "%JAR_FILE%"
if errorlevel 1 (
    echo.
    echo [ERRO] Nao foi possivel iniciar o jogo.
    echo Certifique-se de que o Java 17 ou superior esta instalado e no PATH.
    pause
)
