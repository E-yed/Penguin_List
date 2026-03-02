package com.penguinlist.api;

import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class TodoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsDoneAndPendingTodosForToday() throws Exception {
        mockMvc.perform(get("/api/todos/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.day").value("TODAY"))
                .andExpect(jsonPath("$.done").isArray())
                .andExpect(jsonPath("$.pending").isArray());
    }

    @Test
    void updatesTodoStatus() throws Exception {
        mockMvc.perform(patch("/api/todos/today/4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "done": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.done").value(true));
    }

    @Test
    void rejectsInvalidDayValues() throws Exception {
        mockMvc.perform(get("/api/todos/friday"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("day must be one of: yesterday, today, tomorrow"));
    }

    @Test
    void updatesDescriptionStatusAndSyncsParentTodo() throws Exception {
        mockMvc.perform(patch("/api/todos/today/4/descriptions/0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "done": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.descriptions[0].done").value(true))
                .andExpect(jsonPath("$.done").value(false));
    }

    @Test
    void servesApiRootAtSlashApi() throws Exception {
        mockMvc.perform(get("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.TODAY.day").value("TODAY"));
    }

    @Test
    void servesFrontendAtRoot() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl("/index.html"));
    }
}
