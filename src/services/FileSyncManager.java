package services;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import core.NodeInfo;

/**
 * Orquestra a lógica de sincronização de arquivos. Esta classe atua como o
 * cérebro das operações de P2P, sendo responsável por duas funções principais:
 * 1. **Propagação:** Quando o FolderMonitor detecta uma alteração local, esta
 * classe é acionada para construir e enviar a mensagem (CREATE ou DELETE)
 * para todos os outros peers da rede.
 * 2. **Persistência:** Quando o NetworkListener recebe uma mensagem da rede,
 * esta classe é responsável por aplicar a alteração no sistema de arquivos
 * local (salvando ou deletando um arquivo).
 */
public class FileSyncManager {
    private final String syncDirectory;
    private final NodeInfo currentNode;
    private FolderMonitor folderMonitor;

    /**
     * Construtor do gerenciador de sincronização.
     *
     * @param baseDir O caminho do diretório que será sincronizado.
     * @param node    O objeto de configuração contendo as informações do nó atual.
     */
    public FileSyncManager(String baseDir, NodeInfo node) {
        this.syncDirectory = baseDir;
        this.currentNode = node;
    }

    /**
     * Estabelece a referência ao FolderMonitor. Esta "injeção de dependência" é
     * crucial para resolver o problema de looping, permitindo que o FileSyncManager
     * notifique o monitor sobre alterações remotas para que ele não as trate como
     * alterações locais.
     *
     * @param monitor A instância ativa do FolderMonitor.
     */
    public void setFolderMonitor(FolderMonitor monitor) {
        this.folderMonitor = monitor;
    }

    /**
     * Dispara a propagação da criação de um arquivo. Este método é chamado
     * pelo FolderMonitor quando um novo arquivo (ou uma modificação) é detectado.
     * Ele lê o conteúdo do arquivo, monta o pacote de dados no formato "CREATE|nome|conteúdo"
     * e o envia para a rede.
     *
     * @param filePath O caminho completo do arquivo local que foi criado ou modificado.
     */
    public void broadcastFileCreation(String filePath) {
        System.out.println("Iniciando transmissão do arquivo: " + filePath);
        try {
            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
            String name = Paths.get(filePath).getFileName().toString();
            
            String header = "CREATE|" + name + "|";
            byte[] headerBytes = header.getBytes();
            byte[] packetData = new byte[headerBytes.length + fileContent.length];
            
            System.arraycopy(headerBytes, 0, packetData, 0, headerBytes.length);
            System.arraycopy(fileContent, 0, packetData, headerBytes.length, fileContent.length);
            
            propagateToNetwork(packetData);
            
        } catch (IOException e) {
            System.err.println("Falha ao ler o arquivo para envio: " + e.getMessage());
        }
    }

    /**
     * Dispara a propagação da exclusão de um arquivo. Este método é chamado
     * pelo FolderMonitor quando detecta que um arquivo foi removido do diretório.
     * Ele monta a mensagem no formato "DELETE|nome" e a envia para a rede.
     *
     * @param fileName O nome do arquivo que foi deletado localmente.
     */
    public void broadcastFileDeletion(String fileName) {
        System.out.println("Propagando exclusão do arquivo: " + fileName);
        String packetContent = "DELETE|" + fileName;
        propagateToNetwork(packetContent.getBytes());
    }

    /**
     * Método genérico para enviar um pacote de dados UDP para todos os nós
     * listados no arquivo de configuração. Ele itera sobre a lista de peers,
     * resolve o endereço de cada um e envia o pacote de dados.
     *
     * @param data O payload (array de bytes) a ser enviado no DatagramPacket.
     */
    private void propagateToNetwork(byte[] data) {
        Set<String> nodes = currentNode.getNetworkNodes();
        for (String nodeAddr : nodes) {
            try {
                String[] parts = nodeAddr.split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);
                
                InetAddress targetAddress = InetAddress.getByName(host);
                DatagramPacket packet = new DatagramPacket(data, data.length, targetAddress, port);
                
                try (DatagramSocket socket = new DatagramSocket()) {
                    socket.send(packet);
                }
                
                System.out.println("Operação enviada para o nó: " + nodeAddr);
                
            } catch (Exception e) {
                System.err.println("Falha ao contatar o nó " + nodeAddr + ": " + e.getMessage());
            }
        }
    }

    /**
     * Persiste (salva em disco) um arquivo recebido de outro nó da rede.
     * Este método é chamado pelo NetworkListener ao receber um comando "CREATE".
     * Após salvar, ele notifica o FolderMonitor para atualizar seu mapa interno
     * de arquivos, evitando que a alteração seja reenviada para a rede.
     *
     * @param fileName O nome do arquivo a ser criado no diretório local.
     * @param content  O conteúdo binário do arquivo a ser salvo.
     */
    public void persistReceivedFile(String fileName, byte[] content) {
        try {
            Path destinationPath = Paths.get(syncDirectory, fileName);
            Files.write(destinationPath, content);
            System.out.println("Arquivo recebido e salvo com sucesso em: " + destinationPath);
            
            if (folderMonitor != null) {
                long lastModified = Files.getLastModifiedTime(destinationPath).toMillis();
                folderMonitor.updateKnownFile(fileName, lastModified);
            }
        } catch (IOException e) {
            System.err.println("Erro ao persistir arquivo recebido: " + e.getMessage());
        }
    }

    /**
     * Remove um arquivo do disco local. Este método é chamado pelo
     * NetworkListener ao receber um comando "DELETE". Após apagar, ele notifica o
     * FolderMonitor para que o arquivo seja removido do seu mapa interno.
     *
     * @param fileName O nome do arquivo a ser deletado no diretório local.
     */
    public void eraseReceivedFile(String fileName) {
        try {
            Path targetPath = Paths.get(syncDirectory, fileName);
            Files.deleteIfExists(targetPath);
            System.out.println("Arquivo remoto deletado: " + fileName);

            if (folderMonitor != null) {
                folderMonitor.removeKnownFile(fileName);
            }
        } catch (IOException e) {
            System.err.println("Erro ao deletar arquivo recebido: " + e.getMessage());
        }
    }
}