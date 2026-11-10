package com.ll.simpleDb;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * SQL 을 조각조각 이어 붙이고(append) 실행하는 빌더.
 * 실제 실행은 이 Sql 을 만든 SimpleDb 의 (현재 스레드) 커넥션으로 한다.
 */
public class Sql {
    /** 행(Map) → 객체 매핑용. 컬럼명과 필드명을 그대로 맞추려고 getter/setter 대신 필드로 매핑한다. */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    private final SimpleDb simpleDb;
    private final StringBuilder sqlBuilder = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String sqlPart, Object... params) {
        if (!sqlBuilder.isEmpty()) sqlBuilder.append('\n');

        sqlBuilder.append(sqlPart);
        this.params.addAll(Arrays.asList(params));

        return this;
    }

    /** sqlPart 안의 ? 하나를 값 개수만큼의 ?, ?, ... 로 펼친다. 예) IN (?) + 1, 2, 3 → IN (?, ?, ?) */
    public Sql appendIn(String sqlPart, Object... params) {
        String placeholders = String.join(", ", Collections.nCopies(params.length, "?"));

        return append(sqlPart.replaceFirst("\\?", placeholders), params);
    }

    public long insert() {
        return simpleDb.insert(sql(), params);
    }

    public int update() {
        return simpleDb.update(sql(), params);
    }

    public int delete() {
        return simpleDb.update(sql(), params);
    }

    public List<Map<String, Object>> selectRows() {
        return simpleDb.selectRows(sql(), params);
    }

    public <T> List<T> selectRows(Class<T> cls) {
        return selectRows().stream()
                .map(row -> OBJECT_MAPPER.convertValue(row, cls))
                .toList();
    }

    public Map<String, Object> selectRow() {
        List<Map<String, Object>> rows = selectRows();

        return rows.isEmpty() ? null : rows.getFirst();
    }

    public <T> T selectRow(Class<T> cls) {
        Map<String, Object> row = selectRow();

        return row == null ? null : OBJECT_MAPPER.convertValue(row, cls);
    }

    public LocalDateTime selectDatetime() {
        return (LocalDateTime) selectValue();
    }

    public Long selectLong() {
        return toLong(selectValue());
    }

    public List<Long> selectLongs() {
        return selectRows().stream()
                .map(row -> toLong(firstValue(row)))
                .toList();
    }

    public String selectString() {
        Object value = selectValue();

        return value == null ? null : value.toString();
    }

    public Boolean selectBoolean() {
        Object value = selectValue();

        if (value == null) return null;
        if (value instanceof Boolean bool) return bool;
        if (value instanceof Number number) return number.longValue() != 0;

        String str = value.toString();
        return str.equals("1") || str.equalsIgnoreCase("true");
    }

    /** 첫 번째 행의 첫 번째 컬럼 값. 결과가 없으면 null. */
    private Object selectValue() {
        Map<String, Object> row = selectRow();

        return row == null ? null : firstValue(row);
    }

    private Object firstValue(Map<String, Object> row) {
        return row.values().iterator().next();
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();

        return Long.parseLong(value.toString());
    }

    private String sql() {
        return sqlBuilder.toString();
    }
}
