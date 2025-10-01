package services;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Monitora um diretório local em busca de alterações (criação, modificação
 * e deleção de arquivos). Esta classe implementa `Runnable` para operar em uma
 * thread separada, verificando o estado da pasta periodicamente sem bloquear
 * a aplicação principal. Ele mantém um mapa do estado dos arquivos para
 * detectar mudanças.
 */
public class FolderMonitor implements Runnable {
    private final Path monitoredPath;
    private final FileSyncManager syncManager;
    private final Map<String, Long> knownFiles = new ConcurrentHashMap<>();

    /**
     * Construtor que inicializa o monitor de pastas.
     *
     * @param path    O caminho do diretório a ser monitorado.
     * @param manager A instância do FileSyncManager que será notificada sobre
     * quaisquer alterações detectadas no sistema de arquivos.
     */
    public FolderMonitor(String path, FileSyncManager manager) {
        this.monitoredPath = Paths.get(path);
        this.syncManager = manager;
        scanAndInitializeFiles();
    }

    /**
     * Realiza uma varredura inicial e síncrona no diretório para registrar o
     * estado dos arquivos no momento em que a aplicação é iniciada. Isso cria
     * uma "foto" inicial do diretório, preenchendo o mapa `knownFiles` com os
     * nomes dos arquivos e seus timestamps de última modificação.
     */
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

    /**
     * O corpo principal da thread do monitor. Este método entra em um loop infinito
     * que periodicamente (a cada 2 segundos) verifica o diretório para:
     * 1. Detectar arquivos novos ou modificados comparando o estado atual com o mapa `knownFiles`.
     * 2. Detectar arquivos apagados, comparando o mapa `knownFiles` com a lista atual de arquivos.
     * Para cada alteração detectada, ele invoca o método apropriado no FileSyncManager.
     */
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

                // --- VERIFICA NOVOS ARQUIVOS E MODIFICAÇÕES ---
                for (String fileName : currentFileNames) {
                    Path filePath = monitoredPath.resolve(fileName);
                    long lastModified = getLastModifiedTime(filePath);

                    if (!knownFiles.containsKey(fileName) || knownFiles.get(fileName) < lastModified) {
                        System.out.println("Detectado novo arquivo: " + fileName);
                        syncManager.broadcastFileCreation(filePath.toString());
                        knownFiles.put(fileName, lastModified);
                    }
                }

                // --- VERIFICA ARQUIVOS APAGADOS ---
                Set<String> deletedFileNames = new HashSet<>(knownFiles.keySet());
                deletedFileNames.removeAll(currentFileNames); // O que sobra são os deletados.

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

    /**
     * Retorna o timestamp da última modificação de um arquivo em milissegundos.
     * Este valor é usado para detectar se um arquivo foi alterado.
     *
     * @param path O caminho do arquivo.
     * @return O tempo da última modificação em milissegundos, ou -1 em caso de erro.
     */
    private long getLastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return -1;
        }
    }

    /**
     * Adiciona ou atualiza o registro de um arquivo conhecido no mapa `knownFiles`.
     * Este método é chamado externamente pelo FileSyncManager quando um arquivo
     * é recebido da rede, garantindo que o estado do monitor reflita as
     * alterações remotas.
     *
     * @param fileName      O nome do arquivo.
     * @param lastModified  O timestamp da última modificação do arquivo recebido.
     */
    public void updateKnownFile(String fileName, long lastModified) {
        knownFiles.put(fileName, lastModified);
    }

    /**
     * Remove o registro de um arquivo conhecido do mapa `knownFiles`.
     * Este método é chamado pelo FileSyncManager quando um comando de deleção
     * é recebido da rede.
     *
     * @param fileName O nome do arquivo a ser removido do registro.
     */
    public void removeKnownFile(String fileName) {
        knownFiles.remove(fileName);
    }
}