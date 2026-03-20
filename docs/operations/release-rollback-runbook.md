# 鍙戝竷/鍥炴粴鎵嬪唽 | Release / Rollback Runbook

鏈枃妗ｆ弿杩板彂甯冩祦绋嬪拰鍥炴粴鎿嶄綔姝ラ銆? 
This document describes the release process and rollback procedures.

---

## 鍥炴粴瑙﹀彂鏉′欢 | Trigger Conditions

婊¤冻浠ヤ笅浠讳竴鏉′欢鏃惰Е鍙戝洖婊?| Rollback is triggered when any of the following conditions are met:

- `test:full` 缂哄皯 `API_SMOKE_TEST_OK` 鏍囪 | `test:full` missing `API_SMOKE_TEST_OK`
- 鍙椾繚鎶ょ殑 `/api/**` 璺敱閿欒鐜囬鍗?| Error rate spike on protected `/api/**` routes
- 璺ㄧ鎴锋巿鏉冨洖褰掞紙`403` 璇箟澶辨晥锛?| Cross-tenant authorization regression (`403` semantics broken)
- 鐧诲綍/浼氳瘽杩炵画鎬у洖褰?| Login/session continuity regression

---

## 鍙戝竷妫€鏌ユ竻鍗?| Release Checklist

| 姝ラ | 鍛戒护 | Step | Command |
|------|------|------|---------|
| 1 | `npm run lint` | 1 | `npm run lint` |
| 2 | `npm run build` | 2 | `npm run build` |
| 3 | `npm run test:e2e` | 3 | `npm run test:e2e` |
| 4 | `npm run test:backend` | 4 | `npm run test:backend` |
| 5 | `npm run test:full` | 5 | `npm run test:full` |
| 6 | `npm run preflight:prod` | 6 | `npm run preflight:prod` |

---

## 鍥炴粴姝ラ | Rollback Steps

1. **纭畾鐩爣绋冲畾鐗堟湰** | Identify target stable commit hash
   - 鎵惧埌鏈€鍚庝竴涓ǔ瀹氱殑 commit hash
   - Find the last stable commit hash

2. **閲嶆柊閮ㄧ讲浜х墿** | Redeploy artifacts
   - 浠庣ǔ瀹?commit 閲嶆柊閮ㄧ讲鍚庣鍜屽墠绔骇鐗?   - Redeploy backend + frontend artifacts from stable commit

3. **鎭㈠鐜閰嶇疆** | Re-apply environment config
   - 閲嶆柊搴旂敤绋冲畾鐗堟湰鐨勭幆澧冮厤缃泦
   - Re-apply stable environment config set

4. **楠岃瘉鍋ュ悍鐘舵€?* | Validate health status
   - 纭 `/api/health/ready` 杩斿洖 `UP`
   - Confirm `/api/health/ready` is `UP`

5. **杩愯鍐掔儫娴嬭瘯** | Run smoke tests
   - 鎵ц E2E 鍐掔儫娴嬭瘯鍜?API 鍐掔儫娴嬭瘯
   - Run E2E smoke and API smoke tests

6. **閫氱煡瀹屾垚** | Announce completion
   - 鍙戝竷鍥炴粴瀹屾垚閫氱煡锛屽寘鍚?requestId 鍜岄敊璇獥鍙ｆ憳瑕?   - Announce rollback completion with requestId/error window summary

---

## 鑱岃矗鍒嗗伐 | Ownership

| 瑙掕壊 | 鑱岃矗 | Role | Responsibility |
|------|------|------|----------------|
| 鍙戝竷璐熻矗浜?| 鎶€鏈礋璐ｄ汉/DevOps | Release driver | Tech Lead / DevOps |
| 楠岃瘉璐熻矗浜?| QA | Validation | QA |
| 浜嬫晠娌熼€?| 鍊肩彮璐熻矗浜?| Incident communication | On-duty owner |
