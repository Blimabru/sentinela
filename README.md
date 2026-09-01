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

## 🚀 Como Executar Localmente

Você não precisa ter o Maven instalado globalmente, pois o projeto acompanha o **Maven Wrapper**.

1. Certifique-se de ter o **Java 17 (JDK)** ou superior instalado.
2. Clone o repositório.
3. Abra o terminal (PowerShell ou CMD) na pasta raiz do projeto.
4. Execute o seguinte comando:

```powershell
.\mvnw.cmd clean javafx:run
```

Uma janela do JavaFX se abrirá renderizando o *Game Loop*, o mapa, o personagem e as lógicas de Inteligência Artificial implementadas.

---

## 🗺️ Entregas e Fases do Desenvolvimento

O desenvolvimento deste repositório acompanhou os módulos práticos da disciplina:

- [x] **Entrega 1:** Definição da Temática e História (Branch: `docs/tema`)
- [x] **Entrega 2:** Arquitetura do Jogo e Prática do Game Loop (Branch: `feature/gameloop`)
- [x] **Entrega 3:** Criação do TileMap e Colisões Físicas AABB (Branch: `feature/tilemap`)
- [x] **Entrega 4:** Navegação Autônoma de Agentes com Algoritmo A* (Branch: `feature/astar`)

*(O andamento principal encontra-se unificado na branch `develop` / `main`).*

---

## 🧠 Lógicas de Inteligência Artificial (Atuais)
- **Matriz de Navegabilidade:** Um mundo estruturado em blocos de 40x40 pixels, com diferentes "Custos de Movimento" (Ex: Lama custa mais caro que Asfalto limpo).
- **Algoritmo A* (A-Star):** O Inimigo (*Quadrado Vermelho*) possui uma lógica autônoma que busca o caminho mais curto e eficiente até o jogador (*Quadrado Azul*). Ele recalcula a rota apenas a cada 0.5 segundos para poupar memória, desviando inteligentemente das paredes e priorizando caminhos com menor "custo".
- **Ferramentas de Debug Visual:** O motor desenha pequenos pontos amarelos na tela simulando os *nós (nodes)* em tempo real nos quais o inimigo pretende pisar, para comprovação e debug do comportamento do A*.

---

*Desenvolvido no semestre de 2026.2 para fins de estudo acadêmico.*
