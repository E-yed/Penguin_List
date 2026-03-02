package com.penguinlist.api.dto;

import com.penguinlist.api.model.TodoDay;
import com.penguinlist.api.model.TodoItem;

import java.util.List;

public record TodoDayResponse(TodoDay day, List<TodoItem> done, List<TodoItem> pending) {
}
