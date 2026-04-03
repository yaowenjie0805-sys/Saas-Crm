# Aster CRM

> 杞婚噺绾т紒涓氱骇 CRM 绯荤粺 路 鍏ㄦ爤鍗曚綋浠撳簱 路 寮€绠卞嵆鐢ㄧ殑澶氱鎴蜂笌瀹℃壒寮曟搸

<p align="center">
  <img src="https://img.shields.io/badge/build-passing-brightgreen?style=flat-square" alt="Build Status" />
  <img src="https://img.shields.io/badge/coverage-85%25-green?style=flat-square" alt="Coverage" />
  <img src="https://img.shields.io/badge/license-AGPL--3.0-blue?style=flat-square" alt="License" />
  <img src="https://img.shields.io/badge/version-1.0.0-orange?style=flat-square" alt="Version" />
  <img src="https://img.shields.io/badge/JDK-17%2B-red?style=flat-square" alt="JDK" />
  <img src="https://img.shields.io/badge/Node.js-18%2B-339933?style=flat-square&logo=node.js&logoColor=white" alt="Node.js" />
  <img src="https://img.shields.io/badge/MySQL-8%2B-4479A1?style=flat-square&logo=mysql&logoColor=white" alt="MySQL" />
</p>

<p align="center">
  <a href="./README.en.md">English</a> | <strong>涓枃</strong>
</p>

---

## 馃搵 鐩綍

