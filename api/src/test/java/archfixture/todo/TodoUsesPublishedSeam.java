package archfixture.todo;

import archfixture.auth.api.NotTheSeam;

/** Control, not a violation of the todo rule: to-dos may use the published package auth.api. */
public class TodoUsesPublishedSeam {
    NotTheSeam published = new NotTheSeam();
}
