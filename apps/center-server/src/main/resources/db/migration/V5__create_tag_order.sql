-- 用户自定义分组排序表。
-- position: 0-based 整数，越小越靠前。
-- 只存用户主动拖拽后的状态；首次访问时表为空，CatalogService 使用 spec 原始顺序。
CREATE TABLE tag_order (
    service_name  VARCHAR(255)  NOT NULL,
    environment   VARCHAR(100)  NOT NULL,
    group_slug    VARCHAR(255)  NOT NULL,
    position      INT           NOT NULL,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_tag_order PRIMARY KEY (service_name, environment, group_slug)
);
