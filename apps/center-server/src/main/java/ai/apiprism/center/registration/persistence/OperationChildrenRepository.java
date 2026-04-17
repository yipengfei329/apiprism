package ai.apiprism.center.registration.persistence;

import ai.apiprism.center.registration.persistence.PersistenceRows.OperationParameterRow;
import ai.apiprism.center.registration.persistence.PersistenceRows.OperationRequestBodyRow;
import ai.apiprism.center.registration.persistence.PersistenceRows.OperationResponseRow;
import ai.apiprism.model.CanonicalParameter;
import ai.apiprism.model.CanonicalRequestBody;
import ai.apiprism.model.CanonicalResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hypersistence.tsid.TSID;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * operation 的子表访问层：parameters / request_bodies / responses。
 * 结构变化时一次性 delete + insert 替换，简单稳。
 */
@Repository
public class OperationChildrenRepository {

    private static final TypeReference<Map<String, Object>> MAP_REF = new TypeReference<>() {
    };

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public OperationChildrenRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void deleteByOperationId(String operationId) {
        jdbc.update("DELETE FROM operation_parameters WHERE operation_id = ?", operationId);
        jdbc.update("DELETE FROM operation_request_bodies WHERE operation_id = ?", operationId);
        jdbc.update("DELETE FROM operation_responses WHERE operation_id = ?", operationId);
    }

    public void insertParameters(String operationId, List<CanonicalParameter> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return;
        }
        List<CanonicalParameter> snapshot = List.copyOf(parameters);
        jdbc.batchUpdate("""
                INSERT INTO operation_parameters (id, operation_id, location, name, required, schema_json, sort_order)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                CanonicalParameter p = snapshot.get(i);
                ps.setString(1, TSID.Factory.getTsid().toString());
                ps.setString(2, operationId);
                ps.setString(3, p.getLocation());
                ps.setString(4, p.getName());
                ps.setBoolean(5, p.isRequired());
                ps.setString(6, toJson(p.getSchema()));
                ps.setInt(7, i);
            }

            @Override
            public int getBatchSize() {
                return snapshot.size();
            }
        });
    }

    public void insertRequestBody(String operationId, CanonicalRequestBody body) {
        if (body == null) {
            return;
        }
        jdbc.update("""
                INSERT INTO operation_request_bodies (id, operation_id, content_type, required, schema_json)
                VALUES (?, ?, ?, ?, ?)
                """,
                TSID.Factory.getTsid().toString(), operationId,
                body.getContentType(), body.isRequired(), toJson(body.getSchema()));
    }

    public void insertResponses(String operationId, List<CanonicalResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return;
        }
        List<CanonicalResponse> snapshot = List.copyOf(responses);
        jdbc.batchUpdate("""
                INSERT INTO operation_responses (id, operation_id, status_code, content_type, schema_json)
                VALUES (?, ?, ?, ?, ?)
                """, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                CanonicalResponse r = snapshot.get(i);
                ps.setString(1, TSID.Factory.getTsid().toString());
                ps.setString(2, operationId);
                ps.setString(3, r.getStatusCode());
                ps.setString(4, r.getContentType());
                ps.setString(5, toJson(r.getSchema()));
            }

            @Override
            public int getBatchSize() {
                return snapshot.size();
            }
        });
    }

    public List<OperationParameterRow> findParameters(String operationId) {
        return jdbc.query("""
                SELECT id, operation_id, location, name, required, schema_json, sort_order
                  FROM operation_parameters WHERE operation_id = ? ORDER BY sort_order, name
                """, PARAM_MAPPER, operationId);
    }

    public java.util.Optional<OperationRequestBodyRow> findRequestBody(String operationId) {
        return jdbc.query("""
                SELECT id, operation_id, content_type, required, schema_json
                  FROM operation_request_bodies WHERE operation_id = ?
                """, BODY_MAPPER, operationId).stream().findFirst();
    }

    public List<OperationResponseRow> findResponses(String operationId) {
        return jdbc.query("""
                SELECT id, operation_id, status_code, content_type, schema_json
                  FROM operation_responses WHERE operation_id = ? ORDER BY status_code
                """, RESP_MAPPER, operationId);
    }

    public Map<String, List<OperationParameterRow>> findParametersByOperationIds(Collection<String> ids) {
        return batchByOperation(ids, "operation_parameters", PARAM_MAPPER,
                "id, operation_id, location, name, required, schema_json, sort_order");
    }

    public Map<String, OperationRequestBodyRow> findRequestBodiesByOperationIds(Collection<String> ids) {
        Map<String, List<OperationRequestBodyRow>> grouped = batchByOperation(ids,
                "operation_request_bodies", BODY_MAPPER,
                "id, operation_id, content_type, required, schema_json");
        Map<String, OperationRequestBodyRow> singleton = new HashMap<>();
        grouped.forEach((opId, rows) -> {
            if (!rows.isEmpty()) {
                singleton.put(opId, rows.get(0));
            }
        });
        return singleton;
    }

    public Map<String, List<OperationResponseRow>> findResponsesByOperationIds(Collection<String> ids) {
        return batchByOperation(ids, "operation_responses", RESP_MAPPER,
                "id, operation_id, status_code, content_type, schema_json");
    }

    public Map<String, Object> parseSchema(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, MAP_REF);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize schema JSON", e);
        }
    }

    private <T> Map<String, List<T>> batchByOperation(Collection<String> ids,
                                                     String table,
                                                     RowMapper<T> mapper,
                                                     String columns) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(",", ids.stream().map(x -> "?").toList());
        String sql = "SELECT " + columns + " FROM " + table
                + " WHERE operation_id IN (" + placeholders + ")";
        List<T> rows = jdbc.query(sql, mapper, ids.toArray());
        Map<String, List<T>> grouped = new HashMap<>();
        for (T row : rows) {
            String opId = extractOperationId(row);
            grouped.computeIfAbsent(opId, k -> new ArrayList<>()).add(row);
        }
        return grouped;
    }

    private static String extractOperationId(Object row) {
        if (row instanceof OperationParameterRow p) return p.operationId();
        if (row instanceof OperationRequestBodyRow b) return b.operationId();
        if (row instanceof OperationResponseRow r) return r.operationId();
        throw new IllegalArgumentException("Unknown child row type: " + row.getClass());
    }

    private String toJson(Map<String, Object> schema) {
        if (schema == null || schema.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(schema);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize schema to JSON", e);
        }
    }

    private static final RowMapper<OperationParameterRow> PARAM_MAPPER = (rs, i) -> new OperationParameterRow(
            rs.getString("id"),
            rs.getString("operation_id"),
            rs.getString("location"),
            rs.getString("name"),
            rs.getBoolean("required"),
            rs.getString("schema_json"),
            rs.getInt("sort_order"));

    private static final RowMapper<OperationRequestBodyRow> BODY_MAPPER = (rs, i) -> new OperationRequestBodyRow(
            rs.getString("id"),
            rs.getString("operation_id"),
            rs.getString("content_type"),
            rs.getBoolean("required"),
            rs.getString("schema_json"));

    private static final RowMapper<OperationResponseRow> RESP_MAPPER = (rs, i) -> new OperationResponseRow(
            rs.getString("id"),
            rs.getString("operation_id"),
            rs.getString("status_code"),
            rs.getString("content_type"),
            rs.getString("schema_json"));
}
