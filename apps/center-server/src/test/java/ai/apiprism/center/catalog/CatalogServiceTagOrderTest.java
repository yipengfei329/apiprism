package ai.apiprism.center.catalog;

import ai.apiprism.center.repository.RegistrationRepository;
import ai.apiprism.center.repository.StoredRegistration;
import ai.apiprism.model.CanonicalGroup;
import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalServiceSnapshot;
import ai.apiprism.model.ServiceRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTagOrderTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private TagOrderRepository tagOrderRepository;

    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(registrationRepository, tagOrderRepository);
    }

    @Test
    void getService_returnsOriginalOrderWhenNoOrderSaved() {
        CanonicalGroup groupA = group("a", "a-slug");
        CanonicalGroup groupB = group("b", "b-slug");
        stubService("svc", "dev", List.of(groupA, groupB));
        when(tagOrderRepository.findOrder("svc", "dev")).thenReturn(Map.of());

        List<CanonicalGroup> groups = catalogService.getService("svc", "dev").getGroups();
        assertEquals(List.of("a-slug", "b-slug"), groups.stream().map(CanonicalGroup::getSlug).toList());
    }

    @Test
    void getService_appliesSavedOrder() {
        CanonicalGroup groupA = group("a", "a-slug");
        CanonicalGroup groupB = group("b", "b-slug");
        stubService("svc", "dev", List.of(groupA, groupB));
        when(tagOrderRepository.findOrder("svc", "dev")).thenReturn(Map.of("b-slug", 0, "a-slug", 1));

        List<CanonicalGroup> groups = catalogService.getService("svc", "dev").getGroups();
        assertEquals(List.of("b-slug", "a-slug"), groups.stream().map(CanonicalGroup::getSlug).toList());
    }

    @Test
    void getService_appendsNewGroupsAtEnd() {
        // c-slug is new and not in saved order → appended at end
        CanonicalGroup groupA = group("a", "a-slug");
        CanonicalGroup groupB = group("b", "b-slug");
        CanonicalGroup groupC = group("c", "c-slug");
        stubService("svc", "dev", List.of(groupA, groupB, groupC));
        when(tagOrderRepository.findOrder("svc", "dev")).thenReturn(Map.of("b-slug", 0, "a-slug", 1));

        List<CanonicalGroup> groups = catalogService.getService("svc", "dev").getGroups();
        assertEquals(List.of("b-slug", "a-slug", "c-slug"), groups.stream().map(CanonicalGroup::getSlug).toList());
    }

    @Test
    void getService_ignoresStaleSlugsInSavedOrder() {
        // x-slug was removed from spec; saved order has stale entry
        CanonicalGroup groupA = group("a", "a-slug");
        CanonicalGroup groupB = group("b", "b-slug");
        stubService("svc", "dev", List.of(groupA, groupB));
        when(tagOrderRepository.findOrder("svc", "dev")).thenReturn(Map.of("x-slug", 0, "a-slug", 1, "b-slug", 2));

        List<CanonicalGroup> groups = catalogService.getService("svc", "dev").getGroups();
        assertEquals(List.of("a-slug", "b-slug"), groups.stream().map(CanonicalGroup::getSlug).toList());
    }

    @Test
    void listServices_includesOperationCountPerGroup() {
        CanonicalGroup groupA = group("a", "a-slug", 2);
        CanonicalGroup groupB = group("b", "b-slug", 1);
        StoredRegistration registration = registration("svc", "dev", List.of(groupA, groupB));
        when(registrationRepository.findAll()).thenReturn(List.of(registration));
        when(tagOrderRepository.findOrder("svc", "dev")).thenReturn(Map.of());

        ServiceCatalogItem item = catalogService.listServices().getFirst();

        assertEquals(3, item.getOperationCount());
        assertEquals(List.of(2, 1), item.getGroups().stream()
                .map(ServiceCatalogItem.GroupRef::getOperationCount)
                .toList());
    }

    // ── helpers ──

    private CanonicalGroup group(String name, String slug) {
        return CanonicalGroup.builder().name(name).slug(slug).build();
    }

    private CanonicalGroup group(String name, String slug, int operationCount) {
        CanonicalGroup.CanonicalGroupBuilder builder = CanonicalGroup.builder().name(name).slug(slug);
        for (int i = 0; i < operationCount; i++) {
            builder.operation(CanonicalOperation.builder()
                    .operationId(name + "-" + i)
                    .method("GET")
                    .path("/" + name + "/" + i)
                    .build());
        }
        return builder.build();
    }

    private void stubService(String serviceName, String environment, List<CanonicalGroup> groups) {
        StoredRegistration reg = registration(serviceName, environment, groups);
        when(registrationRepository.findByRef(serviceName, environment)).thenReturn(Optional.of(reg));
    }

    private StoredRegistration registration(String serviceName, String environment, List<CanonicalGroup> groups) {
        CanonicalServiceSnapshot snapshot = CanonicalServiceSnapshot.builder()
                .ref(ServiceRef.builder().name(serviceName).environment(environment).build())
                .title("Test")
                .version("1.0")
                .updatedAt(Instant.now())
                .groups(groups)
                .build();
        return StoredRegistration.builder()
                .id("test-id")
                .snapshot(snapshot)
                .specHash("hash")
                .specFormat("openapi-json")
                .adapterType("test")
                .rawSpec("{}")
                .build();
    }
}
