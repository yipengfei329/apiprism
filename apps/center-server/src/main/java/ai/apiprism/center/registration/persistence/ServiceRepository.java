package ai.apiprism.center.registration.persistence;

import ai.apiprism.center.registration.persistence.PersistenceRows.ServiceRow;
import io.hypersistence.tsid.TSID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * services 表访问层。服务按 name 全局唯一；default_locale 首次注册时写入，后续不被覆盖。
 */
@Repository
public class ServiceRepository {

    private final JdbcTemplate jdbc;

    public ServiceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ServiceRow> findByName(String name) {
        return jdbc.query("SELECT id, name, default_locale, created_at, updated_at FROM services WHERE name = ?",
                MAPPER, name).stream().findFirst();
    }

    public Optional<ServiceRow> findById(String id) {
        return jdbc.query("SELECT id, name, default_locale, created_at, updated_at FROM services WHERE id = ?",
                MAPPER, id).stream().findFirst();
    }

    public List<ServiceRow> findAll() {
        return jdbc.query("SELECT id, name, default_locale, created_at, updated_at FROM services ORDER BY name",
                MAPPER);
    }

    /**
     * 按 name upsert。已存在时仅刷新 updated_at；default_locale 仅在 INSERT 时生效，
     * 避免适配器覆盖中心已敲定的默认语言。
     */
    public ServiceRow upsertByName(String name, String defaultLocale) {
        Optional<ServiceRow> existing = findByName(name);
        if (existing.isPresent()) {
            Instant now = Instant.now();
            jdbc.update("UPDATE services SET updated_at = ? WHERE id = ?",
                    Timestamp.from(now), existing.get().id());
            return new ServiceRow(existing.get().id(), name, existing.get().defaultLocale(),
                    existing.get().createdAt(), now);
        }
        String id = TSID.Factory.getTsid().toString();
        Instant now = Instant.now();
        try {
            jdbc.update("INSERT INTO services (id, name, default_locale, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
                    id, name, defaultLocale, Timestamp.from(now), Timestamp.from(now));
        } catch (DuplicateKeyException race) {
            return findByName(name).orElseThrow();
        }
        return new ServiceRow(id, name, defaultLocale, now, now);
    }

    private static final RowMapper<ServiceRow> MAPPER = (rs, i) -> new ServiceRow(
            rs.getString("id"),
            rs.getString("name"),
            rs.getString("default_locale"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant());
}
