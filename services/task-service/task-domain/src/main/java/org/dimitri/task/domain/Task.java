package org.dimitri.task.domain;

import java.util.UUID;

public final class Task {
	private final UUID id;
	private final String title;

	public Task(UUID id, String title) {
		this.id = id;
		this.title = title;
	}

	public UUID id() {
		return id;
	}

	public String title() {
		return title;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Task)) return false;
		Task task = (Task) o;
		return id.equals(task.id) && title.equals(task.title);
	}

	@Override
	public int hashCode() {
		int result = id.hashCode();
		result = 31 * result + title.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "Task[id=" + id + ", title=" + title + "]";
	}
}
