package com.penguinlist.api.controller;

import com.penguinlist.api.dto.TodoDayResponse;
import com.penguinlist.api.model.TodoDay;
import com.penguinlist.api.service.TodoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ApiRootController {

    private final TodoService todoService;

    public ApiRootController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping("/api")
    public Map<TodoDay, TodoDayResponse> getApiRoot() {
        return todoService.getAllTodos();
    }
}
