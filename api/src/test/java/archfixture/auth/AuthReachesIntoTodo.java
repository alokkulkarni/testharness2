package archfixture.auth;

import archfixture.todo.TodoCore;

/** Deliberate violation: auth uses the to-dos module (rule "auth uses only auth and common"; also closes a cycle). */
public class AuthReachesIntoTodo {
    TodoCore core = new TodoCore();
}
