import { describe, it, expect } from 'vitest'

describe('示例测试', () => {
  it('应该通过基本断言', () => {
    expect(1 + 1).toBe(2)
  })

  it('应该正确处理数组', () => {
    const arr = [1, 2, 3]
    expect(arr.length).toBe(3)
    expect(arr).toContain(2)
  })

  it('应该正确处理对象', () => {
    const obj = { name: 'test', value: 42 }
    expect(obj.name).toBe('test')
    expect(obj).toHaveProperty('value', 42)
  })
})
