package syncapp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import core.NodeInfo;
import services.FolderMonitor;
import services.FileSyncManager;
import network.NetworkListener;

/**
 * Classe principal que inicializa e orquestra a aplicação de sincronização P2P.
 * * O objetivo desta classe é configurar todo o ambiente necessário para um nó (peer) operar.
 * Ela realiza as seguintes tarefas:
 * 1. Valida e processa os argumentos de linha de comando (porta, arquivo de peers e diretório).
 * 2. Lê a lista de outros nós conhecidos na rede a partir de um arquivo.
 * 3. Instancia os componentes chave da aplicação:
 * - NodeInfo: Armazena os dados de configuração do nó.
 * - FileSyncManager: Gerencia a lógica de envio e recebimento de arquivos.
 * - NetworkListener: Escuta por mensagens da rede em uma thread separada.
 * - FolderMonitor: Monitora o diretório local por alterações em outra thread.
 * 4. Inicia as threads para que a aplicação possa lidar com operações de rede e
 * monitoramento de arquivos simultaneamente.
 */
public class SyncApp {

    /**
     * Ponto de entrada da aplicação (main). Este método configura e inicia um nó P2P.
     * * Ele espera três argumentos para funcionar corretamente:
     * - A porta UDP que este nó utilizará para escutar mensagens.
     * - O caminho para um arquivo de texto contendo os endereços (host:porta) dos outros peers na rede.
     * - O caminho para o diretório local que será sincronizado com a rede.
     *
     * @param args Um array de strings contendo os argumentos da linha de comando.
     * - args[0]: A porta de escuta.
     * - args[1]: O caminho para o arquivo de nós conhecidos.
     * - args[2]: O caminho para o diretório de sincronização.
     */
    public static void main(String[] args) {
        // 1. Validação dos Argumentos de Entrada
        if (args.length < 3) {
            System.err.println("Argumentos insuficientes. Uso: java syncapp.SyncApp <porta> <arquivo_nodes> <diretorio_sync>");
            return;
        }

        try {
            // 2. Leitura e Conversão dos Argumentos
            int listeningPort = Integer.parseInt(args[0]);
            String nodesFile = args[1];
            String syncDirectory = args[2];

            // 3. Leitura do Arquivo de Peers Conhecidos
            Set<String> networkNodes = new HashSet<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(nodesFile))) {
                String nodeAddress;
                while ((nodeAddress = reader.readLine()) != null) {
                    if (!nodeAddress.trim().isEmpty()) {
                        networkNodes.add(nodeAddress.trim());
                    }
                }
            }

            // 4. Instanciação dos Componentes Principais
            // Cria um objeto com as informações deste nó.
            NodeInfo selfNode = new NodeInfo("127.0.0.1:" + listeningPort, networkNodes, syncDirectory);
            
            // Cria o gerenciador que lida com a lógica de sincronização.
            FileSyncManager syncManager = new FileSyncManager(syncDirectory, selfNode);
            
            // Cria o listener que escutará a rede por mensagens de outros peers.
            NetworkListener listener = new NetworkListener(selfNode, listeningPort, syncManager);
            
            // Cria o monitor que vigiará a pasta local por mudanças.
            FolderMonitor monitor = new FolderMonitor(syncDirectory, syncManager);

            // 5. Injeção de Dependência
            // Conecta o gerenciador ao monitor para evitar loops de sincronização.
            syncManager.setFolderMonitor(monitor);

            // 6. Inicialização das Threads
            // Inicia o listener de rede em uma nova thread para não bloquear a execução.
            new Thread(listener).start();
            
            // Inicia o monitor de pastas em outra thread.
            new Thread(monitor).start();

            // 7. Feedback Final para o Usuário
            System.out.println(">>> Nó iniciado e operando na porta " + listeningPort);
            System.out.println(">>> Diretório sincronizado: " + syncDirectory);
            System.out.println(">>> Nós conhecidos na rede: " + networkNodes);

        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo de nós: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("A porta fornecida não é um número válido.");
        }
    }
}