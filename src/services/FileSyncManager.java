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

public class FileSyncManager {
    private final String syncDirectory;
    private final NodeInfo currentNode;
    private FolderMonitor folderMonitor;

    public FileSyncManager(String baseDir, NodeInfo node) {
        this.syncDirectory = baseDir;
        this.currentNode = node;
    }

    public void setFolderMonitor(FolderMonitor monitor) {
        this.folderMonitor = monitor;
    }

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

    public void broadcastFileDeletion(String fileName) {
        System.out.println("Propagando exclusão do arquivo: " + fileName);
        String packetContent = "DELETE|" + fileName;
        propagateToNetwork(packetContent.getBytes());
    }

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