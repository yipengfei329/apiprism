package ai.apiprism.center.localization;

import ai.apiprism.center.localization.LocalizationResolver.ResolverKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JdbcTest
@Import({LocalizationRepository.class, LocalizationResolver.class})
class LocalizationResolverTest {

    @Autowired LocalizationRepository repository;
    @Autowired LocalizationResolver resolver;

    private static final LocalizationEntity OP = LocalizationEntity.OPERATION;
    private static final String ID = "op-1";
    private static final String FIELD = LocalizationField.DESCRIPTION;

    @BeforeEach
    void cleanup() {
        repository.deleteByEntity(OP, ID);
    }

    @Test
    void resolvesAdapterAtDefaultLocale_whenUserLocaleMissing() {
        repository.upsert(OP, ID, FIELD, "zh-CN", LocalizationSource.ADAPTER, "默认中文");

        Optional<String> hit = resolver.resolve(OP, ID, FIELD, "en-US", "zh-CN");
        assertEquals(Optional.of("默认中文"), hit);
    }

    @Test
    void userLocaleCenterWins_overAllOthers() {
        repository.upsert(OP, ID, FIELD, "zh-CN", LocalizationSource.ADAPTER, "默认中文");
        repository.upsert(OP, ID, FIELD, "zh-CN", LocalizationSource.CENTER, "中心改写中文");
        repository.upsert(OP, ID, FIELD, "en-US", LocalizationSource.ADAPTER, "adapter english");
        repository.upsert(OP, ID, FIELD, "en-US", LocalizationSource.CENTER, "center english");

        assertEquals(Optional.of("center english"),
                resolver.resolve(OP, ID, FIELD, "en-US", "zh-CN"));
        assertEquals(Optional.of("中心改写中文"),
                resolver.resolve(OP, ID, FIELD, "zh-CN", "zh-CN"));
    }

    @Test
    void userLocaleAdapter_beatsDefaultLocaleCenter() {
        repository.upsert(OP, ID, FIELD, "en-US", LocalizationSource.ADAPTER, "adapter english");
        repository.upsert(OP, ID, FIELD, "zh-CN", LocalizationSource.CENTER, "中心改写中文");

        assertEquals(Optional.of("adapter english"),
                resolver.resolve(OP, ID, FIELD, "en-US", "zh-CN"),
                "用户 locale 的 adapter 行应高于默认 locale 的 center 行");
    }

    @Test
    void fallsBackToDefaultLocaleCenter_whenUserLocaleHasNothing() {
        repository.upsert(OP, ID, FIELD, "zh-CN", LocalizationSource.CENTER, "中心改写中文");
        repository.upsert(OP, ID, FIELD, "zh-CN", LocalizationSource.ADAPTER, "默认中文");

        assertEquals(Optional.of("中心改写中文"),
                resolver.resolve(OP, ID, FIELD, "en-US", "zh-CN"));
    }

    @Test
    void returnsEmpty_whenNothingMatches() {
        assertTrue(resolver.resolve(OP, ID, FIELD, "en-US", "zh-CN").isEmpty());
    }

    @Test
    void upsertUpdatesExistingRow() {
        repository.upsert(OP, ID, FIELD, "zh-CN", LocalizationSource.ADAPTER, "版本 A");
        repository.upsert(OP, ID, FIELD, "zh-CN", LocalizationSource.ADAPTER, "版本 B");

        assertEquals(Optional.of("版本 B"),
                resolver.resolve(OP, ID, FIELD, "zh-CN", "zh-CN"));
        assertEquals(1, repository.findByEntity(OP, ID).size(),
                "相同 (entity, field, locale, source) 不应插入新行");
    }

    @Test
    void upsertWithNullContent_removesRow() {
        repository.upsert(OP, ID, FIELD, "zh-CN", LocalizationSource.ADAPTER, "会被删除");
        repository.upsert(OP, ID, FIELD, "zh-CN", LocalizationSource.ADAPTER, null);

        assertTrue(resolver.resolve(OP, ID, FIELD, "zh-CN", "zh-CN").isEmpty());
        assertTrue(repository.findByEntity(OP, ID).isEmpty());
    }

    @Test
    void resolveBatch_aggregatesAcrossEntitiesAndFields() {
        repository.upsert(OP, "op-a", LocalizationField.DESCRIPTION, "zh-CN",
                LocalizationSource.ADAPTER, "A 描述");
        repository.upsert(OP, "op-a", LocalizationField.SUMMARY, "zh-CN",
                LocalizationSource.CENTER, "A 摘要（中心改写）");
        repository.upsert(OP, "op-b", LocalizationField.DESCRIPTION, "en-US",
                LocalizationSource.CENTER, "B desc");

        List<ResolverKey> keys = List.of(
                new ResolverKey(OP, "op-a", LocalizationField.DESCRIPTION),
                new ResolverKey(OP, "op-a", LocalizationField.SUMMARY),
                new ResolverKey(OP, "op-b", LocalizationField.DESCRIPTION),
                new ResolverKey(OP, "op-missing", LocalizationField.DESCRIPTION));

        Map<ResolverKey, String> result = resolver.resolveBatch(keys, "en-US", "zh-CN");

        assertEquals("A 描述", result.get(keys.get(0)));
        assertEquals("A 摘要（中心改写）", result.get(keys.get(1)));
        assertEquals("B desc", result.get(keys.get(2)));
        assertFalse(result.containsKey(keys.get(3)));
    }

    @Test
    void batchFallsBackToDefaultLocale_whenUserLocaleIsSameAsDefault() {
        repository.upsert(OP, "op-c", LocalizationField.DESCRIPTION, "zh-CN",
                LocalizationSource.ADAPTER, "中文描述");

        Map<ResolverKey, String> result = resolver.resolveBatch(
                List.of(new ResolverKey(OP, "op-c", LocalizationField.DESCRIPTION)),
                "zh-CN", "zh-CN");

        assertEquals("中文描述", result.values().iterator().next());
    }
}
