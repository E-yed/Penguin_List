package com.penguinlist.api.service;

import com.penguinlist.api.dto.CreateTodoRequest;
import com.penguinlist.api.dto.TodoDayResponse;
import com.penguinlist.api.model.TodoDay;
import com.penguinlist.api.model.TodoItem;
import com.penguinlist.api.repository.TodoRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class TodoService {

    private final TodoRepository todoRepository;

    public TodoService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
        seedDataIfEmpty();
    }

    public Map<TodoDay, TodoDayResponse> getAllTodos() {
        Map<TodoDay, TodoDayResponse> response = new EnumMap<>(TodoDay.class);
        List.of(TodoDay.values()).stream()
                .sorted(Comparator.comparing(Enum::ordinal))
                .forEach(day -> response.put(day, getTodosForDay(day)));
        return response;
    }

    public TodoDayResponse getTodosForDay(TodoDay day) {
        List<TodoItem> items = todoRepository.findByDay(day);
        List<TodoItem> done = items.stream().filter(TodoItem::done).toList();
        List<TodoItem> pending = items.stream().filter(item -> !item.done()).toList();
        return new TodoDayResponse(day, done, pending);
    }

    public TodoItem createTodo(TodoDay day, CreateTodoRequest request) {
        List<String> descriptions = request.descriptions() == null
                ? List.of()
                : request.descriptions().stream()
                .filter(text -> text != null && !text.isBlank())
                .map(String::trim)
                .toList();
        return todoRepository.create(day, request.title().trim(), descriptions);
    }

    public TodoItem updateTodoStatus(TodoDay day, long todoId, boolean done) {
        ensureExists(day, todoId);
        return todoRepository.updateTodoStatus(day, todoId, done);
    }

    public TodoItem updateDescriptionStatus(TodoDay day, long todoId, int descriptionIndex, boolean done) {
        TodoItem existing = todoRepository.findByDayAndId(day, todoId)
                .orElseThrow(() -> notFound(day, todoId));
        if (descriptionIndex < 0 || descriptionIndex >= existing.descriptions().size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Description %d was not found for todo %d".formatted(descriptionIndex, todoId));
        }
        TodoItem updated = todoRepository.updateDescriptionStatus(day, todoId, descriptionIndex, done);
        if (updated == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Description %d was not found for todo %d".formatted(descriptionIndex, todoId));
        }
        return updated;
    }

    public void deleteTodo(TodoDay day, long todoId) {
        boolean removed = todoRepository.delete(day, todoId);
        if (!removed) {
            throw notFound(day, todoId);
        }
    }

    private void ensureExists(TodoDay day, long todoId) {
        todoRepository.findByDayAndId(day, todoId)
                .orElseThrow(() -> notFound(day, todoId));
    }

    private void seedDataIfEmpty() {
        try {
            if (!todoRepository.isEmpty()) {
                return;
            }
            todoRepository.seed(TodoDay.YESTERDAY, "Finish history notes", true,
                    "Rewrite the messy chapter summary", "Highlight exam dates");
            todoRepository.seed(TodoDay.YESTERDAY, "Wash clothes before the chair becomes a wardrobe", false,
                    "Dark pile", "White pile", "Find the missing sock");

            todoRepository.seed(TodoDay.TODAY, "Submit math homework", true,
                    "Check question 4 again", "Upload the PDF");
            todoRepository.seed(TodoDay.TODAY, "Clean the desk before it starts a new ecosystem", false,
                    "Throw away snack wrappers", "Stack notebooks", "Rescue the charger cable");

            todoRepository.seed(TodoDay.TOMORROW, "Pack for class", false,
                    "Notebook", "Pens", "Calculator", "Water bottle");
            todoRepository.seed(TodoDay.TOMORROW, "Study biology without falling into a nap", false,
                    "Cell division", "Flashcards", "Past paper questions");
        } catch (DataAccessException exception) {
            throw new IllegalStateException("Failed to initialize SQLite data", exception);
        }
    }

    private ResponseStatusException notFound(TodoDay day, long todoId) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Todo %d was not found for %s".formatted(todoId, day.name().toLowerCase()));
    }
}
