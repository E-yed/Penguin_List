const DAY_ORDER = ["yesterday", "today", "tomorrow"];
const DAY_LABELS = {
    yesterday: "Yesterday",
    today: "Today",
    tomorrow: "Tomorrow"
};

const DAY_COPY = {
    yesterday: {
        title: "Yesterday left receipts.",
        subtitle: "Look back at what got finished, what slipped, and what needs another shot today."
    },
    today: {
        title: "Today is where the list gets smaller.",
        subtitle: "Use the web app at the root. Hit <code>/api</code> when you want the same data as JSON."
    },
    tomorrow: {
        title: "Tomorrow starts getting easier tonight.",
        subtitle: "Stack tomorrow's tasks now so future you walks into a plan instead of a mess."
    }
};

const state = {
    day: resolveDayFromPath(),
    data: null
};

const elements = {
    dayTabs: [...document.querySelectorAll(".day-tab")],
    heroTitle: document.getElementById("hero-title"),
    heroSubtitle: document.getElementById("hero-subtitle"),
    dayTitle: document.getElementById("day-title"),
    dayMeta: document.getElementById("day-meta"),
    summaryPill: document.getElementById("summary-pill"),
    pendingCount: document.getElementById("pending-count"),
    doneCount: document.getElementById("done-count"),
    pendingList: document.getElementById("pending-list"),
    doneList: document.getElementById("done-list"),
    penguinArt: document.getElementById("penguin-art"),
    todoForm: document.getElementById("todo-form"),
    titleInput: document.getElementById("title-input"),
    addDescription: document.getElementById("add-description"),
    descriptionList: document.getElementById("description-list"),
    formMessage: document.getElementById("form-message"),
    taskTemplate: document.getElementById("task-template")
};

bootstrap();

function bootstrap() {
    bindEvents();
    addDescriptionField();
    loadDay(state.day);
}

function bindEvents() {
    elements.dayTabs.forEach((button) => {
        button.addEventListener("click", () => navigateToDay(button.dataset.day));
    });

    window.addEventListener("popstate", () => {
        const day = resolveDayFromPath();
        loadDay(day);
    });

    elements.todoForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        const title = elements.titleInput.value.trim();
        const descriptions = [...elements.descriptionList.querySelectorAll("input")]
            .map((input) => input.value.trim())
            .filter(Boolean);

        if (!title) {
            setFormMessage("Title is required.");
            return;
        }

        await fetchJson(`/api/todos/${state.day}`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ title, descriptions })
        });

        elements.todoForm.reset();
        elements.descriptionList.innerHTML = "";
        addDescriptionField();
        setFormMessage("Task added.");
        await loadDay(state.day);
    });

    elements.addDescription.addEventListener("click", () => addDescriptionField());
}

async function loadDay(day) {
    state.day = day;
    markActiveTab(day);
    elements.dayTitle.textContent = DAY_LABELS[day];
    elements.dayMeta.textContent = "Loading";
    elements.summaryPill.textContent = "Syncing";

    const response = await fetchJson(`/api/todos/${day}`);
    state.data = response;
    render();
    history.replaceState({}, "", day === "today" ? "/" : `/${day}`);
}

function render() {
    const pending = state.data.pending ?? [];
    const done = state.data.done ?? [];
    const total = pending.length + done.length;
    const copy = DAY_COPY[state.day];

    elements.heroTitle.textContent = copy.title;
    elements.heroSubtitle.innerHTML = copy.subtitle;
    elements.pendingCount.textContent = String(pending.length);
    elements.doneCount.textContent = String(done.length);
    elements.dayMeta.textContent = `Pending ${pending.length}, done ${done.length}`;
    elements.summaryPill.textContent = `${total} total task${total === 1 ? "" : "s"}`;
    elements.penguinArt.src = getPenguinArt(state.day);

    renderTaskList(elements.pendingList, pending, "No pending tasks for this day.");
    renderTaskList(elements.doneList, done, "Nothing finished here yet.");
}

function renderTaskList(container, items, emptyMessage) {
    container.innerHTML = "";

    if (!items.length) {
        const empty = document.createElement("div");
        empty.className = "empty-state";
        empty.textContent = emptyMessage;
        container.appendChild(empty);
        return;
    }

    items.forEach((item) => {
        const fragment = elements.taskTemplate.content.cloneNode(true);
        const card = fragment.querySelector(".task-card");
        const checkbox = fragment.querySelector(".todo-checkbox");
        const title = fragment.querySelector(".task-title");
        const descriptions = fragment.querySelector(".task-descriptions");
        const deleteBtn = fragment.querySelector(".delete-btn");

        title.textContent = item.title;
        checkbox.checked = item.done;
        checkbox.disabled = item.descriptions.length > 0;
        card.classList.toggle("done", item.done);

        checkbox.addEventListener("change", async () => {
            await fetchJson(`/api/todos/${state.day}/${item.id}`, {
                method: "PATCH",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ done: checkbox.checked })
            });
            await loadDay(state.day);
        });

        deleteBtn.addEventListener("click", async () => {
            await fetch(`/api/todos/${state.day}/${item.id}`, { method: "DELETE" });
            await loadDay(state.day);
        });

        if (item.descriptions.length) {
            item.descriptions.forEach((description, index) => {
                const li = document.createElement("li");
                const button = document.createElement("button");
                button.type = "button";
                button.className = description.done ? "done" : "";
                button.textContent = description.text;
                button.addEventListener("click", async () => {
                    await fetchJson(`/api/todos/${state.day}/${item.id}/descriptions/${index}`, {
                        method: "PATCH",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({ done: !description.done })
                    });
                    await loadDay(state.day);
                });
                li.appendChild(button);
                descriptions.appendChild(li);
            });
        } else {
            const li = document.createElement("li");
            li.textContent = item.done ? "Completed task" : "Standalone task";
            descriptions.appendChild(li);
        }

        container.appendChild(fragment);
    });
}

function addDescriptionField(value = "") {
    const row = document.createElement("div");
    row.className = "description-row";

    const input = document.createElement("input");
    input.type = "text";
    input.maxLength = 120;
    input.placeholder = "Break down the task into micro steps here (optional)";
    input.value = value;

    const removeButton = document.createElement("button");
    removeButton.type = "button";
    removeButton.textContent = "X";
    removeButton.addEventListener("click", () => {
        row.remove();
        if (!elements.descriptionList.children.length) {
            addDescriptionField();
        }
    });

    row.append(input, removeButton);
    elements.descriptionList.appendChild(row);
}

function navigateToDay(day) {
    history.pushState({}, "", day === "today" ? "/" : `/${day}`);
    loadDay(day);
}

function markActiveTab(day) {
    elements.dayTabs.forEach((button) => {
        button.classList.toggle("active", button.dataset.day === day);
    });
}

function resolveDayFromPath() {
    const path = window.location.pathname.replace(/^\/+|\/+$/g, "");
    return DAY_ORDER.includes(path) ? path : "today";
}

function getPenguinArt(day) {
    if (day === "tomorrow") {
        return "/assets/gunther_thinking.png";
    }
    if (day === "today") {
        return "/assets/gunther-suit.png";
    }
    return "/assets/gunther-beach.png";
}

function setFormMessage(message) {
    elements.formMessage.textContent = message;
}

async function fetchJson(url, options = {}) {
    const response = await fetch(url, options);
    if (!response.ok) {
        let message = "Request failed.";
        try {
            const error = await response.json();
            message = error.error ?? error.message ?? message;
        } catch {
            // Ignore non-JSON errors.
        }
        setFormMessage(message);
        throw new Error(message);
    }
    return response.status === 204 ? null : response.json();
}
