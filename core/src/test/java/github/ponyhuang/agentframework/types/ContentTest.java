package github.ponyhuang.agentframework.types;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Content class.
 * Tests Content types: TEXT, FUNCTION_CALL, FUNCTION_RESULT, IMAGE.
 */
class ContentTest {

    /**
     * Test Content.text() factory method.
     * Verifies it creates a TEXT type content.
     */
    @Test
    void testTextContentFactory() {
        Content content = Content.text("Hello, world!");

        // Verify type is TEXT
        assertEquals(Content.ContentType.TEXT, content.getType());
        // Verify text value
        assertEquals("Hello, world!", content.getText());
    }

    /**
     * Test Content.fromFunctionCall() factory method.
     * Verifies it creates a FUNCTION_CALL type content.
     */
    @Test
    void testFunctionCallContentFactory() {
        Map<String, Object> functionCall = Map.of(
                "id", "call_123",
                "name", "get_weather",
                "arguments", Map.of("city", "Beijing")
        );

        Content content = Content.fromFunctionCall(functionCall);

        // Verify type is FUNCTION_CALL
        assertEquals(Content.ContentType.FUNCTION_CALL, content.getType());
        // Verify function call data
        assertNotNull(content.getFunctionCall());
        assertEquals("get_weather", content.getFunctionName());
    }

    /**
     * Test Content.fromFunctionResult() factory method.
     * Verifies it creates a FUNCTION_RESULT type content.
     */
    @Test
    void testFunctionResultContentFactory() {
        Content content = Content.fromFunctionResult("call_123", "get_weather", "Sunny");

        // Verify type is FUNCTION_RESULT
        assertEquals(Content.ContentType.FUNCTION_RESULT, content.getType());
        // Verify tool call ID
        assertEquals("call_123", content.getToolCallId());
        // Verify tool name
        assertEquals("get_weather", content.getToolName());
    }

    /**
     * Test getFunctionName() method.
     * Verifies extraction of function name from function call.
     */
    @Test
    void testGetFunctionName() {
        Map<String, Object> functionCall = Map.of(
                "name", "test_function",
                "arguments", Map.of()
        );

        Content content = Content.fromFunctionCall(functionCall);

        // Verify function name extraction
        assertEquals("test_function", content.getFunctionName());
    }

    /**
     * Test getFunctionName() returns null for non-function call content.
     * Verifies proper null handling.
     */
    @Test
    void testGetFunctionNameReturnsNullForText() {
        Content content = Content.text("Regular text");

        // Verify null is returned for text content
        assertNull(content.getFunctionName());
    }

    /**
     * Test getFunctionArguments() method.
     * Verifies extraction of function arguments from function call.
     */
    @Test
    void testGetFunctionArguments() {
        Map<String, Object> args = Map.of("city", "Beijing", "unit", "celsius");
        Map<String, Object> functionCall = Map.of(
                "name", "get_weather",
                "arguments", args
        );

        Content content = Content.fromFunctionCall(functionCall);

        // Verify function arguments extraction
        Map<String, Object> extractedArgs = content.getFunctionArguments();
        assertNotNull(extractedArgs);
        assertEquals("Beijing", extractedArgs.get("city"));
        assertEquals("celsius", extractedArgs.get("unit"));
    }

    /**
     * Test getFunctionArguments() returns null for non-function call.
     * Verifies proper null handling.
     */
    @Test
    void testGetFunctionArgumentsReturnsNullForText() {
        Content content = Content.text("Regular text");

        // Verify null is returned for text content
        assertNull(content.getFunctionArguments());
    }

    /**
     * Test ImageContent creation.
     * Verifies ImageContent stores url and detail.
     */
    @Test
    void testImageContentCreation() {
        Content.ImageContent imageContent = new Content.ImageContent("https://example.com/image.png");

        // Verify url
        assertEquals("https://example.com/image.png", imageContent.getUrl());
        // Verify default detail
        assertEquals("auto", imageContent.getDetail());
    }

    /**
     * Test ImageContent with custom detail.
     * Verifies custom detail is stored.
     */
    @Test
    void testImageContentWithCustomDetail() {
        Content.ImageContent imageContent = new Content.ImageContent("https://example.com/image.png", "high");

        // Verify custom detail
        assertEquals("high", imageContent.getDetail());
    }

    /**
     * Test Content.builder() with all fields.
     * Verifies builder pattern works correctly.
     */
    @Test
    void testContentBuilderWithAllFields() {
        Map<String, Object> functionCall = Map.of("name", "test", "arguments", Map.of());

        Content content = Content.builder()
                .type(Content.ContentType.FUNCTION_CALL)
                .functionCall(functionCall)
                .build();

        // Verify type
        assertEquals(Content.ContentType.FUNCTION_CALL, content.getType());
        // Verify function call
        assertNotNull(content.getFunctionCall());
    }

    /**
     * Test Content.getFunctionResult() returns result wrapped in Map.
     * Verifies the result is wrapped with "result" key.
     */
    @Test
    void testFunctionResultWrappedInMap() {
        Content content = Content.fromFunctionResult("call_123", "test_tool", "result_value");

        // Verify function result is wrapped
        Map<String, Object> result = content.getFunctionResult();
        assertNotNull(result);
        assertEquals("result_value", result.get("result"));
    }

    /**
     * Test Content.getFunctionResult() with null result.
     * Verifies null result handling.
     */
    @Test
    void testFunctionResultWithNullResult() {
        Content content = Content.fromFunctionResult("call_123", "test_tool", null);

        // Verify function result is null
        assertNull(content.getFunctionResult());
    }

    /**
     * Test ContentType enum getValue() method.
     * Verifies each enum value has correct string representation.
     */
    @Test
    void testContentTypeEnumValues() {
        // Verify TEXT type value
        assertEquals("text", Content.ContentType.TEXT.getValue());
        // Verify IMAGE type value
        assertEquals("image", Content.ContentType.IMAGE.getValue());
        // Verify FUNCTION_CALL type value
        assertEquals("function_call", Content.ContentType.FUNCTION_CALL.getValue());
        // Verify FUNCTION_RESULT type value
        assertEquals("function_result", Content.ContentType.FUNCTION_RESULT.getValue());
    }
}
