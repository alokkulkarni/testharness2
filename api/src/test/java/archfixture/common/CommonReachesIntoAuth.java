package archfixture.common;

import archfixture.auth.AuthInternals;

/** Deliberate violation: the shared kernel uses a module (rule "common uses no module and not config"). */
public class CommonReachesIntoAuth {
    AuthInternals internals = new AuthInternals();
}
