package core;

import java.util.Set;

public class NodeInfo {
    private String nodeAddress;
    private Set<String> networkNodes;
    private String syncFolderPath;

    public NodeInfo(String address, Set<String> knownNodes, String folderPath) {
        this.nodeAddress = address;
        this.networkNodes = knownNodes;
        this.syncFolderPath = folderPath;
    }

    public String getNodeAddress() {
        return nodeAddress;
    }

    public Set<String> getNetworkNodes() {
        return networkNodes;
    }

    public String getSyncFolderPath() {
        return syncFolderPath;
    }
}