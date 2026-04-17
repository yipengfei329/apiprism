package ai.apiprism.center.registration.persistence;

import ai.apiprism.center.registration.persistence.PersistenceRows.EnvironmentRow;
import io.hypersistence.tsid.TSID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * environments 表访问层。按 code 全局唯一。
 */
@Repository
public class EnvironmentRepository {

    private final JdbcTemplate jdbc;

    public EnvironmentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<EnvironmentRow> findByCode(String code) {
        return jdbc.query("SELECT id, code FROM environments WHERE code = ?", MAPPER, code)
                .stream().findFirst();
    }

    public Optional<EnvironmentRow> findById(String id) {
        return jdbc.query("SELECT id, code FROM environments WHERE id = ?", MAPPER, id)
                .stream().findFirst();
    }

    public List<EnvironmentRow> findAll() {
        return jdbc.query("SELECT id, code FROM environments ORDER BY code", MAPPER);
    }

    public EnvironmentRow upsertByCode(String code) {
        Optional<EnvironmentRow> existing = findByCode(code);
        if (existing.isPresent()) {
            return existing.get();
        }
        String id = TSID.Factory.getTsid().toString();
        try {
            jdbc.update("INSERT INTO environments (id, code) VALUES (?, ?)", id, code);
        } catch (DuplicateKeyException race) {
            return findByCode(code).orElseThrow();
        }
        return new EnvironmentRow(id, code);
    }

    private static final RowMapper<EnvironmentRow> MAPPER =
            (rs, i) -> new EnvironmentRow(rs.getString("id"), rs.getString("code"));
}
