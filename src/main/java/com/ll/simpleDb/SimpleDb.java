package com.ll.simpleDb;

import lombok.Setter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC 를 감싸 SQL 실행을 쉽게 해 주는 유틸리티.
 * 커넥션은 스레드마다 따로 열어서(ThreadLocal) 여러 스레드가 동시에 써도 서로 섞이지 않는다.
 */
public class SimpleDb {
    private static final int PORT = 3306;

    private final String url;
    private final String username;
    private final String password;
    private final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    @Setter
    private boolean devMode;

    public SimpleDb(String host, String username, String password, String dbName) {
        this.url = "jdbc:mysql://%s:%d/%s".formatted(host, PORT, dbName);
        this.username = username;
        this.password = password;
    }

    public void run(String sql, Object... params) {
        genSql().append(sql, params).update();
    }

    public Sql genSql() {
        return new Sql(this);
    }

    public void startTransaction() {
        try {
            getConnection().setAutoCommit(false);
        } catch (SQLException e) {
            throw new SimpleDbException("트랜잭션 시작 실패", e);
        }
    }

    public void commit() {
        Connection connection = getConnection();
        try {
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new SimpleDbException("커밋 실패", e);
        }
    }

    public void rollback() {
        Connection connection = getConnection();
        try {
            connection.rollback();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new SimpleDbException("롤백 실패", e);
        }
    }

    /** 현재 스레드의 커넥션을 닫는다. 다른 스레드의 커넥션에는 영향이 없다. */
    public void close() {
        Connection connection = connectionHolder.get();
        connectionHolder.remove();

        if (connection == null) return;

        try {
            connection.close();
        } catch (SQLException e) {
            throw new SimpleDbException("커넥션 종료 실패", e);
        }
    }

    long insert(String sql, List<Object> params) {
        logSql(sql, params);

        try (PreparedStatement statement = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindParams(statement, params);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new SimpleDbException("SQL 실행 실패: " + sql, e);
        }
    }

    int update(String sql, List<Object> params) {
        logSql(sql, params);

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            bindParams(statement, params);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new SimpleDbException("SQL 실행 실패: " + sql, e);
        }
    }

    List<Map<String, Object>> selectRows(String sql, List<Object> params) {
        logSql(sql, params);

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            bindParams(statement, params);

            try (ResultSet resultSet = statement.executeQuery()) {
                return toRows(resultSet);
            }
        } catch (SQLException e) {
            throw new SimpleDbException("SQL 실행 실패: " + sql, e);
        }
    }

    private Connection getConnection() {
        try {
            Connection connection = connectionHolder.get();

            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(url, username, password);
                connectionHolder.set(connection);
            }

            return connection;
        } catch (SQLException e) {
            throw new SimpleDbException("DB 연결 실패: " + url, e);
        }
    }

    private void bindParams(PreparedStatement statement, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            statement.setObject(i + 1, params.get(i));
        }
    }

    private List<Map<String, Object>> toRows(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        List<Map<String, Object>> rows = new ArrayList<>();

        while (resultSet.next()) {
            Map<String, Object> row = new LinkedHashMap<>();

            for (int i = 1; i <= columnCount; i++) {
                row.put(metaData.getColumnLabel(i), normalize(resultSet.getObject(i)));
            }

            rows.add(row);
        }

        return rows;
    }

    /** 드라이버가 TIMESTAMP 를 java.sql.Timestamp 로 돌려주는 경우에도 LocalDateTime 으로 맞춘다. */
    private Object normalize(Object value) {
        if (value instanceof Timestamp timestamp) return timestamp.toLocalDateTime();

        return value;
    }

    private void logSql(String sql, List<Object> params) {
        if (!devMode) return;

        System.out.println("== rawSql ==");
        System.out.println(toRawSql(sql, params));
        System.out.println();
    }

    /** 개발 모드 출력용: ? 자리에 실제 값을 채운 SQL. */
    private String toRawSql(String sql, List<Object> params) {
        StringBuilder rawSql = new StringBuilder();
        int paramIndex = 0;

        for (char ch : sql.toCharArray()) {
            if (ch == '?' && paramIndex < params.size()) {
                rawSql.append(toSqlLiteral(params.get(paramIndex++)));
            } else {
                rawSql.append(ch);
            }
        }

        return rawSql.toString().strip();
    }

    private String toSqlLiteral(Object value) {
        if (value == null) return "NULL";
        if (value instanceof Number || value instanceof Boolean) return value.toString();

        return "'" + value.toString().replace("'", "''") + "'";
    }
}
