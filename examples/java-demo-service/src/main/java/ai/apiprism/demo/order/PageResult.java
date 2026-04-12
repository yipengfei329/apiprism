package ai.apiprism.demo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 通用分页结果包装。 */
@Data
@Builder
@Schema(description = "分页结果")
public class PageResult<T> {

    @Schema(description = "当前页数据列表")
    private List<T> items;

    @Schema(description = "当前页码", example = "1")
    private int page;

    @Schema(description = "每页条数", example = "20")
    private int size;

    @Schema(description = "总记录数", example = "128")
    private long total;

    @Schema(description = "总页数", example = "7")
    private int totalPages;
}
