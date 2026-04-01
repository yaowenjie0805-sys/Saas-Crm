# CRM

## Seed Tenant Config (ASCII Safe)

Backend seed tenant settings:

- Property key: `app.seed.tenant-id`
- Env key: `APP_SEED_TENANT_ID`
- Related flags: `APP_SEED_ENABLED`, `APP_SEED_DEMO_ENABLED`

Recommended local example:

```env
APP_SEED_ENABLED=true
APP_SEED_TENANT_ID=tenant_local
APP_SEED_DEMO_ENABLED=true
```

Behavior:

- If `APP_SEED_ENABLED=true`, set a non-empty `APP_SEED_TENANT_ID`.
- Set `APP_SEED_TENANT_ID` explicitly in local/prod env files to avoid drift.
- Profile fallback defaults: `dev/main=tenant_local`, `test=tenant_test`, `prod=<empty>`.

## Seed Tenant Deployment Checklist

- Required rule: when `APP_SEED_ENABLED=true`, `APP_SEED_TENANT_ID` must be set and non-empty.
- Recommended `dev`:
  `APP_SEED_ENABLED=true`, `APP_SEED_TENANT_ID=tenant_local`, `APP_SEED_DEMO_ENABLED=true`
- Recommended `test`:
  `APP_SEED_ENABLED=true`, `APP_SEED_TENANT_ID=tenant_test`, `APP_SEED_DEMO_ENABLED=false`
- Recommended `prod`:
  `APP_SEED_ENABLED=false`, `APP_SEED_TENANT_ID=` (keep empty unless you explicitly run seed)
- Common failure symptom:
  login `500` or startup seed fail-fast (`seed_tenant_id_required`) when seed is enabled but tenant id is blank.

## 1. 妞ゅ湱娲伴悳鎵Ц閿涘牅缍橀崣顖欎簰閸忓牏婀呮潻娆庨嚋閿?

- 閺嬭埖鐎敍姝歊eact + Vite`閿涘牆澧犵粩顖ょ礆+ `Spring Boot 2.7`閿涘牆鎮楃粩顖ょ礉JDK 17閿? `MySQL + Flyway`閵?
- 鏉╂劘顢戝Ο鈥崇础閿涙碍婀伴崷浼寸帛鐠?`dev`閿涘本鏆熼幑顔肩氨姒涙顓?`crm_local`閵?
- 婢舵氨顫ら幋鍑ょ窗閺嶇绺鹃幒銉ュ經閹?`X-Tenant-Id` 閸嬫氨顫ら幋鐑芥缁傛眹鈧?
- 闂嗗棙鍨氶懗钘夊閿涙碍鏁幐浣风磼娑撴艾浜曟穱?闁藉鎷?webhook閿涙盯顥ｆ稊锔芥暜閹?webhook 閸?App ID/App Secret 閻╃绻涢敍鍧眅nant_access_token + 濞戝牊浼呴幒銉ュ經閿涘鈧?
- 鏉╀胶些閻樿埖鈧緤绱拌ぐ鎾冲 migration 閻楀牊婀伴崚?`V20`閵?

## 2. 閻╊喖缍嶇紒鎾寸€?

```text
crm/
  apps/
    api/                      # Spring Boot 閸氬海顏?
      src/main/java/com/yao/crm/
        controller/           # REST 閹貉冨煑閸?
        service/              # 娑撴艾濮熼張宥呭
        entity/               # JPA 鐎圭偘缍?
        repository/           # 閺佺増宓佺拋鍧楁６
        config/               # Spring 闁板秶鐤?
        security/             # 闁村瓨娼堟稉搴＄暔閸忋劎娴夐崗?
        exception/            # 娑撴艾濮熷鍌氱埗娴ｆ挾閮撮敍鍦攗sinessException閵嗕笒rrorCode閿?
        enums/                # 鐢悂鍣洪弸姘閿涘牏濮搁幀浣虹垳閵嗕胶琚崹瀣亣娑撴拝绱?
        event/                # 妫板棗鐓欐禍瀣╂閿涘牅绨ㄦ禒璺哄絺鐢啩绗岄惄鎴濇儔閿?
      src/main/resources/
        db/migration/         # Flyway 閼存碍婀?V1~V20
    web/                      # React 閸撳秶顏?
      src/crm/components/     # 妞ょ敻娼版稉搴濈瑹閸旓紕绮嶆禒?
  scripts/                    # 閸氼垰濮╅妴涓廈閵嗕椒鎱ㄦ径宥冣偓浣歌窗濡偓閼存碍婀?
  docs/                       # 鏉╂劗娣稉搴笉閻炲棙鏋冨?
