package com.yao.crm.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReportUtils
 */
class ReportUtilsTest {

    // ========== normalizeFromDate tests ==========

    @Test
    @DisplayName("shouldReturnFromDate_whenFromDateIsNotNull")
    void shouldReturnFromDate_whenFromDateIsNotNull() {
        LocalDate fromDate = LocalDate.of(2026, 1, 15);
        LocalDate toDate = LocalDate.of(2026, 1, 31);

        LocalDate result = ReportUtils.normalizeFromDate(fromDate, toDate);

        assertEquals(fromDate, result);
    }

    @Test
    @DisplayName("shouldReturnOpenRangeStart_whenFromDateIsNullAndToDateNotNull")
    void shouldReturnOpenRangeStart_whenFromDateIsNullAndToDateNotNull() {
        LocalDate toDate = LocalDate.of(2026, 1, 31);

        LocalDate result = ReportUtils.normalizeFromDate(null, toDate);

        assertEquals(LocalDate.of(1970, 1, 1), result);
    }

    @Test
    @DisplayName("shouldReturnNull_whenBothDatesAreNull_forNormalizeFromDate")
    void shouldReturnNull_whenBothDatesAreNull_forNormalizeFromDate() {
        LocalDate result = ReportUtils.normalizeFromDate(null, null);

        assertNull(result);
    }

    // ========== normalizeToDate tests ==========

    @Test
    @DisplayName("shouldReturnToDate_whenToDateIsNotNull")
    void shouldReturnToDate_whenToDateIsNotNull() {
        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 1, 31);

        LocalDate result = ReportUtils.normalizeToDate(fromDate, toDate);

