package ai.apiprism.center.exceptions;

/**
 * 指定 revision 不存在或不属于目标 service/environment 时抛出。
 */
public class RevisionNotFoundException extends RuntimeException {

    public RevisionNotFoundException(String serviceName, String environment, String revisionId) {
        super("Revision not found: " + revisionId + " for " + serviceName + " (" + environment + ")");
    }
}
