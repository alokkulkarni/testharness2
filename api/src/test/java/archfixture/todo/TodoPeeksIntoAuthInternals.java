package archfixture.todo;

import archfixture.auth.AuthInternals;

/** Deliberate violation: todo uses a type auth does not publish (rule "todo uses only todo, common and auth.api"). */
public class TodoPeeksIntoAuthInternals {
    AuthInternals internals = new AuthInternals();
}
