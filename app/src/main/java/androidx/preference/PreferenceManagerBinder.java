package androidx.preference;

/**
 * A helper to attach a new {@link PreferenceManager} to a {@link Preference}. This class is in the {@code androidx.preference} package to hack
 * around the protected visibility of {@link Preference#onAttachedToHierarchy(PreferenceManager)}.
 */
public class PreferenceManagerBinder {
    private PreferenceManagerBinder() {
    }

    public static void bind(Preference pref, PreferenceManager manager) {
        pref.onAttachedToHierarchy(manager);
    }
}
