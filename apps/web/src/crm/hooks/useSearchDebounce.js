import { useState, useEffect, useRef, useCallback } from 'react';

/**
 * 搜索防抖Hook - 用于搜索输入的防抖处理
 * @param {string} initialValue - 初始值
 * @param {Function} onSearch - 搜索回调函数
 * @param {number} delay - 防抖延迟（毫秒）
 */
export function useSearchDebounce(initialValue = '', onSearch, delay = 300) {
  const [value, setValue] = useState(initialValue);
  const [debouncedValue, setDebouncedValue] = useState(initialValue);
  const timerRef = useRef(null);
  const onSearchRef = useRef(onSearch);

  // 保持onSearch最新引用
  useEffect(() => {
    onSearchRef.current = onSearch;
  }, [onSearch]);

  // 防抖逻辑
  useEffect(() => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }
    
    timerRef.current = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }
    };
  }, [value, delay]);

  // 当debouncedValue变化时触发搜索
  useEffect(() => {
    if (onSearchRef.current && debouncedValue !== undefined) {
      onSearchRef.current(debouncedValue);
    }
  }, [debouncedValue]);

  const handleChange = useCallback((newValue) => {
    setValue(newValue);
  }, []);

  const handleSubmit = useCallback(() => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }
    setDebouncedValue(value);
  }, [value]);

  return {
    value,
    debouncedValue,
    setValue: handleChange,
    submit: handleSubmit
  };
}

/**
 * 纯防抖Hook - 不触发回调，只更新防抖值
 */
export function useDebounce(value, delay = 300) {
  const [debouncedValue, setDebouncedValue] = useState(value);
  const timerRef = useRef(null);

  useEffect(() => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }

    timerRef.current = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }
    };
  }, [value, delay]);

  return debouncedValue;
}
