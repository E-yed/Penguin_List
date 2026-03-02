package com.penguinlist.api.model;

import java.util.List;

public record TodoItem(Long id, String title, List<TodoDescription> descriptions, boolean done) {
}
