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

public class SyncApp {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Argumentos insuficientes. Uso: java syncapp.SyncApp <porta> <arquivo_nodes> <diretorio_sync>");
            return;
        }

        try {
            int listeningPort = Integer.parseInt(args[0]);
            String nodesFile = args[1];
            String syncDirectory = args[2];

            Set<String> networkNodes = new HashSet<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(nodesFile))) {
                String nodeAddress;
                while ((nodeAddress = reader.readLine()) != null) {
                    if (!nodeAddress.trim().isEmpty()) {
                        networkNodes.add(nodeAddress.trim());
                    }
                }
            }

            NodeInfo selfNode = new NodeInfo("127.0.0.1:" + listeningPort, networkNodes, syncDirectory);
            FileSyncManager syncManager = new FileSyncManager(syncDirectory, selfNode);
            NetworkListener listener = new NetworkListener(selfNode, listeningPort, syncManager);
            FolderMonitor monitor = new FolderMonitor(syncDirectory, syncManager);

            // Injeta a referência do monitor no gerenciador de sincronização
            syncManager.setFolderMonitor(monitor);

            new Thread(listener).start();
            new Thread(monitor).start();

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