package network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import core.NodeInfo;
import services.FileSyncManager;

/**
 * Responsável por escutar ativamente a rede por pacotes UDP de outros peers.
 * Esta classe implementa a interface `Runnable`, permitindo que opere em uma
 * thread dedicada para não bloquear a execução da aplicação principal. Sua única
 * função é receber mensagens, identificar o comando (CREATE/DELETE) e delegar
 * a ação para o FileSyncManager.
 */
public class NetworkListener implements Runnable {
    private final NodeInfo selfNode;
    private final DatagramSocket udpSocket;
    private final FileSyncManager syncManager;
    
    /**
     * Construtor que inicializa o listener de rede em uma porta UDP específica.
     * Ele cria e abre o `DatagramSocket` que ficará aguardando pacotes.
     *
     * @param node      O objeto contendo as informações do nó atual (selfNode).
     * @param port      A porta UDP em que o socket deve se vincular para escutar.
     * @param manager   A instância do FileSyncManager que será usada para
     * processar os comandos de sincronização recebidos.
     */
    public NetworkListener(NodeInfo node, int port, FileSyncManager manager) {
        this.selfNode = node;
        this.syncManager = manager;
        try {
            this.udpSocket = new DatagramSocket(port);
        } catch (Exception e) {
            throw new RuntimeException("Não foi possível iniciar o listener na porta " + port, e);
        }
    }
    
    /**
     * O corpo principal da thread do listener. Este método entra em um loop
     * infinito (`while(true)`) que executa os seguintes passos repetidamente:
     * 1. Aguarda (bloqueia) até que um pacote UDP seja recebido na porta.
     * 2. Extrai os dados do pacote recebido.
     * 3. Delega os dados para o método `processPacket` para interpretação e ação.
     * 4. Lida com quaisquer exceções que possam ocorrer durante o recebimento.
     */
    @Override
    public void run() {
        byte[] receiveBuffer = new byte[65507]; // Aloca buffer para o tamanho máximo de um pacote UDP.
        System.out.println("Listener de rede ativo, aguardando pacotes...");
        
        while(true) {
            DatagramPacket receivedPacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            try {
                udpSocket.receive(receivedPacket); // Ponto de bloqueio, aguarda pacote.
                
                String rawMessage = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
                System.out.println("[" + selfNode.getNodeAddress() + "] pacote recebido de outro nó: " + rawMessage.substring(0, Math.min(60, rawMessage.length())) + "...");
                
                processPacket(receivedPacket.getData(), receivedPacket.getLength());
                
            } catch(Exception e) {
                System.err.println("Erro ao processar pacote UDP: " + e.getMessage());
            }
        }
    }
    
    /**
     * Analisa e processa o conteúdo de um pacote UDP recebido.
     * Ele converte o array de bytes em uma string, quebra a mensagem usando "|"
     * como delimitador para extrair o comando e seus argumentos, e então
     * invoca o método apropriado no FileSyncManager.
     *
     * O protocolo da mensagem é: "COMANDO|ARGUMENTO1|ARGUMENTO2..."
     * - Para CREATE: "CREATE|nomeDoArquivo|conteúdoDoArquivo"
     * - Para DELETE: "DELETE|nomeDoArquivo"
     *
     * @param data      Os dados brutos (bytes) do pacote recebido.
     * @param length    O comprimento real dos dados no array de bytes.
     */
    private void processPacket(byte[] data, int length) {
        String messageStr = new String(data, 0, length);
        String[] parts = messageStr.split("\\|", 3);
        String command = parts[0];

        if ("CREATE".equals(command) && parts.length == 3) {
            String fileName = parts[1];
            // Calcula o início do conteúdo do arquivo nos dados brutos.
            int contentIndex = command.length() + fileName.length() + 2; // "CREATE|fileName|"
            byte[] fileContent = Arrays.copyOfRange(data, contentIndex, length);
            syncManager.persistReceivedFile(fileName, fileContent);
        } else if ("DELETE".equals(command) && parts.length == 2) {
            String fileName = parts[1];
            syncManager.eraseReceivedFile(fileName);
        }
    }
}