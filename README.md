# Sistema de Sincronização de Arquivos P2P em Java

## 🎯 Problema Principal Abordado

O objetivo desse projeto é implementar um sistema P2P (peer-to-peer) com protocolo de transporte UDP, onde um diretório específico é automaticamente sincronizado entre todos os nós (peers) da rede, no momento de criação e deleção.

## 🚀 Tecnologias e Pré-requisitos

### Linguagem Utilizada
* **Java**

### O que é necessário instalar para rodar?
* **Para execução com Docker:**
    * [**Docker Desktop**](https://www.docker.com/products/docker-desktop/)

* **Para execução Local:**
    * [**Java Development Kit (JDK)**](https://adoptium.net/pt-PT/temurin/releases/?version=11) - Versão 11 ou superior.

---

## 🛠️ Comandos para Execução e Testes

Existem duas formas de executar o projeto: utilizando Docker ou localmente.

### 🐳 Execução com Docker

Esta é a forma mais simples de testar o ambiente, pois já inclui todas as dependências.

#### Passo 1: Construir e Iniciar os Contêineres
Este comando irá compilar o código e iniciar os três peers em segundo plano.
```bash
docker-compose up -d --build
```

#### Passo 2: Acompanhar os Logs
Para ver as mensagens dos peers em tempo real.
```bash
docker-compose logs -f
```

#### Passo 3: Testar a Sincronização
Abra um **novo terminal** para executar os comandos abaixo.

* **Para criar um ficheiro:**
    ```bash
    docker exec -it peer1 sh -c "echo 'teste via docker' > /app/tmp/peer1/teste_docker.txt"
    ```

* **Para apagar o ficheiro:**
    ```bash
    docker exec -it peer1 sh -c "rm /app/tmp/peer1/teste_docker.txt"
    ```

#### Passo 4: Parar e Remover os Contêineres
Quando terminar, este comando irá parar e limpar todos os contêineres.
```bash
docker-compose down
```

---

### 🖥️ Execução Local

Para executar localmente, é necessário ter o **JDK 11 ou superior** instalado.

#### Passo 1: Compilar o Código
Este comando compila todos os ficheiros `.java` e coloca os resultados na pasta `bin`.
```bash
javac -d bin src/syncapp/SyncApp.java src/core/NodeInfo.java src/services/*.java src/network/NetworkListener.java
```

#### Passo 2: Executar os Três Peers
Abra **três terminais separados** e execute um comando em cada um.

* **Terminal 1 (Peer 1):**
    ```bash
    java -cp bin syncapp.SyncApp 5000 knownPeers/knownPeers1_local.txt tmp/peer1
    ```

* **Terminal 2 (Peer 2):**
    ```bash
    java -cp bin syncapp.SyncApp 5001 knownPeers/knownPeers2_local.txt tmp/peer2
    ```

* **Terminal 3 (Peer 3):**
    ```bash
    java -cp bin syncapp.SyncApp 5002 knownPeers/knownPeers3_local.txt tmp/peer3
    ```

#### Passo 3: Testar a Sincronização
Use um **quarto terminal** para manipular os ficheiros.

* **Para criar um ficheiro:**
    ```bash
    echo "teste local" > tmp/peer1/teste.txt
    ```

* **Para apagar um ficheiro:**
    * No Windows:
        ```cmd
        del tmp\peer1\teste.txt
        ```
    * No Linux ou macOS:
        ```bash
        rm tmp/peer1/teste.txt
        ```