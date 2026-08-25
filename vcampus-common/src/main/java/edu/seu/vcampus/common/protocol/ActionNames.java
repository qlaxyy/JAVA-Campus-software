package edu.seu.vcampus.common.protocol;

import java.util.Locale;
import java.util.Objects;

/**
 * Creates consistently formatted public action names.
 */
public final class ActionNames {

    private ActionNames() {
    }

    /**
     * Creates an action in {@code MODULE.VERB} form.
     *
     * @param module one of the identifiers in {@link ModuleNames}
     * @param verb uppercase business operation without spaces or dots
     * @return validated action name
     */
    public static String of(String module, String verb) {
        String normalizedModule = normalize(module, "module");
        String normalizedVerb = normalize(verb, "verb");
        if (!ModuleNames.isSupported(normalizedModule)) {
            throw new IllegalArgumentException("unsupported module: " + module);
        }
        if (!normalizedVerb.matches("[A-Z][A-Z0-9_]*")) {
            throw new IllegalArgumentException("verb must contain only letters, digits or underscores");
        }
        return normalizedModule + "." + normalizedVerb;
    }

    private static String normalize(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
