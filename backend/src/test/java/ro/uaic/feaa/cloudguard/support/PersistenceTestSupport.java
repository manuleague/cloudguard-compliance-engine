package ro.uaic.feaa.cloudguard.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/** Ajutor pentru teste: excepțiile aruncate din listeneri pot ajunge învelite de JPA sau de Spring. */
public final class PersistenceTestSupport {

    private PersistenceTestSupport() {
    }

    /** Verifică faptul că excepția aruncată sau una dintre cauzele ei este de tipul cerut. */
    public static void assertThrowsInChain(ThrowingCallable action, Class<? extends Throwable> expected) {
        Throwable thrown = null;
        try {
            action.call();
        } catch (Throwable t) {
            thrown = t;
        }
        assertThat(thrown).as("era așteptată o excepție").isNotNull();
        Throwable cursor = thrown;
        while (cursor != null) {
            if (expected.isInstance(cursor)) {
                return;
            }
            cursor = cursor.getCause() == cursor ? null : cursor.getCause();
        }
        throw new AssertionError("Nicio excepție din lanț nu este de tipul " + expected.getSimpleName()
                + ", ci: " + thrown, thrown);
    }
}
