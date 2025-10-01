package services;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FolderMonitor implements Runnable {
    private final Path monitoredPath;
    private final FileSyncManager syncManager;
    private final Map<String, Long> knownFiles = new ConcurrentHashMap<>();

    public FolderMonitor(String path, FileSyncManager manager) {
        this.monitoredPath = Paths.get(path);
        this.syncManager = manager;
        scanAndInitializeFiles();
    }

    private void scanAndInitializeFiles() {
        System.out.println("Realizando verificação inicial do diretório: " + monitoredPath);
        try (Stream<Path> stream = Files.list(monitoredPath)) {
            stream.forEach(path -> {
                if (Files.isRegularFile(path)) {
                    knownFiles.put(path.getFileName().toString(), getLastModifiedTime(path));
                }
            });
        } catch (IOException e) {
            System.err.println("Erro na verificação inicial do diretório: " + e.getMessage());
        }
        System.out.println("Verificação inicial concluída. Arquivos conhecidos: " + knownFiles.keySet());
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(2000);

                Set<String> currentFileNames;
                try (Stream<Path> stream = Files.list(monitoredPath)) {
                    currentFileNames = stream
                        .filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .collect(Collectors.toSet());
                }

                for (String fileName : currentFileNames) {
                    Path filePath = monitoredPath.resolve(fileName);
                    long lastModified = getLastModifiedTime(filePath);

                    if (!knownFiles.containsKey(fileName) || knownFiles.get(fileName) < lastModified) {
                        System.out.println("Detectado novo arquivo: " + fileName);
                        syncManager.broadcastFileCreation(filePath.toString());
                        knownFiles.put(fileName, lastModified);
                    }
                }

                Set<String> deletedFileNames = new HashSet<>(knownFiles.keySet());
                deletedFileNames.removeAll(currentFileNames);

                for (String fileName : deletedFileNames) {
                    System.out.println("Detectado arquivo apagado: " + fileName);
                    syncManager.broadcastFileDeletion(fileName);
                    knownFiles.remove(fileName);
                }

            } catch (IOException e) {
                System.err.println("Erro ao monitorar o diretório: " + e.getMessage());
            } catch (InterruptedException e) {
                System.err.println("Monitoramento de diretório interrompido.");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private long getLastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return -1;
        }
    }

    public void updateKnownFile(String fileName, long lastModified) {
        knownFiles.put(fileName, lastModified);
    }

    public void removeKnownFile(String fileName) {
        knownFiles.remove(fileName);
    }
}