import java.io.Serializable;

public class FileInfo implements Serializable {
    boolean owned;
    String originServer;
    String originSuperpeer;
    Integer version;
    boolean valid;
    Integer TTR;

    public boolean isOwned() {
        return owned;
    }

    public boolean isValid() {
        return valid;
    }

    public Integer getVersion() {
        return version;
    }

    public String getOriginServer() {
        return originServer;
    }

    public Integer getTTR() {
        return TTR;
    }

    public void setOriginServer(String originServer) {
        this.originServer = originServer;
    }

    public void setOwned(boolean owned) {
        this.owned = owned;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public void setTTR(Integer TTR) {
        this.TTR = TTR;
    }

    public String getOriginSuperpeer() {
        return originSuperpeer;
    }

    public void setOriginSuperpeer(String originSuperpeer) {
        this.originSuperpeer = originSuperpeer;
    }
}
