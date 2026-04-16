package ai.apiprism.demo.boot4;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Tag(name = "Books", description = "图书管理接口")
@RestController
@RequestMapping("/api/books")
public class BookController {

    private final Map<Long, Book> store = new ConcurrentHashMap<>();
    private final AtomicLong idSeq = new AtomicLong();

    @Operation(summary = "获取所有图书")
    @GetMapping
    public List<Book> list() {
        return List.copyOf(store.values());
    }

    @Operation(summary = "创建图书")
    @PostMapping
    public Book create(@RequestBody BookCreateRequest request) {
        long id = idSeq.incrementAndGet();
        Book book = new Book(id, request.title(), request.author(), request.isbn());
        store.put(id, book);
        return book;
    }

    @Operation(summary = "按 ID 查询图书")
    @GetMapping("/{id}")
    public Book get(@PathVariable Long id) {
        Book book = store.get(id);
        if (book == null) {
            throw new IllegalArgumentException("Book not found: " + id);
        }
        return book;
    }

    @Operation(summary = "按 ID 删除图书")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        store.remove(id);
    }

    public record Book(Long id, String title, String author, String isbn) {}

    public record BookCreateRequest(String title, String author, String isbn) {}
}
