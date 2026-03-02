package com.penguinlist.api.controller;

import com.penguinlist.api.dto.CreateTodoRequest;
import com.penguinlist.api.dto.TodoDayResponse;
import com.penguinlist.api.dto.UpdateDescriptionStatusRequest;
import com.penguinlist.api.dto.UpdateTodoStatusRequest;
import com.penguinlist.api.model.TodoDay;
import com.penguinlist.api.model.TodoItem;
import com.penguinlist.api.service.TodoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping
    public Map<TodoDay, TodoDayResponse> getAllTodos() {
        return todoService.getAllTodos();
    }

    @GetMapping("/{day}")
    public TodoDayResponse getTodosByDay(@PathVariable String day) {
        return todoService.getTodosForDay(TodoDay.from(day));
    }

    @PostMapping("/{day}")
    @ResponseStatus(HttpStatus.CREATED)
    public TodoItem createTodo(@PathVariable String day, @Valid @RequestBody CreateTodoRequest request) {
        return todoService.createTodo(TodoDay.from(day), request);
    }

    @PatchMapping("/{day}/{todoId}")
    public TodoItem updateTodoStatus(
            @PathVariable String day,
            @PathVariable long todoId,
            @Valid @RequestBody UpdateTodoStatusRequest request
    ) {
        return todoService.updateTodoStatus(TodoDay.from(day), todoId, request.done());
    }

    @PatchMapping("/{day}/{todoId}/descriptions/{descriptionIndex}")
    public TodoItem updateDescriptionStatus(
            @PathVariable String day,
            @PathVariable long todoId,
            @PathVariable int descriptionIndex,
            @Valid @RequestBody UpdateDescriptionStatusRequest request
    ) {
        return todoService.updateDescriptionStatus(TodoDay.from(day), todoId, descriptionIndex, request.done());
    }

    @DeleteMapping("/{day}/{todoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTodo(@PathVariable String day, @PathVariable long todoId) {
        todoService.deleteTodo(TodoDay.from(day), todoId);
    }
}
