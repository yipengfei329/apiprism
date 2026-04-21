package ai.apiprism.center.revision;

import ai.apiprism.center.registration.SemanticHasher;
import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalServiceSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 计算两个 revision 快照之间的接口变更情况。
 * 以 "METHOD PATH" 作为接口身份标识键，比较新增、删除、修改。
 */
public class RevisionDiffCalculator {

    private RevisionDiffCalculator() {
    }

    /**
     * @param previous 前驱 revision 的快照，首个 revision 时传 null
     * @param current  当前 revision 的快照
     */
    public static RevisionDiff compute(CanonicalServiceSnapshot previous, CanonicalServiceSnapshot current) {
        Map<String, CanonicalOperation> currentOps = flattenOps(current);
        int total = currentOps.size();

        if (previous == null) {
            List<RevisionDiff.EndpointRef> added = currentOps.values().stream()
                    .map(RevisionDiffCalculator::toRef)
                    .toList();
            return new RevisionDiff(total, added, List.of(), List.of());
        }

        Map<String, CanonicalOperation> previousOps = flattenOps(previous);

        List<RevisionDiff.EndpointRef> added = new ArrayList<>();
        List<RevisionDiff.EndpointRef> modified = new ArrayList<>();

        for (Map.Entry<String, CanonicalOperation> entry : currentOps.entrySet()) {
            String key = entry.getKey();
            CanonicalOperation currentOp = entry.getValue();
            CanonicalOperation previousOp = previousOps.get(key);
            if (previousOp == null) {
                added.add(toRef(currentOp));
            } else if (!SemanticHasher.operationJson(currentOp).equals(SemanticHasher.operationJson(previousOp))) {
                modified.add(toRef(currentOp));
            }
        }

        List<RevisionDiff.EndpointRef> removed = previousOps.entrySet().stream()
                .filter(e -> !currentOps.containsKey(e.getKey()))
                .map(e -> toRef(e.getValue()))
                .toList();

        return new RevisionDiff(total, added, removed, modified);
    }

    /**
     * 将 snapshot 中所有 operation 展平为 "METHOD PATH" → operation 的有序映射。
     */
    private static Map<String, CanonicalOperation> flattenOps(CanonicalServiceSnapshot snapshot) {
        Map<String, CanonicalOperation> ops = new LinkedHashMap<>();
        if (snapshot.getGroups() == null) {
            return ops;
        }
        for (var group : snapshot.getGroups()) {
            if (group.getOperations() == null) {
                continue;
            }
            for (CanonicalOperation op : group.getOperations()) {
                String key = op.getMethod().toUpperCase() + " " + op.getPath();
                ops.putIfAbsent(key, op);
            }
        }
        return ops;
    }

    private static RevisionDiff.EndpointRef toRef(CanonicalOperation op) {
        return new RevisionDiff.EndpointRef(op.getMethod(), op.getPath(), op.getOperationId(), op.getSummary());
    }
}