        assertEquals(toDate, result);
    }

    @Test
    @DisplayName("shouldReturnOpenRangeEnd_whenToDateIsNullAndFromDateNotNull")
    void shouldReturnOpenRangeEnd_whenToDateIsNullAndFromDateNotNull() {
        LocalDate fromDate = LocalDate.of(2026, 1, 1);

        LocalDate result = ReportUtils.normalizeToDate(fromDate, null);

        assertEquals(LocalDate.of(2099, 12, 31), result);
    }

    @Test
    @DisplayName("shouldReturnNull_whenBothDatesAreNull_forNormalizeToDate")
    void shouldReturnNull_whenBothDatesAreNull_forNormalizeToDate() {
        LocalDate result = ReportUtils.normalizeToDate(null, null);

        assertNull(result);
    }

    // ========== safeLong tests ==========

    @Test
    @DisplayName("shouldReturnZero_whenValueIsNull")
    void shouldReturnZero_whenValueIsNull() {
        long result = ReportUtils.safeLong(null);

        assertEquals(0L, result);
    }

    @Test
    @DisplayName("shouldReturnValue_whenValueIsNotNull")
    void shouldReturnValue_whenValueIsNotNull() {
        long result = ReportUtils.safeLong(12345L);

        assertEquals(12345L, result);
    }

    @Test
    @DisplayName("shouldReturnCorrectValue_whenValueIsZero")
    void shouldReturnCorrectValue_whenValueIsZero() {
        long result = ReportUtils.safeLong(0L);

        assertEquals(0L, result);
    }

    @Test
    @DisplayName("shouldReturnCorrectValue_whenValueIsNegative")
    void shouldReturnCorrectValue_whenValueIsNegative() {
        long result = ReportUtils.safeLong(-500L);

        assertEquals(-500L, result);
    }

    // ========== toIntMap tests ==========

    @Test
    @DisplayName("shouldReturnEmptyMap_whenInputIsNull")
    void shouldReturnEmptyMap_whenInputIsNull() {
        Map<String, Integer> result = ReportUtils.toIntMap(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("shouldConvertToMap_whenValidInput")
    void shouldConvertToMap_whenValidInput() {
        List<Object[]> rows = Arrays.asList(
                new Object[]{"key1", 10},
                new Object[]{"key2", 20},
                new Object[]{"key3", 30}
        );

        Map<String, Integer> result = ReportUtils.toIntMap(rows);

        assertEquals(3, result.size());
        assertEquals(10, result.get("key1"));
        assertEquals(20, result.get("key2"));
        assertEquals(30, result.get("key3"));
    }

    @Test
    @DisplayName("shouldHandleNullKey_whenConvertToIntMap")
    void shouldHandleNullKey_whenConvertToIntMap() {
        List<Object[]> rows = Collections.singletonList(
                new Object[]{null, 10}
        );

        Map<String, Integer> result = ReportUtils.toIntMap(rows);

        assertEquals(1, result.size());
        assertEquals(10, result.get("Unknown")); // null key becomes "Unknown"
    }

    @Test
    @DisplayName("shouldHandleNullValue_whenConvertToIntMap")
    void shouldHandleNullValue_whenConvertToIntMap() {
        List<Object[]> rows = Collections.singletonList(
                new Object[]{"key1", null}
        );

        Map<String, Integer> result = ReportUtils.toIntMap(rows);

        assertEquals(1, result.size());
        assertEquals(0, result.get("key1")); // null value becomes 0
    }

    // ========== toLongMap tests ==========

    @Test
    @DisplayName("shouldReturnEmptyMap_whenInputIsNull_forToLongMap")
    void shouldReturnEmptyMap_whenInputIsNull_forToLongMap() {
        Map<String, Long> result = ReportUtils.toLongMap(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("shouldConvertToLongMap_whenValidInput")
    void shouldConvertToLongMap_whenValidInput() {
        List<Object[]> rows = Arrays.asList(
                new Object[]{"key1", 1000L},
                new Object[]{"key2", 2000L}
        );

        Map<String, Long> result = ReportUtils.toLongMap(rows);

        assertEquals(2, result.size());
        assertEquals(1000L, result.get("key1"));
        assertEquals(2000L, result.get("key2"));
    }

    // ========== isBlank tests ==========

    @Test
    @DisplayName("shouldReturnTrue_whenInputIsNull")
    void shouldReturnTrue_whenInputIsNull() {
        assertTrue(ReportUtils.isBlank(null));
    }

    @Test
    @DisplayName("shouldReturnTrue_whenInputIsEmpty")
    void shouldReturnTrue_whenInputIsEmpty() {
        assertTrue(ReportUtils.isBlank(""));
    }

    @Test
    @DisplayName("shouldReturnTrue_whenInputIsWhitespace")
    void shouldReturnTrue_whenInputIsWhitespace() {
        assertTrue(ReportUtils.isBlank("   "));
        assertTrue(ReportUtils.isBlank("\t\n"));
    }

    @Test
    @DisplayName("shouldReturnFalse_whenInputHasContent")
    void shouldReturnFalse_whenInputHasContent() {
        assertFalse(ReportUtils.isBlank("test"));
        assertFalse(ReportUtils.isBlank("  test  "));
    }

    // ========== normalized tests ==========

    @Test
    @DisplayName("shouldReturnEmpty_whenInputIsNull")
    void shouldReturnEmpty_whenInputIsNull() {
        assertEquals("", ReportUtils.normalized(null));
    }

    @Test
    @DisplayName("shouldReturnLowercaseTrimmed_whenValidInput")
    void shouldReturnLowercaseTrimmed_whenValidInput() {
        assertEquals("hello world", ReportUtils.normalized("  Hello World  "));
    }

    @Test
    @DisplayName("shouldReturnEmpty_whenInputIsBlank")
    void shouldReturnEmpty_whenInputIsBlank() {
        assertEquals("", ReportUtils.normalized("   "));
    }

    // ========== normalizeBucket tests ==========

    @Test
    @DisplayName("shouldReturnUnknown_whenInputIsNull")
    void shouldReturnUnknown_whenInputIsNull() {
        assertEquals("Unknown", ReportUtils.normalizeBucket(null));
    }

    @Test
    @DisplayName("shouldReturnUnknown_whenInputIsEmpty")
    void shouldReturnUnknown_whenInputIsEmpty() {
        assertEquals("Unknown", ReportUtils.normalizeBucket(""));
    }

    @Test
    @DisplayName("shouldReturnValue_whenValidInput")
    void shouldReturnValue_whenValidInput() {
        assertEquals("test", ReportUtils.normalizeBucket("test"));
        assertEquals("123", ReportUtils.normalizeBucket(123));
    }

    // ========== asInt tests ==========

    @Test
    @DisplayName("shouldReturnZero_whenInputIsNull_forAsInt")
    void shouldReturnZero_whenInputIsNull_forAsInt() {
        assertEquals(0, ReportUtils.asInt(null));
    }

    @Test
    @DisplayName("shouldReturnIntValue_whenInputIsNumber")
    void shouldReturnIntValue_whenInputIsNumber() {
        assertEquals(42, ReportUtils.asInt(42L));
        assertEquals(42, ReportUtils.asInt(42));
        assertEquals(42, ReportUtils.asInt(42.5));
    }

    @Test
    @DisplayName("shouldParseString_whenInputIsString")
    void shouldParseString_whenInputIsString() {
        assertEquals(123, ReportUtils.asInt("123"));
    }

    @Test
    @DisplayName("shouldReturnZero_whenInputIsInvalidString")
    void shouldReturnZero_whenInputIsInvalidString() {
        assertEquals(0, ReportUtils.asInt("not a number"));
    }

    // ========== asLong tests ==========

    @Test
    @DisplayName("shouldReturnZero_whenInputIsNull_forAsLong")
    void shouldReturnZero_whenInputIsNull_forAsLong() {
        assertEquals(0L, ReportUtils.asLong(null));
    }

    @Test
    @DisplayName("shouldReturnLongValue_whenInputIsNumber")
    void shouldReturnLongValue_whenInputIsNumber() {
        assertEquals(123456789L, ReportUtils.asLong(123456789L));
        assertEquals(123L, ReportUtils.asLong(123));
    }

    // ========== startOfDay tests ==========

    @Test
    @DisplayName("shouldReturnNull_whenInputIsNull_forStartOfDay")
    void shouldReturnNull_whenInputIsNull_forStartOfDay() {
        assertNull(ReportUtils.startOfDay(null));
    }

    @Test
    @DisplayName("shouldReturnStartOfDay_whenValidInput")
    void shouldReturnStartOfDay_whenValidInput() {
        LocalDate date = LocalDate.of(2026, 1, 15);
        LocalDateTime result = ReportUtils.startOfDay(date);

        assertEquals(LocalDateTime.of(2026, 1, 15, 0, 0, 0), result);
    }

    // ========== endOfDay tests ==========

    @Test
    @DisplayName("shouldReturnNull_whenInputIsNull_forEndOfDay")
    void shouldReturnNull_whenInputIsNull_forEndOfDay() {
        assertNull(ReportUtils.endOfDay(null));
    }

    @Test
    @DisplayName("shouldReturnEndOfDay_whenValidInput")
    void shouldReturnEndOfDay_whenValidInput() {
        LocalDate date = LocalDate.of(2026, 1, 15);
        LocalDateTime result = ReportUtils.endOfDay(date);

        assertEquals(LocalDateTime.of(2026, 1, 15, 23, 59, 59, 999999999), result);
    }

    // ========== castMap tests ==========

    @Test
    @DisplayName("shouldReturnEmptyMap_whenInputIsNull")
    void shouldReturnEmptyMap_whenInputIsNull_forCastMap() {
        Map<String, Object> result = ReportUtils.castMap(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("shouldReturnEmptyMap_whenInputIsNotMap")
    void shouldReturnEmptyMap_whenInputIsNotMap() {
        Map<String, Object> result = ReportUtils.castMap("not a map");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("shouldReturnMap_whenInputIsMap")
    void shouldReturnMap_whenInputIsMap() {
        Map<String, Object> input = new java.util.HashMap<String, Object>();
        input.put("key", "value");

        Map<String, Object> result = ReportUtils.castMap(input);

        assertEquals(input, result);
    }

    // ========== normalizeRole tests ==========

    @Test
    @DisplayName("shouldReturnUppercase_whenValidRole")
    void shouldReturnUppercase_whenValidRole() {
        assertEquals("ADMIN", ReportUtils.normalizeRole("admin"));
        assertEquals("USER", ReportUtils.normalizeRole("  user  "));
    }

    @Test
    @DisplayName("shouldHandleNull_whenNormalizeRole")
    void shouldHandleNull_whenNormalizeRole() {
        // null becomes empty string, then uppercase (still empty)
        assertEquals("", ReportUtils.normalizeRole(null));
    }

    // ========== normalizeDate tests ==========

    @Test
    @DisplayName("shouldReturnEmpty_whenInputIsNull_forNormalizeDate")
    void shouldReturnEmpty_whenInputIsNull_forNormalizeDate() {
        assertEquals("", ReportUtils.normalizeDate(null));
    }

    @Test
    @DisplayName("shouldReturnString_whenValidDate")
    void shouldReturnString_whenValidDate() {
        LocalDate date = LocalDate.of(2026, 1, 15);
        assertEquals("2026-01-15", ReportUtils.normalizeDate(date));
    }

    // ========== normalizeCacheValue tests ==========

    @Test
    @DisplayName("shouldReturnEmptyString_whenInputIsNull")
    void shouldReturnEmptyString_whenInputIsNull() {
        assertEquals("", ReportUtils.normalizeCacheValue(null));
    }

    @Test
    @DisplayName("shouldReturnTrimmedValue_whenValidInput")
    void shouldReturnTrimmedValue_whenValidInput() {
        assertEquals("value", ReportUtils.normalizeCacheValue("  value  "));
    }
}