```

## 3. 閺嶇绺鹃崝鐔诲厴濡€虫健

### 3.1 娑撴艾濮熷Ο鈥虫健

- 鐎广垺鍩涙稉搴や粓缁姹夐敍姘吂閹存灚鈧浇浠堢化璁虫眽閵嗕浇绐℃潻娑栤偓浣锋崲閸?
- 闁库偓閸烆喗绁︾粙瀣剁窗缁捐法鍌ㄩ妴浣告櫌閺堟亽鈧焦濮ゆ禒鏋偓浣筋吂閸楁洏鈧礁娲栧▎淇扁偓浣告値閸?
- 鐎光剝澹掓稉顓炵妇閿涙碍膩閺夎￥鈧礁鐤勬笟瀣ㄢ偓浣锋崲閸斅扳偓浣芥祮鐎孤扳偓浣稿亾閸旂偑鈧讣LA
- 閸楀繋缍旀稉搴㈡綀闂勬劧绱扮拠鍕啈閵嗕焦妞块崝銊ュ彙娴滎偁鈧礁鐡у▓鍨綀闂勬劑鈧礁娲熼梼?
- 閹躲儴銆冩稉搴″瀻閺嬫劧绱版禒顏囥€冮惄妯糕偓浣瑰Г鐞涖劍膩閺夎￥鈧礁顕遍崙杞版崲閸?
- 瀹搞儰缍斿ù渚婄窗閼哄倻鍋ｉ妴浣瑰⒔鐞涘被鈧浇鐨熸惔锔衡偓渚€鈧氨鐓?
- 鐎电厧鍙嗙€电厧鍤敍姘闁插繐顕遍崗銉ｂ偓浣搞亼鐠愩儵鍣哥拠鏇樷偓浣割嚤閸戣桨缍旀稉?

### 3.2 閸忕鐎烽幒銉ュ經閸撳秶绱?

- 閸╄櫣顢呴幒銉ュ經閿涙瓪/api/**`
- 缁夌喐鍩涙稉姘閹恒儱褰涢敍姝?api/v1/**`
- 闁板秶鐤?濞岃崵鎮婇幒銉ュ經閿涙瓪/api/v2/**`

鐢瓕顫嗛幒褍鍩楅崳銊ュ讲閸︺劎娲拌ぐ鏇熺叀閻绱?
`apps/api/src/main/java/com/yao/crm/controller`

## 4. 閹垛偓閺堫垱鐖?

- 閸撳秶顏敍姝奺act 19閵嗕箓ite閵嗕阜eact Router閵嗕箲ustand
- 閸氬海顏敍姝媝ring Boot 2.7閵嗕讣pring Data JPA閵嗕笚lyway閵嗕阜edis閵嗕竼affeine閿涘牊婀伴崷鎵处鐎涙﹫绱氶妴涓穉bbitMQ
- API 閺傚洦銆傞敍姝磒ringdoc-openapi閿涘湯wagger UI閿?
- 閺佺増宓佹惔鎿勭窗MySQL 8+
- 濞村鐦敍娆経nit閿涘牆鎮楃粩顖ょ礆閵嗕赋laywright閿涘牆澧犵粩?E2E閿?

## 5. 閺堫剙婀村鈧崣鎴濇彥闁喎绱戞慨?

### 5.1 閻滎垰顣ㄧ憰浣圭湴 | Environment Requirements

| 瀹搞儱鍙?Tool | 閻楀牊婀?Version | 鐠囧瓨妲?Note |
|-----------|-------------|----------|
| JDK | 17+ | Java 瀵偓閸欐垹骞嗘晶?|
| Node.js | 18+ | 閸撳秶顏潻鎰攽閻滎垰顣?|
| MySQL | 8.0+ | 娑撶粯鏆熼幑顔肩氨 |
| Redis | 6.0+ | 缂傛挸鐡ㄩ敍鍫濆讲闁绱濇妯款吇娴ｈ法鏁ら張顒€婀寸紓鎾崇摠閿?|
| Maven | 3.6+ | Java 閺嬪嫬缂撳銉ュ徔 |

### 5.2 娑撯偓闁款喖鎯庨崝銊﹀瘹閸?| Quick Start Guide

> 婢跺秴鍩楁禒銉ょ瑓閸涙垝鎶ら敍灞惧瘻妞ゅ搫绨幍褑顢戦崡鍐插讲閸氼垰濮╂い鍦窗

```bash
# 1. 閸忓娈曟い鍦窗 | Clone the project
git clone https://github.com/yaowenjie0805-sys/Saas-Crm.git
cd Saas-Crm

# 2. 闁板秶鐤嗛悳顖氼暔閸欐﹢鍣?| Configure environment
# Windows:
copy .env.example .env
copy .env.backend.local.example .env.backend.local
# Linux/Mac:
cp .env.example .env
cp .env.backend.local.example .env.backend.local
# 缂傛牞绶?.env 閸?.env.backend.local 婵夘偄鍙嗘担鐘垫畱閺佺増宓佹惔鎾崇槕閻胶鐡戦柊宥囩枂

# 3. 閸掓繂顫愰崠鏍ㄦ殶閹诡喖绨?| Initialize database
# Windows:
npm run db:init
# Linux/Mac:
bash scripts/init-db.sh root root crm_local

# 4. 閸氼垰濮╅崥搴ｎ伂 | Start backend
npm run dev:backend
# 閹存牞鈧懍濞囬悽銊︽纯缁嬪啿鐣鹃惃鍕壖閺?
# npm run dev:backend:stable

# 5. 閸氼垰濮╅崜宥囶伂閿涘牆褰熷鈧稉鈧稉顏嗙矒缁旑垽绱殀 Start frontend (new terminal)
npm install
npm run dev

# 6. 鐠佸潡妫?| Access
# 閸撳秶顏?Frontend: http://localhost:5173
# 閸氬海顏?API: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### 5.3 姒涙顓荤拋鍧楁６閸︽澘娼?| Default URLs

| 閺堝秴濮?Service | 閸︽澘娼?URL |
|-------------|---------|
| 閸撳秶顏?Frontend | `http://localhost:5173` |
| 閸氬海顏?Backend | `http://localhost:8080` |
| 閸嬨儱鎮嶅Λ鈧弻?Health Check | `http://localhost:8080/api/health` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/api-docs` |

## 6. 閺佺増宓佹惔鎾圭讣缁夎绗屾穱顔碱槻

### 6.1 濮濓絽鐖舵潻浣盒?

閸氬海顏崥顖氬З閺冩湹绱伴懛顏勫З Flyway migrate閿涘潉validate-on-migrate=true`閿涘鈧?

### 6.2 婵″倹鐏夐柆鍥у煂 checksum mismatch / failed migration

娴ｈ法鏁ゆ禒鎾崇氨閼存碍婀伴崑姘ｂ偓婊冨帥婢跺洣鍞ら崘宥勬叏婢跺秮鈧繐绱?

```bash
npm run db:flyway:repair
```

鐠囥儴鍓奸張顑跨窗閿?

1. 婢跺洣鍞よぐ鎾冲閺佺増宓佹惔鎾冲煂 `backups/`
2. 閹笛嗩攽 `flyway repair`
3. 閹笛嗩攽 `flyway migrate`
4. 鏉堟挸鍤張鈧弬鎵閺堫兛绗屾径杈Е鐠佹澘缍嶉弫?

## 7. 闂嗗棙鍨氶柊宥囩枂閿涘牓鍣搁悙鐧哥礆

### 7.1 闁板秶鐤嗛弬鍥︽閸旂姾娴囩憴鍕灟

閸氬海顏崨鎴掓姢娴兼俺鍤滈崝銊嚢閸欐牗鐗撮惄顔肩秿 `.env.backend.local`閿涘牐瀚㈢€涙ê婀敍澶堚偓?
- 鐠囪褰囬柅鏄忕帆閸︻煉绱癭scripts/run-maven.mjs`
- 濡剝婢橀弬鍥︽閿涙瓪.env.backend.local.example`

鐠併倛鐦夌粵鎯ф倳鐎靛棝鎸滈崝鈥崇箑閺勬儳绱￠柊宥囩枂閿?
```env
AUTH_TOKEN_SECRET=replace-with-a-long-random-secret
APP_SEED_TENANT_ID=tenant_local
```
`APP_SEED_TENANT_ID` is required when `APP_SEED_ENABLED=true`.
鐠囧瓨妲戦敍?- `AUTH_TOKEN_SECRET` 韫囧懎锝為敍灞肩瑝閼崇晫鏆€缁岀尨绱欓崠鍛儓閸忋劎鈹栭惂钘夌摟缁楋讣绱氭稉鏂剧瑝閼虫垝濞囬悽銊╃帛鐠併倕鈧鈧?- 娴犵粯鍓伴悳顖氼暔娑撳瀚㈢紓鍝勩亼/缁岃櫣娅ф导姘躬閸氼垰濮╅弮鍓佹纯閹恒儱銇戠拹銉幢閻㈢喍楠囬悳顖氼暔娴ｈ法鏁ゆ妯款吇閸婄厧鎮撻弽铚傜窗婢惰精瑙﹂敍鍫ユ姜閻㈢喍楠囨妯款吇閸婇棿绱伴崨濠咁劅閿涘鈧?- 閺堫亝顒滅涵顕€鍘ょ純顔煎讲閼冲€熜曢崣鎴犳瑜?500 閹?token 缁涙儳鎮曢弽锟犵崣婢惰精瑙﹂妴?
### 7.2 妞嬬偘鍔熼敍鍦損p 濡€崇础閿涘本甯归懡鎰剁礆

閸?`.env.backend.local` 闁板秶鐤嗛敍?

```env
INTEGRATION_WEBHOOK_PROVIDERS=WECOM,DINGTALK,FEISHU
INTEGRATION_FEISHU_APP_ID=cli_xxx
INTEGRATION_FEISHU_APP_SECRET=xxx
INTEGRATION_FEISHU_RECEIVE_ID=oc_xxx
INTEGRATION_FEISHU_RECEIVE_ID_TYPE=chat_id
INTEGRATION_FEISHU_BASE_URL=https://open.feishu.cn
```

鐠囧瓨妲戦敍?

- `RECEIVE_ID_TYPE` 閺€顖涘瘮閿涙瓪chat_id` / `open_id` / `user_id` / `union_id`
- 妞嬬偘鍔熸导姘喘閸忓牐铔?App API 閻╃绻涢敍灞炬弓闁板秶鐤嗛弮鑸靛閸ョ偤鈧偓 webhook閵?

### 7.3 娴间椒绗熷顔讳繆/闁藉鎷?webhook閿涘牆褰查柅澶涚礆

```env
INTEGRATION_WECOM_WEBHOOK_URL=
INTEGRATION_DINGTALK_WEBHOOK_URL=
INTEGRATION_DINGTALK_SECRET=
```

### 7.4 閻喐婧€鏉╃偤鈧碍鈧勭ゴ鐠?

```bash
powershell -ExecutionPolicy Bypass -File scripts/test-webhooks.ps1
```

閼存碍婀版导姘倱閺冭埖顥呴弻銉窗

- WECOM webhook
- DINGTALK webhook
- FEISHU webhook
- FEISHU App 濡€崇础

### 7.5 AI 閸旂喕鍏樺鈧崗鍏呯瑢闁板秶鐤?
閸氬海顏?AI 闁板秶鐤嗛崥灞剧壉閺夈儴鍤?`.env.backend.local`閿涘苯鑻熼柅姘崇箖 `application.properties` 閺勭姴鐨犻崚?`ai.openai.*` / `ai.anthropic.*`閵?閹恒劏宕樻导妯哄帥闁板秶鐤?OpenAI閿?
```env
AI_OPENAI_API_KEY=sk-xxx
AI_OPENAI_BASE_URL=https://api.openai.com
AI_OPENAI_MODEL=gpt-4o
```

Anthropic閿涘牆褰查柅澶涚礆閿?
```env
AI_ANTHROPIC_API_KEY=
AI_ANTHROPIC_MODEL=claude-3-5-sonnet
```

鐠囧瓨妲戦敍?- 瀵偓閸忓磭鐡ラ悾銉窗姒涙顓荤€瑰鍙忛敍灞炬弓闁板秶鐤?API Key 閺冩儼顫嬫稉鍝勫彠闂?AI 鐠嬪啰鏁ら妴?- 閹恒儱褰涢悽銊┾偓鏃撶窗閻劋绨?CRM 閻?AI 婢х偛宸遍懗钘夊閿涘牆顩х痪璺ㄥ偍鐠愩劑鍣虹拠鍕強閵嗕線鏀㈤崬顔肩紦鐠侇喓鈧礁鍞寸€瑰湱鏁撻幋鎰搼閿涘鈧?- 閺堫亪鍘ょ純顔款攽娑撶尨绱扮化鑽ょ埠娴兼艾娲栭柅鈧崚鏉垮敶缂冾喖鍘规惔鏇⑩偓鏄忕帆閹存牞绻戦崶鐐┾偓娣嶪 閺堝秴濮熼張顏堝帳缂冾噯绱濇稉宥呭讲閻劉鈧繃褰佺粈鐚寸礉娑撳秴濂栭崫宥呯唨绾偓娑撴艾濮熼柧鎹愮熅閵?
### 7.6 Tenant Missing Guard (tenant.reject-missing)

- Config key: tenant.reject-missing (env: TENANT_REJECT_MISSING).
- Behavior: when true, requests missing tenant context are rejected.
- Recommended defaults: dev/test=false, prod=true.

## 8. 鐢摜鏁ら崨鎴掓姢

```bash
# 閸撳秶顏弸鍕紦
npm run build

# 閸氬海顏ù瀣槸
npm run test:backend

# 閸撳秶顏?E2E
npm run test:e2e

# 閸忋劑鎽肩捄顖涙拱閸︿即鐛欑拠?
npm run test:full

# 閻滎垰顣ㄩ弽锟犵崣
npm run validate:env
```

### 8.1 Security scan

- `npm run security:scan`: run repo security scan (Node/web checks).
- `npm run security:scan:java`: run Java security scan for backend.
- `SECURITY_SCAN_JAVA_ENABLED`: optional Java scan toggle. If false, Java scan is skipped.
- `SECURITY_SCAN_JAVA_REQUIRED`: gate behavior toggle. If true, Java scan failure blocks the job.
- CI policy now: PR = closed (no gate), `main` = soft gate (warn only), `staging` = hard gate (must pass).

## 9. 鐢瓕顫嗛梻顕€顣?| FAQ

### 9.1 閸氼垰濮╅弮鑸靛Г Flyway 閺嶏繝鐛欐径杈Е | Flyway Checksum Mismatch

閸忓牊澧界悰宀嬬窗`npm run db:flyway:repair`閵?

### 9.2 閺佺増宓佹惔鎾圭箾閹恒儱銇戠拹銉︹偓搴濈疄閸旂儑绱祙 Database Connection Failed

濡偓閺屻儰浜掓稉瀣殤閻愮櫢绱?
1. MySQL 閺堝秴濮熼弰顖氭儊閸氼垰濮╅敍姝歮ysql -u root -p`
2. 閺佺増宓佹惔鎾存Ц閸氾箑鐡ㄩ崷顭掔窗`SHOW DATABASES LIKE 'crm_local';`
3. `.env` 閺傚洣娆㈡稉顓犳畱 `DB_URL`閵嗕梗DB_USER`閵嗕梗DB_PASSWORD` 閺勵垰鎯佸锝団€?
4. MySQL 鏉╃偞甯撮弫鐗堟Ц閸氾箑鍑″鈽呯窗`SHOW STATUS LIKE 'Threads_connected';`

### 9.3 缁旑垰褰涚悮顐㈠窗閻劍鈧簼绠為崝鐑囩吹| Port Already in Use

Windows:
```powershell
# 閺屻儳婀?8080 缁旑垰褰涢崡鐘垫暏
netstat -ano | findstr :8080
# 缂佹挻娼潻娑氣柤閿涘湧ID 娑撹桨绗傛稉鈧銉︾叀閸掓壆娈戞潻娑氣柤ID閿?
taskkill /PID <鏉╂稓鈻糏D> /F
```

Linux/Mac:
```bash
# 閺屻儳婀?8080 缁旑垰褰涢崡鐘垫暏
lsof -i :8080
# 缂佹挻娼潻娑氣柤
kill -9 <PID>
```

### 9.4 Maven 娑撳娴囬幈銏♀偓搴濈疄闁板秶鐤嗛梹婊冨剼閿涚劜 Maven Mirror Configuration

缂傛牞绶?Maven 闁板秶鐤嗛弬鍥︽閿涘湹indows: `~/.m2/settings.xml`閿涘inux/Mac: `~/.m2/settings.xml`閿涘绱?

```xml
<mirrors>
  <mirror>
    <id>aliyun</id>
    <mirrorOf>central</mirrorOf>
    <name>Aliyun Maven Mirror</name>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

### 9.5 閸撳秶顏崥顖氬З閹躲儵鏁?Node 閻楀牊婀版稉宥咁嚠閿涚劜 Node Version Mismatch

妞ゅ湱娲扮憰浣圭湴 Node.js 18+閿涘本甯归懡鎰▏閻?nvm 缁狅紕鎮婃径姘卞閺堫剨绱?

```bash
# 鐎瑰顥?nvm (Windows 閻劍鍩涙稉瀣祰 nvm-windows)
# Windows: https://github.com/coreybutler/nvm-windows/releases

# 鐎瑰顥婇獮璺哄瀼閹广垹鍩?Node 18
nvm install 18
nvm use 18

# 妤犲矁鐦夐悧鍫熸拱
node -v
```

### 9.6 婵″倷缍嶉崚鍥ㄥ床娑擃叀瀚抽弬鍥风吹| How to Switch Language?

閸撳秶顏弨顖涘瘮娑擃叀瀚抽弬鍥у瀼閹诡澁绱?
1. 閻愮懓鍤い鐢告桨閸欏厖绗傜憴鎺旀暏閹村嘲銇旈崓?
2. 闁瀚?鐠佸墽鐤?/ Settings"
3. 閸?鐠囶叀鈻?/ Language"娑撳濯哄鍡曡厬闁瀚?娑擃厽鏋?閹?English"

### 9.7 閸氬海顏挧閿嬫降娴滃棔绲炬鐐板姛閸欐垳绗夐崙鍝勫箵 | Feishu Integration Not Working

娴兼ê鍘涘Λ鈧弻銉窗

1. `.env.backend.local` 閺勵垰鎯佺€涙ê婀稉鏃堟暛閸氬秵顒滅涵?
2. `INTEGRATION_FEISHU_RECEIVE_ID` 閺勵垰鎯侀崣顖滄暏
3. 鎼存梻鏁ら弶鍐閺勵垰鎯侀崠鍛儓濞戝牊浼呴崣鎴︹偓浣烘祲閸?scope

### 9.8 閺堫剙婀撮弨閫涚啊闁板秶鐤嗘担鍡曠瑝閻㈢喐鏅?| Config Changes Not Taking Effect

绾喛顓绘担鐘虫Ц闁俺绻?`npm run dev:backend` 閹?`npm run dev:backend:stable` 閸氼垰濮╅敍鍫滆⒈閼板懘鍏樻导姘宠泲 `run-maven.mjs` 鐠囪褰囬張顒€婀?env閿涘鈧?

## 10. 閻╃鍙ч梼鍛邦嚢

- 閻╊喖缍嶇拠瀛樻閿涙瓪docs/PROJECT_STRUCTURE.md`
- 閸忋劍娅欑拫鍐暏閸ユ拝绱癭docs/PROJECT_FLOW_MAP.md`
- 鏉╂劗娣弬鍥ㄣ€傜槐銏犵穿閿涙瓪docs/README.md`
- 閸涙垝鎶ら崣鍌濃偓鍐跨窗`docs/operations/command-reference.md`
- 閻滎垰顣ㄩ惌鈺呮█閿涙瓪docs/operations/environment-matrix.md`

---

婵″倹鐏夋担鐘侯洣缂佈呯敾閹碘晛鐫嶆潻娆庨嚋妞ゅ湱娲伴敍灞界紦鐠侇喕绮犳潻娆庣瑏閺夛紕鍤庨獮鎯邦攽閹恒劏绻橀敍?

1. 閹跺﹣绗侀弬褰掓肠閹存劙鍘ょ純顔间粵閹存劗顫ら幋宄版倵閸欐澘褰茬紒瀛樺Б閿涘牐鈧奔绗夐弰顖欑矌閻滎垰顣ㄩ崣姗€鍣洪敍?
2. 缂佹瑩鈧氨鐓￠柧鎹愮熅閸旂姴銇戠拹銉ユ啞鐠€锔跨瑢閸欘垵顫嬮崠鏍櫢鐠囨洟娼伴弶?
3. 鐎瑰苯鏉介垾婊勫复閸?妞ょ敻娼?閺佺増宓佹惔鎾偓婵嗩嚠閻撗勬瀮濡楋綇绱濋梽宥勭秵閹恒儲澧滈幋鎰拱

## 11. 娴滃苯绱戣箛顐︹偓鐔峰弳閸?

- 閻戭厾鍋ｉ弬鍥︽閸︽澘娴橀敍姝歞ocs/DEVELOPMENT_HOTSPOTS.md`
- 閸忋劍娅欑拫鍐暏閸ユ拝绱癭docs/PROJECT_FLOW_MAP.md`
- 閺佸懘娈扮€规矮缍呴幍瀣斀閿涙瓪docs/PROJECT_TROUBLESHOOTING.md`
- 閸旂喕鍏樺Ο鈥虫健閻晠妯€閿涙瓪docs/MODULE_CAPABILITY_MATRIX.md`
- 閹恒儱褰涢崚鍡欑矋濞撳懎宕熼敍姝歞ocs/API_ENDPOINT_CATALOG.md`
- Postman 閼辨棁鐨熼崠鍜冪窗`docs/postman/crm-api.postman_collection.json` + `docs/postman/crm-local.postman_environment.json`
