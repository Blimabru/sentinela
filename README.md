# 🗡️ Project Fade To Black

> **Projeto Final - Disciplina de Inteligência Artificial**  
> Um ARPG (Action Role-Playing Game) 2D top-down com temática *Dark Fantasy*, focado em navegação autônoma de inimigos, percepção IA e algoritmos de busca gráfica.

---

## 📖 Sobre o Projeto
**Project Fade To Black** mergulha os jogadores em uma Era Medieval decadente. O protagonista, outrora um Paladino reverenciado, foi sequestrado por demônios e acordou desarmado nas profundezas de uma masmorra profana. Para sobreviver, ele precisa recuperar sua Fé e escapar, evitando criaturas que o caçam utilizando **algoritmos de Inteligência Artificial** baseados em campo de visão, audição e busca de rotas (A*).

---

## 🛠️ Tecnologias Utilizadas
- **Linguagem Principal:** Java 17
- **Motor Gráfico e Entrada:** JavaFX
- **Gerenciamento de Dependências:** Maven (via Maven Wrapper `mvnw`)
- **Arquitetura Base:** Orientação a Objetos, Matrizes Bidimensionais (TileMap), Algoritmos de Grafos (A-Star)

*(Este projeto foi desenvolvido **do zero** sem uso de Engines proprietárias como Unity ou Godot, conforme exigência acadêmica, a fim de aplicar as fundações de IA na unha).*

---

## 🚀 Como Executar o Jogo

O projeto já inclui o **Maven Wrapper** embarcado, o que significa que você **não precisa instalar o Maven** no seu computador para rodar o jogo.

### Pré-requisitos
- Ter o **Java Development Kit (JDK) 17** (ou versão superior) instalado e configurado nas variáveis de ambiente (`JAVA_HOME`).

### Passo a Passo

1. Faça o clone deste repositório em sua máquina:
   ```bash
   git clone <URL_DO_SEU_REPOSITORIO>
   ```
2. Pelo terminal (PowerShell, CMD ou Bash), navegue até a pasta raiz do projeto:
   ```bash
   cd sentinela
   ```
3. Execute o comando do Maven Wrapper correspondente ao seu sistema operacional:

   **No Windows:**
   ```powershell
   .\mvnw.cmd clean javafx:run
   ```

   **No Linux ou macOS:**
   ```bash
   ./mvnw clean javafx:run
   ```

> [!NOTE]  
> Ao rodar este comando pela primeira vez, o Maven Wrapper irá baixar automaticamente todas as dependências necessárias do JavaFX e compilar o código. Assim que finalizado, a janela do jogo se abrirá automaticamente!

---

## 🗺️ Entregas e Fases do Desenvolvimento

O desenvolvimento deste repositório acompanhou os módulos práticos da disciplina. Abaixo você encontra a explicação conceitual de cada entrega e onde encontrar o código-fonte correspondente.

### 1. Definição da Temática e História
A fase de planejamento onde a "alma" do jogo foi concebida. Definimos que seria um jogo *Dark Fantasy* no qual o personagem busca recuperar sua "fé".
- **Onde encontrar:** Os documentos textuais detalhando a história, arquétipos de inimigos e mecânicas de "fé" estão na pasta `docs/`.
- **Branch Histórica:** `docs/tema` (ou os primeiros commits da `main`).

### 2. Arquitetura do Jogo e Prática do Game Loop
Nesta etapa, construímos o "motor" do jogo do zero. O Game Loop é um ciclo infinito que roda dezenas de vezes por segundo, responsável por duas coisas vitais: **Atualizar a Lógica (Física/Matemática)** e **Renderizar os Gráficos na Tela**. Desvinculamos a velocidade da física da velocidade da máquina usando a variável `deltaTime`.
- **Como funciona:** O `AnimationTimer` do JavaFX pulsa o loop. A classe `GameWorld` propaga o `update()` para as entidades, enquanto o `Renderer` limpa e repinta o quadro.
- **Onde encontrar:** 
  - `src/main/java/br/edu/unex/sentinela/core/GameEngine.java` (O coração do Loop)
  - `src/main/java/br/edu/unex/sentinela/input/InputManager.java` (Captura de teclado)
  - `src/main/java/br/edu/unex/sentinela/rendering/Renderer.java` (Ilustrador de vídeo)
- **Branch Histórica:** `feature/gameloop`

### 3. Criação do TileMap e Colisões Físicas AABB
A passagem do cenário imaginário para a estrutura de dados. Criamos o mapa usando uma **Matriz Bidimensional (Grid)**, onde cada "célula" (Tile) possui propriedades como Custo de Movimento e Permissão de Passagem (Walkable).
- **Como funciona:** O Jogador possui uma caixa de colisão retangular (Bounding Box). Antes de permitir que ele mova seus pixels pela tela, o jogo checa os 4 cantos imaginários da caixa contra as células matriziais do `TileMap`. Se esbarrar em uma parede, o movimento é cancelado. Adicionamos também um bloco de "Lama" que reduz matematicamente a velocidade de travessia.
- **Onde encontrar:**
  - `src/main/java/br/edu/unex/sentinela/world/Tile.java` (A estrutura base do bloco de chão)
  - `src/main/java/br/edu/unex/sentinela/world/TileMap.java` (A matriz geográfica instanciada)
  - `src/main/java/br/edu/unex/sentinela/entity/Player.java` (Contém a lógica restritiva de checagem do AABB)
- **Branch Histórica:** `feature/tilemap`

### 4. Navegação Autônoma de Agentes com Algoritmo A* (A-Star)
Aqui, implementamos o cérebro autônomo do Inimigo. Em vez de simplesmente andar em linha reta e prender a cara na parede, ele navega pelo labirinto escolhendo ativamente a rota mais rápida e barata.
- **Como funciona:** O algoritmo avalia os vizinhos de cada bloco do mapa, calculando o "Custo Real (G)" mais o "Custo Estimado até o destino (H - Heurística de Manhattan)". Ele desvia de paredes pois elas têm custo infinito (intransitáveis), e pesa a decisão se vale a pena atravessar a Lama (mais lento) ou dar a volta pelo asfalto (mais longo). O inimigo repensa sua rota a cada 0.5s para não sobrecarregar o processador.
- **Onde encontrar:**
  - `src/main/java/br/edu/unex/sentinela/ai/Node.java` (Estrutura mental de avaliação de caminho)
  - `src/main/java/br/edu/unex/sentinela/ai/AStarPathfinder.java` (O motor lógico do A*)
  - `src/main/java/br/edu/unex/sentinela/entity/Enemy.java` (O agente Inimigo consumindo o A*)
- **Ferramentas de Debug:** O `Renderer` (linha amarela tracejada) extrai a rota pretendida da mente do Inimigo e desenha bolinhas no chão para visualizarmos as intenções algorítmicas ao vivo!
- **Branch Histórica:** `feature/astar`

---

*Desenvolvido no semestre de 2026.2 para fins de estudo acadêmico.*