- [馃幆 涓轰粈涔堥€夋嫨 Aster CRM](#涓轰粈涔堥€夋嫨-aster-crm)
- [鉁?鏍稿績鍔熻兘](#鏍稿績鍔熻兘)
- [馃殌 蹇€熷紑濮媇(#蹇€熷紑濮?
- [馃摝 瀹夎閫夐」](#瀹夎閫夐」)
- [馃洜锔?鎶€鏈爤](#鎶€鏈爤)
- [馃摉 鏂囨。瀵艰埅](#鏂囨。瀵艰埅)
- [馃И 娴嬭瘯](#娴嬭瘯)
- [馃悰 甯歌闂](#甯歌闂)
- [馃 璐＄尞鎸囧崡](#璐＄尞鎸囧崡)
- [馃搫 璁稿彲璇乚(#璁稿彲璇?
- [馃檹 鑷磋阿](#鑷磋阿)

---

## 馃幆 涓轰粈涔堥€夋嫨 Aster CRM

Aster CRM 鏄竴涓潰鍚戜腑灏忎紒涓氱殑寮€婧愬叏鏍?CRM 绯荤粺锛屾彁渚涗粠閿€鍞嚎绱㈠埌鍚堝悓绛捐鐨勫畬鏁翠笟鍔￠棴鐜紝鍚屾椂鍏峰鐏垫椿鐨勫绉熸埛闅旂鍜屼紒涓氱骇鏉冮檺娌荤悊鑳藉姏銆?
- 鉁?**鍏ㄦ爤鍗曚綋浠撳簱**锛氬墠鍚庣缁熶竴绠＄悊锛屼竴濂楀懡浠ゅ畬鎴愬紑鍙戙€佹瀯寤恒€佹祴璇曞叏娴佺▼
- 鉁?**澶氱鎴峰師鐢熸敮鎸?*锛氬熀浜?`X-Tenant-Id` 璇锋眰澶寸殑寮洪殧绂绘灦鏋勶紝鏀寔 SaaS 鍖栭儴缃?- 鉁?**寮€绠卞嵆鐢ㄧ殑瀹℃壒寮曟搸**锛氬唴缃彲閰嶇疆瀹℃壒娴佺▼锛屾敮鎸佸绾у鏍革紝鏃犻渶浜屾寮€鍙?- 鉁?**AI 鏅鸿兘鍒嗘瀽闆嗘垚**锛氬師鐢熷鎺?OpenAI / Claude锛屾彁渚涢攢鍞娴嬩笌鏅鸿兘鎶ヨ〃娲炲療
- 鉁?**浼佷笟鍗虫椂閫氳闆嗘垚**锛氭敮鎸佷紒涓氬井淇°€侀拤閽夈€侀涔︿笁澶у钩鍙?Webhook 閫氱煡
- 鉁?**瀹屽杽鐨?CI/CD 浣撶郴**锛欸itHub Actions 澶氶樁娈垫祦姘寸嚎锛屽唴缃€ц兘闂ㄦ帶涓庡畨鍏ㄦ壂鎻?
---

## 鉁?鏍稿績鍔熻兘

### 閿€鍞鐞?- 瀹㈡埛锛圓ccount锛夊叏鐢熷懡鍛ㄦ湡绠＄悊涓庢爣绛惧垎绫?- 閿€鍞閬擄紙Pipeline锛夊彲瑙嗗寲锛屽晢鏈猴紙Opportunity锛夐樁娈佃窡韪?- 鑱旂郴浜猴紙Contact锛夊叧绯诲浘璋变笌娌熼€氬巻鍙茶褰?- 鍚堝悓涓庢姤浠峰崟鐢熸垚銆佺増鏈鐞嗕笌鐢靛瓙绛剧讲

### 涓氬姟娴佺▼
- 璁㈠崟绠＄悊锛堝垱寤?鈫?灞ョ害 鈫?浜や粯 鈫?缁撶畻锛?- 鏀粯绠＄悊涓庡璐︽祦姘?- 澶氱骇瀹℃壒娴佺▼寮曟搸锛堝彲閰嶇疆鑺傜偣銆佹潯浠跺垎鏀級
- 浠诲姟绠＄悊涓庢棩绋嬫彁閱?
### 鏁版嵁鍒嗘瀽
- 瀹炴椂浠〃鐩橈紙ECharts 5 鍙鍖栵級
- 鑷畾涔夋姤琛ㄨ璁″櫒锛堟嫋鎷藉瓧娈点€佹潯浠惰繃婊ゃ€佽仛鍚堣绠楋級
- 鏁版嵁瀵煎叆瀵煎嚭锛圗xcel / CSV锛?- AI 鏅鸿兘鍒嗘瀽锛堥攢鍞秼鍔块娴嬨€佸紓甯告娴嬶級

### 鏉冮檺娌荤悊
- 澶氱鎴锋暟鎹殧绂伙紙Row-Level Security锛?- RBAC 瑙掕壊鏉冮檺妯″瀷
- 瀛楁绾ф暟鎹劚鏁忎笌瀹夊叏鎺у埗
- 瀹屾暣瀹¤鏃ュ織锛堟搷浣滆€呫€佹椂闂淬€佸彉鏇村唴瀹癸級

### 浼佷笟闆嗘垚
- 浼佷笟寰俊 / 閽夐拤 / 椋炰功 Webhook 閫氱煡
- OpenAI / Claude AI 鎺ュ彛瀵规帴
- 鏍囧噯 REST API + Swagger 鏂囨。
- Excel 鎵归噺瀵煎叆瀵煎嚭

### 鍔熻兘鐘舵€?
| 妯″潡 | 鐘舵€?|
|------|------|
| 瀹㈡埛 / 鑱旂郴浜?/ 鍟嗘満绠＄悊 | 鉁?Stable |
| 鍚堝悓 / 鎶ヤ环鍗?| 鉁?Stable |
| 璁㈠崟 / 鏀粯绠＄悊 | 鉁?Stable |
| 瀹℃壒娴佺▼寮曟搸 | 鉁?Stable |
| RBAC 鏉冮檺 + 澶氱鎴烽殧绂?| 鉁?Stable |
| 浠〃鐩?+ 鍩虹鎶ヨ〃 | 鉁?Stable |
| 瀹¤鏃ュ織 | 鉁?Stable |
| 鑷畾涔夋姤琛ㄨ璁″櫒 | 馃毀 Beta |
| AI 鏅鸿兘鍒嗘瀽 | 馃毀 Beta |
| 浼佷笟寰俊 / 閽夐拤 / 椋炰功闆嗘垚 | 馃毀 Beta |
| 绉诲姩绔€傞厤 | 馃搵 Planned |
| 宸ヤ綔娴佸彲瑙嗗寲缂栬緫鍣?| 馃搵 Planned |

---

## 馃殌 蹇€熷紑濮?
### 鐜瑕佹眰

| 渚濊禆 | 鐗堟湰 | 璇存槑 |
|------|------|------|
| JDK | 17+ | 鍚庣杩愯鏃?|
| Node.js | 18+ | 鍓嶇鏋勫缓涓庤繍琛?|
| MySQL | 8.0+ | 涓绘暟鎹簱锛堝繀闇€锛?|
| Redis | 6+ | 鏈湴缂撳瓨锛堟帹鑽愶級 |
| RabbitMQ | 3.11+ | 娑堟伅闃熷垪锛堟帹鑽愶級 |

### 5 鍒嗛挓鍚姩

**绗竴姝ワ細鍏嬮殕浠撳簱**

```bash
git clone https://github.com/your-repo/aster-crm.git
cd aster-crm
```

**绗簩姝ワ細閰嶇疆鐜鍙橀噺**

```bash
cp .env.example .env
cp .env.backend.local.example .env.backend.local
```

缂栬緫 `.env`锛屽～鍐欐暟鎹簱杩炴帴淇℃伅锛?
```env
DB_URL=jdbc:mysql://localhost:3306/aster_crm?useUnicode=true&characterEncoding=utf8
DB_USER=root
DB_PASSWORD=your_password
```

缂栬緫 `.env.backend.local`锛屼慨鏀?Token 瀵嗛挜锛?*鐢熶骇鐜蹇呴』淇敼**锛夛細

```env
AUTH_TOKEN_SECRET=your-secret-key-at-least-32-chars
```

**绗笁姝ワ細鍒濆鍖栨暟鎹簱**

```bash
npm run db:init
```

**绗洓姝ワ細鍚姩鍚庣**

```bash
npm run dev:backend
```

**绗簲姝ワ細瀹夎渚濊禆骞跺惎鍔ㄥ墠绔?*

```bash
npm install
npm run dev
```

### 榛樿鐧诲綍淇℃伅

| 椤圭洰 | 鍊?|
|------|-----|
| 璁块棶鍦板潃 | http://localhost:5173 |
| 鐢ㄦ埛鍚?| `admin` |
| 瀵嗙爜 | `admin123` |
| API 鍦板潃 | http://localhost:8080/api |
| Swagger | http://localhost:8080/swagger-ui.html |

> 鈿狅笍 `admin123` 浠呯敤浜庢湰鍦板紑鍙戙€?*鐢熶骇鐜璇烽€氳繃 `AUTH_BOOTSTRAP_DEFAULT_PASSWORD` 璁剧疆寮哄瘑鐮併€?*

---

## 馃摝 瀹夎閫夐」

### 馃惓 Docker Compose 涓€閿儴缃诧紙鎺ㄨ崘锛?
**鐢熶骇鐜锛?*

```bash
cp .env.production.example .env.production
# 缂栬緫 .env.production 濉啓鐢熶骇閰嶇疆
docker compose --env-file .env.production -f infra/production/docker-compose.yml up -d
```

**棰勫彂甯冿紙Staging锛夌幆澧冿細**

```bash
cd infra/staging
cp staging.env.example staging.env
# 缂栬緫 staging.env
docker compose -f docker-compose.yml --env-file staging.env up -d
```

### 馃捇 鏈湴寮€鍙戠幆澧?
鍙傝€?[馃殌 蹇€熷紑濮媇(#蹇€熷紑濮? 绔犺妭锛岄€愭瀹屾垚鏈湴鍚姩銆?
寮€鍙戝父鐢ㄥ懡浠わ細

```bash
npm run dev:backend      # 鍚姩鍚庣锛堢儹閲嶈浇锛?npm run dev              # 鍚姩鍓嶇锛圴ite HMR锛?npm run lint             # ESLint 浠ｇ爜妫€鏌?npm run test:frontend    # 鍓嶇鍗曞厓娴嬭瘯
npm run test:e2e         # E2E 绔埌绔祴璇?```

### 馃彮 鐢熶骇鐜鎵嬪姩閮ㄧ讲

```bash
# 鏋勫缓鍚庣 JAR
npm run build:backend

# 鏋勫缓鍓嶇闈欐€佹枃浠?npm run build

# 鍚庣 JAR 浣嶄簬 apps/api/target/
# 鍓嶇浜х墿浣嶄簬 apps/web/dist/
```

璇︾粏閮ㄧ讲娴佺▼璇峰弬鑰?[docs/operations/staging-deploy-runbook.md](./docs/operations/staging-deploy-runbook.md)銆?
---

## 馃洜锔?鎶€鏈爤

### 鍓嶇

| 鎶€鏈?| 鐗堟湰 | 鐢ㄩ€?|
|------|------|------|
| React | 19 | UI 妗嗘灦 |
| Vite | 7 | 鏋勫缓宸ュ叿 |
| Ant Design | 5 | 缁勪欢搴?|
| Tailwind CSS | 4 | 鍘熷瓙鍖?CSS |
| Zustand | 5 | 鐘舵€佺鐞?|
| ECharts | 5 | 鏁版嵁鍙鍖?|
| Vitest | latest | 鍗曞厓娴嬭瘯 |
| Playwright | latest | E2E 娴嬭瘯 |

### 鍚庣

| 鎶€鏈?| 鐗堟湰 | 鐢ㄩ€?|
|------|------|------|
| Spring Boot | 2.7 | 搴旂敤妗嗘灦 |
| JDK | 17 | 杩愯鏃?|
| Spring Data JPA | - | ORM 鏁版嵁璁块棶 |
| Spring Security | - | 璁よ瘉涓庢巿鏉?|
| Flyway | 9 | 鏁版嵁搴撶増鏈鐞?|
| MySQL | 8+ | 涓绘暟鎹簱 |
| Redis / Caffeine | - | 鏈湴 + 鍒嗗竷寮忕紦瀛?|
| RabbitMQ | - | 寮傛娑堟伅闃熷垪 |

### 鍩虹璁炬柦

| 鎶€鏈?| 鐢ㄩ€?|
|------|------|
| Docker Compose | 瀹瑰櫒鍖栭儴缃?|
| Nginx | 鍙嶅悜浠ｇ悊 / 闈欐€佹枃浠舵湇鍔?|
| GitHub Actions | CI/CD 娴佹按绾?|

瀹屾暣鏋舵瀯渚濊禆鍥捐鍙傝€?[docs/ARCH_RUNTIME_DEPENDENCY_MAP.md](./docs/ARCH_RUNTIME_DEPENDENCY_MAP.md)銆?
---

## 馃摉 鏂囨。瀵艰埅

| 鏂囨。 | 璇存槑 |
|------|------|
| [docs/PROJECT_SPECIFICATION.md](./docs/PROJECT_SPECIFICATION.md) | 椤圭洰瑙勮寖涓庤璁″師鍒?|
| [docs/PROJECT_STRUCTURE.md](./docs/PROJECT_STRUCTURE.md) | 鐩綍缁撴瀯璇存槑 |
| [docs/DEVELOPMENT_CONVENTIONS.md](./docs/DEVELOPMENT_CONVENTIONS.md) | 寮€鍙戠害瀹氫笌缂栫爜瑙勮寖 |
| [docs/api-documentation.md](./docs/api-documentation.md) | API 鎺ュ彛鏂囨。 |
| [docs/ARCH_RUNTIME_DEPENDENCY_MAP.md](./docs/ARCH_RUNTIME_DEPENDENCY_MAP.md) | 鏋舵瀯杩愯鏃朵緷璧栧浘 |
| [docs/PROJECT_TROUBLESHOOTING.md](./docs/PROJECT_TROUBLESHOOTING.md) | 甯歌闂鎺掓煡鎵嬪唽 |
| [docs/operations/environment-matrix.md](./docs/operations/environment-matrix.md) | 澶氱幆澧冮厤缃煩闃?|
| [docs/operations/release-strategy.md](./docs/operations/release-strategy.md) | 鍙戝竷绛栫暐涓庢祦绋?|

---

## 馃И 娴嬭瘯

Aster CRM 閲囩敤涓夊眰娴嬭瘯绛栫暐锛岀‘淇濇牳蹇冧笟鍔￠€昏緫鐨勫彲闈犳€э細

```bash
# 鍓嶇鍗曞厓娴嬭瘯锛圴itest锛?npm run test:frontend

# 鍚庣鍗曞厓娴嬭瘯锛圝Unit 5锛?npm run test:backend

# E2E 绔埌绔祴璇曪紙Playwright锛?npm run test:e2e

# 鏌ョ湅鍓嶇娴嬭瘯瑕嗙洊鐜囨姤鍛?# 鎶ュ憡鐢熸垚浜?apps/web/coverage/
```

| 娴嬭瘯绫诲瀷 | 宸ュ叿 | 瑕嗙洊鑼冨洿 |
|----------|------|----------|
| 鍓嶇鍗曞厓娴嬭瘯 | Vitest | 缁勪欢銆佸伐鍏峰嚱鏁般€佺姸鎬佺鐞?|
| 鍚庣鍗曞厓娴嬭瘯 | JUnit 5 + Mockito | Service 灞傘€丷epository 灞?|
| E2E 娴嬭瘯 | Playwright | 鏍稿績鐢ㄦ埛娴佺▼锛堢櫥褰曘€丆RUD銆佸鎵癸級 |

---

## 馃悰 甯歌闂

### 鍚姩鍚庣櫥褰曡繑鍥?500 / `seed_tenant_id_required`

**鍘熷洜锛?* `APP_SEED_ENABLED=true` 浣?`APP_SEED_TENANT_ID` 涓虹┖銆?
**瑙ｅ喅锛?* 鍦?`.env.backend.local` 涓坊鍔狅細

```env
APP_SEED_ENABLED=true
APP_SEED_TENANT_ID=tenant_default
APP_SEED_DEMO_ENABLED=true
```

### 鐧诲綍濮嬬粓澶辫触 / Token 楠岃瘉閿欒

**鍘熷洜锛?* `AUTH_TOKEN_SECRET` 浣跨敤浜嗙ず渚嬮粯璁ゅ€兼垨闀垮害涓嶈冻 32 浣嶃€?
**瑙ｅ喅锛?* 鍦?`.env.backend.local` 涓缃冻澶熷己搴︾殑瀵嗛挜锛?
```env
AUTH_TOKEN_SECRET=my-super-secret-key-at-least-32-chars
```

### 鍚庣鍚姩鏃跺嚭鐜?Redis 鐩稿叧 WARNING

**鍘熷洜锛?* 鏈惎鐢?Redis Repository锛屽睘浜庢甯镐俊鎭棩蹇椼€?
**瑙ｅ喅锛?* 鏈湴寮€鍙戝彲瀹夊叏蹇界暐銆傝嫢闇€瀹屾暣 Redis 鏀寔锛岃閰嶇疆 `REDIS_HOST` 鍜?`REDIS_PORT`銆?
### AI 鍔熻兘鏃犲搷搴?/ 鎶ラ敊

**鍘熷洜锛?* 鏈厤缃?AI 鏈嶅姟 API Key銆?
**瑙ｅ喅锛?* 鍦?`.env.backend.local` 涓厤缃搴旀湇鍔″瘑閽ワ細

```env
AI_OPENAI_API_KEY=sk-...
# 鎴?AI_ANTHROPIC_API_KEY=sk-ant-...
```

鏇村鎺掓煡姝ラ璇峰弬鑰?[docs/PROJECT_TROUBLESHOOTING.md](./docs/PROJECT_TROUBLESHOOTING.md)銆?
---

## 馃 璐＄尞鎸囧崡

娆㈣繋鎻愪氦 Issue 鍜?Pull Request锛?
### 璐＄尞娴佺▼

```
1. Fork 鏈粨搴?2. 鍩轰簬 main 鍒涘缓鐗规€у垎鏀細git checkout -b feat/your-feature
3. 鎻愪氦鍙樻洿锛堥伒寰?Conventional Commits锛夛細git commit -m "feat: add awesome feature"
4. 鎺ㄩ€佸垎鏀細git push origin feat/your-feature
5. 鎻愪氦 Pull Request锛岀瓑寰?CI 閫氳繃鍚庤姹?Review
```

### 寮€鍙戣鑼?
- 浠ｇ爜椋庢牸璇烽伒寰?[寮€鍙戠害瀹氭枃妗(./docs/DEVELOPMENT_CONVENTIONS.md)
- 鏂板姛鑳介渶闄勫甫鍗曞厓娴嬭瘯锛岃鐩栨牳蹇冨垎鏀?- PR 鎻忚堪闇€璇存槑鍙樻洿鍔ㄦ満涓庡奖鍝嶈寖鍥?- 瀵绘壘 `good first issue` 鏍囩鐨?Issue 鍏ユ墜璐＄尞

### 鎻愪氦娑堟伅鏍煎紡

```
feat:     鏂板姛鑳?fix:      Bug 淇
docs:     鏂囨。鏇存柊
refactor: 浠ｇ爜閲嶆瀯锛堜笉鍚姛鑳藉彉鏇达級
test:     娴嬭瘯鐩稿叧
chore:    鏋勫缓/CI/渚濊禆绛夋潅椤?```

---

## 馃搫 璁稿彲璇?
鏈」鐩熀浜?**[AGPL-3.0](./LICENSE)** 璁稿彲璇佸紑婧愩€?
- 鉁?鍏佽涓汉瀛︿範銆佺爺绌朵笌闈炲晢涓氫娇鐢?- 鉁?淇敼鍚庣殑浠ｇ爜蹇呴』浠ョ浉鍚岃鍙瘉寮€婧?- 鈿狅笍 鍟嗕笟浣跨敤璇风‘淇濋伒瀹?AGPL-3.0 鐨勭綉缁滄湇鍔℃潯娆撅紝鎴栬仈绯讳綔鑰呰幏鍙栧晢涓氭巿鏉?
---

## 馃檹 鑷磋阿

鎰熻阿浠ヤ笅浼樼寮€婧愰」鐩负 Aster CRM 鎻愪緵鐨勫熀纭€鏀寔锛?
- [Spring Boot](https://spring.io/projects/spring-boot) - 鍚庣搴旂敤妗嗘灦
- [React](https://react.dev/) - 鍓嶇 UI 妗嗘灦
- [Ant Design](https://ant.design/) - 浼佷笟绾?UI 缁勪欢搴?- [Vite](https://vitejs.dev/) - 鏋侀€熷墠绔瀯寤哄伐鍏?- [Flyway](https://flywaydb.org/) - 鏁版嵁搴撶増鏈縼绉荤鐞?- [ECharts](https://echarts.apache.org/) - 寮€婧愬彲瑙嗗寲鍥捐〃搴?- [Playwright](https://playwright.dev/) - 鍙潬鐨勭鍒扮娴嬭瘯妗嗘灦

---

<p align="center">
  濡傛灉杩欎釜椤圭洰瀵逛綘鏈夊府鍔╋紝璇风粰鎴戜滑涓€涓?猸?Star锛?</p>

---

## AI 鍔熻兘鍏ュ彛涓庡洖褰掓祴璇曪紙2026-04 鏇存柊锛?
- 鍓嶇鍏ュ彛锛氶《閮ㄦ搷浣滄爮 `AI鍔熻兘` 鎸夐挳锛坄data-testid="topbar-ai-shortcut"`锛?- 璺宠浆琛屼负锛氱偣鍑诲悗鑷姩鍒囧埌 Dashboard锛屽苟瀹氫綅鍒?`AI Follow-up Summary` 鍖哄潡
- 鏍稿績瀹炵幇锛?  - `apps/web/src/crm/components/layout/TopBar.jsx`
  - `apps/web/src/crm/components/MainContent.jsx`
  - `apps/web/src/crm/components/pages/dashboard/AiFollowUpSummarySection.jsx`

鎺ㄨ崘鍥炲綊鍛戒护锛?
```bash
# 鍗曟祴锛歍opBar AI 鍏ュ彛鎸夐挳
npm run test --workspace apps/web -- --run src/crm/__tests__/TopBar.aiShortcut.test.jsx

# 鍗曟祴锛歁ainContent 璺宠浆閾捐矾锛圓I 鎸夐挳 -> Dashboard -> AI 闈㈡澘锛?npm run test --workspace apps/web -- --run src/crm/__tests__/MainContent.aiShortcutBridge.test.jsx

# E2E锛氱湡瀹炴祻瑙堝櫒閾捐矾 smoke
npm run test:e2e:ai-shortcut
```
