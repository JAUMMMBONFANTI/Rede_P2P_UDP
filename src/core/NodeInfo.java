package core;

import java.util.Set;

/**
 * Armazena as informações essenciais sobre um nó (peer) na rede P2P.
 * Esta classe funciona como um modelo de dados imutável (Data Class) para
 * carregar e transportar as configurações de cada nó durante a execução.
 * Ela centraliza todas as informações de identidade e configuração de um peer.
 */
public class NodeInfo {
    private String nodeAddress;
    private Set<String> networkNodes;
    private String syncFolderPath;

    /**
     * Construtor para inicializar as informações de um nó.
     * Cada parâmetro define uma característica fundamental para a operação do peer na rede.
     *
     * @param address       O endereço de rede (host:porta) deste nó específico.
     * É usado para que outros nós possam se comunicar com ele.
     * @param knownNodes    Um conjunto de strings contendo os endereços de outros nós
     * conhecidos na rede. Esta é a "lista de contatos" do peer.
     * @param folderPath    O caminho absoluto ou relativo para o diretório local que
     * será monitorado e sincronizado com os outros nós da rede.
     */
    public NodeInfo(String address, Set<String> knownNodes, String folderPath) {
        this.nodeAddress = address;
        this.networkNodes = knownNodes;
        this.syncFolderPath = folderPath;
    }

    /**
     * Retorna o endereço de rede deste nó.
     *
     * @return Uma string representando o endereço no formato "host:porta".
     */
    public String getNodeAddress() {
        return nodeAddress;
    }

    /**
     * Retorna o conjunto de nós conhecidos na rede por este peer.
     *
     * @return Um Set<String> contendo os endereços dos outros nós.
     * Este conjunto é usado para propagar as alterações de arquivos.
     */
    public Set<String> getNetworkNodes() {
        return networkNodes;
    }

    /**
     * Retorna o caminho do diretório que está sendo sincronizado.
     *
     * @return Uma string com o caminho da pasta monitorada por este nó.
     */
    public String getSyncFolderPath() {
        return syncFolderPath;
    }
}