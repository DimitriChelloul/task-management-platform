package org.dimitri.task.controller;

import org.dimitri.task.domain.Task;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    @GetMapping
    public ResponseEntity<List<Task>> list() {
        // minimal stub: return an empty list or example
        Task sample = new Task(UUID.randomUUID(), "Sample task");
        return ResponseEntity.ok(List.of(sample));
    }
}
