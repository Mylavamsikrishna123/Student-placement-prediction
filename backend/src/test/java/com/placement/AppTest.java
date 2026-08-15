package com.placement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for App's static helper methods (JSON parsing, escaping, validation,
 * query parsing). These exercise the real code paths used by the HTTP handlers
 * without spinning up the HTTP server or a database.
 */
@DisplayName("App static helper tests")
public class AppTest {

    // ---------- escape ----------

    @Nested
    @DisplayName("escape()")
    class EscapeTests {
        @Test
        @DisplayName("null becomes empty string")
        public void escapeNull() {
            assertEquals("", App.escape(null));
        }

        @Test
        @DisplayName("backslash and quote are escaped")
        public void escapeQuotesAndBackslashes() {
            assertEquals("a\\\\b\\\"c", App.escape("a\\b\"c"));
        }

        @Test
        @DisplayName("newlines, carriage returns and tabs are escaped")
        public void escapeControlChars() {
            assertEquals("a\\nb\\rc\\td", App.escape("a\nb\rc\td"));
        }
    }

    // ---------- isValidEmail ----------

    @Nested
    @DisplayName("isValidEmail()")
    class EmailTests {
        @Test
        public void validEmails() {
            assertTrue(App.isValidEmail("student@example.com"));
            assertTrue(App.isValidEmail("a.b-c_d+tag@sub.domain.org"));
        }

        @Test
        public void invalidEmails() {
            assertFalse(App.isValidEmail(null));
            assertFalse(App.isValidEmail(""));
            assertFalse(App.isValidEmail("noatsign.com"));
            assertFalse(App.isValidEmail("user@"));
            assertFalse(App.isValidEmail("user@domain"));
            assertFalse(App.isValidEmail("user@domain.x")); // TLD too short
        }
    }

    // ---------- isValidPassword ----------

    @Nested
    @DisplayName("isValidPassword()")
    class PasswordTests {
        @Test
        public void acceptsLongEnough() {
            assertTrue(App.isValidPassword("password123"));
            assertTrue(App.isValidPassword("abcdefgh"));
        }

        @Test
        public void rejectsShortOrNull() {
            assertFalse(App.isValidPassword(null));
            assertFalse(App.isValidPassword(""));
            assertFalse(App.isValidPassword("short"));
            assertFalse(App.isValidPassword("seven77")); // 7 chars
        }
    }

    // ---------- parseJson ----------

    @Nested
    @DisplayName("parseJson()")
    class ParseJsonTests {
        @Test
        @DisplayName("null and empty bodies return empty maps")
        public void nullAndEmpty() {
            assertTrue(App.parseJson(null).isEmpty());
            assertTrue(App.parseJson("").isEmpty());
            assertTrue(App.parseJson("   ").isEmpty());
        }

        @Test
        @DisplayName("parses flat string key/value pairs with and without braces")
        public void flatStrings() {
            Map<String,String> m = App.parseJson("{\"email\":\"a@b.com\",\"password\":\"secret\"}");
            assertEquals("a@b.com", m.get("email"));
            assertEquals("secret", m.get("password"));
            assertEquals(2, m.size());
        }

        @Test
        @DisplayName("falls back to default when key missing")
        public void missingKeyReturnsNull() {
            Map<String,String> m = App.parseJson("{\"email\":\"x@y.com\"}");
            assertNull(m.get("password"));
        }

        @Test
        @DisplayName("parses without surrounding braces")
        public void parsesWithoutBraces() {
            Map<String,String> m = App.parseJson("\"role\":\"admin\",\"id\":\"42\"");
            assertEquals("admin", m.get("role"));
            assertEquals("42", m.get("id"));
        }
    }

    // ---------- parseSkills ----------

    @Nested
    @DisplayName("parseSkills()")
    class ParseSkillsTests {
        @Test
        @DisplayName("null returns empty map")
        public void nullReturnsEmpty() {
            assertTrue(App.parseSkills(null).isEmpty());
        }

        @Test
        @DisplayName("parses {\"Java\":3,\"SQL\":2}")
        public void parsesSkills() {
            Map<String,Integer> s = App.parseSkills("{\"Java\":3,\"SQL\":2}");
            assertEquals(2, s.size());
            assertEquals(3, s.get("Java"));
            assertEquals(2, s.get("SQL"));
        }

        @Test
        @DisplayName("ignores non-integer values")
        public void ignoresNonInteger() {
            Map<String,Integer> s = App.parseSkills("{\"Java\":\"high\"}");
            assertTrue(s.isEmpty());
        }
    }

    // ---------- queryToMap ----------

    @Nested
    @DisplayName("queryToMap()")
    class QueryToMapTests {
        @Test
        @DisplayName("null returns empty map")
        public void nullReturnsEmpty() {
            assertTrue(App.queryToMap(null).isEmpty());
        }

        @Test
        @DisplayName("parses a=b&c=d")
        public void parsesPairs() {
            Map<String,String> m = App.queryToMap("a=b&c=d");
            assertEquals("b", m.get("a"));
            assertEquals("d", m.get("c"));
            assertEquals(2, m.size());
        }

        @Test
        @DisplayName("decodes URL-encoded values")
        public void urlDecodes() {
            Map<String,String> m = App.queryToMap("email=user%40example.com");
            assertEquals("user@example.com", m.get("email"));
        }

        @Test
        @DisplayName("skips pairs without '='")
        public void skipsMalformed() {
            Map<String,String> m = App.queryToMap("foo&bar=baz");
            assertEquals(1, m.size());
            assertEquals("baz", m.get("bar"));
        }
    }

    // ---------- mapToJson / listToJson ----------

    @Nested
    @DisplayName("mapToJson() and listToJson()")
    class SerializeTests {
        @Test
        @DisplayName("null map -> {}")
        public void nullMap() {
            assertEquals("{}", App.mapToJson(null));
        }

        @Test
        @DisplayName("empty map -> {}")
        public void emptyMap() {
            assertEquals("{}", App.mapToJson(new LinkedHashMap<>()));
        }

        @Test
        @DisplayName("serializes string and number values")
        public void stringsAndNumbers() {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("status", "ok");
            m.put("count", 7);
            assertEquals("{\"status\":\"ok\",\"count\":7}", App.mapToJson(m));
        }

        @Test
        @DisplayName("escapes special characters in strings")
        public void escapesValues() {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("msg", "a\"b\nc");
            assertEquals("{\"msg\":\"a\\\"b\\nc\"}", App.mapToJson(m));
        }

        @Test
        @DisplayName("serializes nested maps recursively")
        public void nestedMap() {
            Map<String,Object> inner = new LinkedHashMap<>();
            inner.put("x", 1);
            Map<String,Object> outer = new LinkedHashMap<>();
            outer.put("data", inner);
            assertEquals("{\"data\":{\"x\":1}}", App.mapToJson(outer));
        }

        @Test
        @DisplayName("listToJson serializes a list of maps as an array")
        public void listOfMaps() {
            List<Map<String,Object>> list = new ArrayList<>();
            Map<String,Object> a = new LinkedHashMap<>();
            a.put("id", 1);
            list.add(a);
            Map<String,Object> b = new LinkedHashMap<>();
            b.put("id", 2);
            list.add(b);
            assertEquals("[{\"id\":1},{\"id\":2}]", App.listToJson(list));
        }
    }
}
