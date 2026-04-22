package ai.apiprism.center.catalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class TagOrderRepositoryTest {

    @Autowired
    private TagOrderRepository tagOrderRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanUp() {
        jdbc.update("DELETE FROM tag_order");
    }

    @Test
    void findOrder_returnsEmptyMapWhenNoRowsExist() {
        Map<String, Integer> order = tagOrderRepository.findOrder("svc-x", "dev");
        assertTrue(order.isEmpty());
    }

    @Test
    void saveOrder_persistsAndFindOrderReturnsCorrectPositions() {
        tagOrderRepository.saveOrder("svc-a", "dev", List.of("beta", "alpha", "gamma"));

        Map<String, Integer> order = tagOrderRepository.findOrder("svc-a", "dev");
        assertEquals(3, order.size());
        assertEquals(0, order.get("beta"));
        assertEquals(1, order.get("alpha"));
        assertEquals(2, order.get("gamma"));
    }

    @Test
    void saveOrder_replacesExistingOrder() {
        tagOrderRepository.saveOrder("svc-b", "prod", List.of("x", "y", "z"));
        tagOrderRepository.saveOrder("svc-b", "prod", List.of("z", "x"));

        Map<String, Integer> order = tagOrderRepository.findOrder("svc-b", "prod");
        assertEquals(2, order.size());
        assertEquals(0, order.get("z"));
        assertEquals(1, order.get("x"));
        assertNull(order.get("y"));
    }

    @Test
    void saveOrder_emptyList_clearsAllRows() {
        tagOrderRepository.saveOrder("svc-c", "dev", List.of("a", "b"));
        tagOrderRepository.saveOrder("svc-c", "dev", List.of());

        Map<String, Integer> order = tagOrderRepository.findOrder("svc-c", "dev");
        assertTrue(order.isEmpty());
    }

    @Test
    void deleteByRef_removesAllRows() {
        tagOrderRepository.saveOrder("svc-d", "dev", List.of("a", "b", "c"));
        tagOrderRepository.deleteByRef("svc-d", "dev");

        Map<String, Integer> order = tagOrderRepository.findOrder("svc-d", "dev");
        assertTrue(order.isEmpty());
    }

    @Test
    void deleteByRef_doesNotAffectOtherEnvironments() {
        tagOrderRepository.saveOrder("svc-e", "dev", List.of("a", "b"));
        tagOrderRepository.saveOrder("svc-e", "prod", List.of("c", "d"));

        tagOrderRepository.deleteByRef("svc-e", "dev");

        assertTrue(tagOrderRepository.findOrder("svc-e", "dev").isEmpty());
        assertEquals(2, tagOrderRepository.findOrder("svc-e", "prod").size());
    }
}
