package github.ponyhuang.agentframework.types;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Role enum.
 * Tests Role.fromValue() method and enum values.
 */
class RoleTest {

    /**
     * Test Role.fromValue() with "user" value.
     * Verifies USER role is returned.
     */
    @Test
    void testFromValueUser() {
        Role role = Role.fromValue("user");

        // Verify USER role is returned
        assertEquals(Role.USER, role);
    }

    /**
     * Test Role.fromValue() with "assistant" value.
     * Verifies ASSISTANT role is returned.
     */
    @Test
    void testFromValueAssistant() {
        Role role = Role.fromValue("assistant");

        // Verify ASSISTANT role is returned
        assertEquals(Role.ASSISTANT, role);
    }

    /**
     * Test Role.fromValue() with "system" value.
     * Verifies SYSTEM role is returned.
     */
    @Test
    void testFromValueSystem() {
        Role role = Role.fromValue("system");

        // Verify SYSTEM role is returned
        assertEquals(Role.SYSTEM, role);
    }

    /**
     * Test Role.fromValue() with "tool" value.
     * Verifies TOOL role is returned.
     */
    @Test
    void testFromValueTool() {
        Role role = Role.fromValue("tool");

        // Verify TOOL role is returned
        assertEquals(Role.TOOL, role);
    }

    /**
     * Test Role.fromValue() is case insensitive.
     * Verifies uppercase values work.
     */
    @Test
    void testFromValueCaseInsensitive() {
        // Verify case insensitivity
        assertEquals(Role.USER, Role.fromValue("USER"));
        assertEquals(Role.ASSISTANT, Role.fromValue("ASSISTANT"));
        assertEquals(Role.SYSTEM, Role.fromValue("SYSTEM"));
        assertEquals(Role.TOOL, Role.fromValue("TOOL"));
    }

    /**
     * Test Role.fromValue() throws IllegalArgumentException for unknown value.
     * Verifies exception handling.
     */
    @Test
    void testFromValueThrowsExceptionForUnknown() {
        // Verify exception is thrown for unknown role
        assertThrows(IllegalArgumentException.class, () -> Role.fromValue("unknown"));
    }

    /**
     * Test Role.fromValue() throws IllegalArgumentException for null.
     * Verifies null handling.
     */
    @Test
    void testFromValueThrowsExceptionForNull() {
        // Verify exception is thrown for null
        assertThrows(IllegalArgumentException.class, () -> Role.fromValue(null));
    }

    /**
     * Test Role.getValue() returns correct string values.
     * Verifies enum string representation.
     */
    @Test
    void testGetValue() {
        // Verify each role returns correct value
        assertEquals("user", Role.USER.getValue());
        assertEquals("assistant", Role.ASSISTANT.getValue());
        assertEquals("system", Role.SYSTEM.getValue());
        assertEquals("tool", Role.TOOL.getValue());
    }

    /**
     * Test Role enum has correct number of values.
     * Verifies enum completeness.
     */
    @Test
    void testRoleEnumValues() {
        Role[] roles = Role.values();

        // Verify all 4 roles exist
        assertEquals(4, roles.length);
    }

    /**
     * Test Role.valueOf() works correctly.
     * Verifies standard enum method.
     */
    @Test
    void testValueOf() {
        // Verify valueOf works
        assertEquals(Role.USER, Role.valueOf("USER"));
        assertEquals(Role.ASSISTANT, Role.valueOf("ASSISTANT"));
        assertEquals(Role.SYSTEM, Role.valueOf("SYSTEM"));
        assertEquals(Role.TOOL, Role.valueOf("TOOL"));
    }
}
