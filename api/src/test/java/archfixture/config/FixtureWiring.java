package archfixture.config;

import archfixture.auth.AuthInternals;
import archfixture.todo.TodoCore;

/** Fixture: the composition root may use any module. */
public class FixtureWiring {
    AuthInternals auth = new AuthInternals();
    TodoCore todo = new TodoCore();
}
