package com.penguinlist.api.repository;

import com.penguinlist.api.model.TodoDay;
import com.penguinlist.api.model.TodoDescription;
import com.penguinlist.api.model.TodoItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class TodoRepository {

    private final JdbcTemplate jdbcTemplate;

    public TodoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isEmpty() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM todos", Integer.class);
        return count == null || count == 0;
    }

    public List<TodoItem> findByDay(TodoDay day) {
        List<TodoRow> todoRows = jdbcTemplate.query(
                "SELECT id, title, done FROM todos WHERE day = ? ORDER BY id",
                (rs, rowNum) -> new TodoRow(rs.getLong("id"), rs.getString("title"), rs.getBoolean("done")),
                day.name()
        );

        if (todoRows.isEmpty()) {
            return List.of();
        }

        Map<Long, List<TodoDescription>> descriptionsByTodoId = new LinkedHashMap<>();
        jdbcTemplate.query(
                "SELECT todo_id, position, text, done FROM todo_descriptions WHERE todo_id IN (" +
                        todoRows.stream().map(row -> "?").reduce((left, right) -> left + "," + right).orElse("?") +
                        ") ORDER BY todo_id, position",
                ps -> {
                    for (int i = 0; i < todoRows.size(); i++) {
                        ps.setLong(i + 1, todoRows.get(i).id());
                    }
                },
                (RowCallbackHandler) rs -> descriptionsByTodoId
                        .computeIfAbsent(rs.getLong("todo_id"), ignored -> new ArrayList<>())
                        .add(new TodoDescription(rs.getString("text"), rs.getBoolean("done")))
        );

        return todoRows.stream()
                .map(row -> new TodoItem(
                        row.id(),
                        row.title(),
                        List.copyOf(descriptionsByTodoId.getOrDefault(row.id(), List.of())),
                        row.done()
                ))
                .toList();
    }

    public Optional<TodoItem> findByDayAndId(TodoDay day, long todoId) {
        List<TodoItem> items = findByDay(day).stream()
                .filter(item -> item.id() == todoId)
                .toList();
        return items.stream().findFirst();
    }

    public TodoItem create(TodoDay day, String title, List<String> descriptions) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO todos(day, title, done) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, day.name());
            statement.setString(2, title);
            statement.setBoolean(3, false);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create todo");
        }

        long todoId = key.longValue();
        for (int i = 0; i < descriptions.size(); i++) {
            jdbcTemplate.update(
                    "INSERT INTO todo_descriptions(todo_id, position, text, done) VALUES (?, ?, ?, ?)",
                    todoId, i, descriptions.get(i), false
            );
        }

        return findRequired(day, todoId);
    }

    public TodoItem updateTodoStatus(TodoDay day, long todoId, boolean done) {
        jdbcTemplate.update("UPDATE todos SET done = ? WHERE id = ? AND day = ?", done, todoId, day.name());
        jdbcTemplate.update("UPDATE todo_descriptions SET done = ? WHERE todo_id = ?", done, todoId);
        return findRequired(day, todoId);
    }

    public TodoItem updateDescriptionStatus(TodoDay day, long todoId, int descriptionIndex, boolean done) {
        int updated = jdbcTemplate.update(
                """
                UPDATE todo_descriptions
                SET done = ?
                WHERE todo_id = ?
                  AND position = ?
                  AND EXISTS (SELECT 1 FROM todos WHERE id = ? AND day = ?)
                """,
                done, todoId, descriptionIndex, todoId, day.name()
        );

        if (updated == 0) {
            return null;
        }

        Boolean allDone = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MIN(done), 0) FROM todo_descriptions WHERE todo_id = ?",
                Boolean.class,
                todoId
        );
        jdbcTemplate.update("UPDATE todos SET done = ? WHERE id = ? AND day = ?", Boolean.TRUE.equals(allDone), todoId, day.name());
        return findRequired(day, todoId);
    }

    public boolean delete(TodoDay day, long todoId) {
        int deleted = jdbcTemplate.update("DELETE FROM todos WHERE id = ? AND day = ?", todoId, day.name());
        return deleted > 0;
    }

    public void seed(TodoDay day, String title, boolean done, String... descriptions) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO todos(day, title, done) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, day.name());
            statement.setString(2, title);
            statement.setBoolean(3, done);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to seed todo");
        }

        for (int i = 0; i < descriptions.length; i++) {
            jdbcTemplate.update(
                    "INSERT INTO todo_descriptions(todo_id, position, text, done) VALUES (?, ?, ?, ?)",
                    key.longValue(), i, descriptions[i], done
            );
        }
    }

    private TodoItem findRequired(TodoDay day, long todoId) {
        return findByDayAndId(day, todoId)
                .orElseThrow(() -> new IllegalStateException("Todo %d was not found after persistence".formatted(todoId)));
    }

    private record TodoRow(long id, String title, boolean done) {
    }
}
