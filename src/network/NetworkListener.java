package network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import core.NodeInfo;
import services.FileSyncManager;

public class NetworkListener implements Runnable {
    private final NodeInfo selfNode;
    private final DatagramSocket udpSocket;
    private final FileSyncManager syncManager;
    
    public NetworkListener(NodeInfo node, int port, FileSyncManager manager) {
        this.selfNode = node;
        this.syncManager = manager;
        try {
            this.udpSocket = new DatagramSocket(port);
        } catch (Exception e) {
            throw new RuntimeException("Não foi possível iniciar o listener na porta " + port, e);
        }
    }
    
    @Override
    public void run() {
        byte[] receiveBuffer = new byte[65507]; // Tamanho máximo UDP
        System.out.println("Listener de rede ativo, aguardando pacotes...");
        
        while(true) {
            DatagramPacket receivedPacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            try {
                udpSocket.receive(receivedPacket);
                
                String rawMessage = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
                System.out.println("[" + selfNode.getNodeAddress() + "] pacote recebido de outro nó: " + rawMessage.substring(0, Math.min(60, rawMessage.length())) + "...");
                
                processPacket(receivedPacket.getData(), receivedPacket.getLength());
                
            } catch(Exception e) {
                System.err.println("Erro ao processar pacote UDP: " + e.getMessage());
            }
        }
    }
    
    private void processPacket(byte[] data, int length) {
        String messageStr = new String(data, 0, length);
        String[] parts = messageStr.split("\\|", 3);
        String command = parts[0];

        if ("CREATE".equals(command) && parts.length == 3) {
            String fileName = parts[1];
            int contentIndex = command.length() + fileName.length() + 2; // "CREATE|fileName|"
            byte[] fileContent = Arrays.copyOfRange(data, contentIndex, length);
            syncManager.persistReceivedFile(fileName, fileContent);
        } else if ("DELETE".equals(command) && parts.length == 2) {
            String fileName = parts[1];
            syncManager.eraseReceivedFile(fileName);
        }
    }
}