package github.ponyhuang.agentframework.hooks;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Hook matcher that filters when hooks fire based on regex patterns.
 */
public class HookMatcher {

    private final Pattern pattern;

    /**
     * Creates a HookMatcher with the given regex pattern.
     *
     * @param matcher the regex pattern (null or "*" matches all)
     */
    public HookMatcher(String matcher) {
        if (matcher == null || matcher.isEmpty() || "*".equals(matcher)) {
            this.pattern = null; // Matches all
        } else {
            this.pattern = Pattern.compile(matcher);
        }
    }

    /**
     * Checks if this matcher matches the given value.
     *
     * @param value the value to match against
     * @return true if matches, false otherwise
     */
    public boolean matches(String value) {
        if (pattern == null) {
            return true; // Null pattern matches all
        }
        if (value == null) {
            return false;
        }
        return pattern.matcher(value).matches();
    }

    /**
     * Checks if this matcher matches the given value (find, not exact match).
     *
     * @param value the value to match against
     * @return true if the pattern is found, false otherwise
     */
    public boolean matchesContains(String value) {
        if (pattern == null) {
            return true;
        }
        if (value == null) {
            return false;
        }
        return pattern.matcher(value).find();
    }

    /**
     * Validates if a given string is a valid regex pattern.
     *
     * @param pattern the pattern to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidPattern(String pattern) {
        try {
            Pattern.compile(pattern);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    /**
     * Gets the pattern string.
     *
     * @return the pattern string, or null if matches all
     */
    public String getPatternString() {
        return pattern != null ? pattern.pattern() : null;
    }

    @Override
    public String toString() {
        return "HookMatcher{" +
                "pattern=" + (pattern != null ? pattern.pattern() : "*") +
                '}';
    }

    /**
     * Builder for HookMatcher.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String matcher;

        public Builder matcher(String matcher) {
            this.matcher = matcher;
            return this;
        }

        public HookMatcher build() {
            return new HookMatcher(matcher);
        }
    }
}
