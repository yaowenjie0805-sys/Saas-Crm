package com.yao.crm.controller;

import com.yao.crm.dto.request.AdminUpdateUserRequest;
import com.yao.crm.entity.UserAccount;
import com.yao.crm.repository.UserAccountRepository;
import com.yao.crm.security.LoginRiskService;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.yao.crm.support.TestTenant.TENANT_TEST;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AdminUserController 闂傚倸鍊风粈渚€骞夐敓鐘偓鍐幢濡炴洘妞藉浠嬵敇閻愭彃浜堕梻浣筋潐瀹曟绮旈鈧畷鐑筋敇濞戞ü澹曢梺鎸庣箓妤犳悂鐛Ο璁崇箚?
 * 
 * 婵犵數濮烽弫鎼佸磻閻愬樊鐒芥繛鍡樻惄閺佸嫰鏌涢鐘插姕闁稿骸锕ラ妵鍕冀閵娧呯厑闂佸摜鍠愬浠嬪蓟濞戙垹鐒洪柛鎰典簼閸ｎ厾绱掗崜褍浜炬繝銏★耿濠€渚€姊虹紒姗堜緵闁哥姵鐗犲畷婵嗏枎閹寸姷锛滃┑掳鍊愰崑鎾绘煙閾忣個顏堟偩閻戣姤鍊婚柤鎭掑劚閳ь剛绮穱濠囶敍濠靛棗鎯為悷婊呭閻撯€愁潖? * 1. listUsers - 闂傚倸鍊风粈渚€骞夐敍鍕殰婵°倕鎳岄埀顒€鍟村畷銊╁级閹存繃鍎俊鐐€栭幐鍫曞垂閸︻厾涓嶉柕澶嗘櫆閻撳啰鎲稿鍫濈闁绘柨顨庨崵鏇㈡煕椤垵浜滅紒鍓佸仱閹﹢鎮欓棃娑楀濠电偛鐗勯崹钘夘潖? * 2. updateUser - 闂傚倸鍊风粈渚€骞栭鈷氭椽濡舵径瀣槐闂侀潧艌閺呮盯鎷戦悢灏佹斀闁绘ê寮舵径鍕煕鐎ｃ劌濮傞柡灞炬礃缁绘繆绠涢弴鐘虫闂備胶绮幐璇裁洪弽顒€绲归梻浣规偠閸庤崵寰婇懞銉︽珷闁圭粯宕¤ぐ鎺撳亗閹兼番鍨瑰▓妤呮⒑鏉炴壆顦︾紒澶屾嚀閻ｉ攱绺介崨濠冩珳闁瑰吋鐣崹瑙勭閹绢喗鈷掑ù锝堟娴滃綊鏌熼搹顐ｅ磳闁诡垰娲︾€靛ジ寮堕幋鐘插Е婵＄偑鍊栭崹鐓庘枖閺囩姭鍋撳顒佸覆nerScope闂傚倸鍊风欢姘焽瑜嶈灋闁哄啫鐗忛崣鏇㈡煟閻愮敻銈眀led闂? * 3. unlockUser - 闂傚倷娴囧畷鐢稿窗閹扮増鍋￠弶鍫氭櫇缁€濠傘€掑锝呬壕閻庢鍠撻崝鎴﹀极閹邦厼绶為悗锝庝簷缂傛捇姊绘笟鈧褔鈥﹂鐘典笉闁硅揪闄勯崑?
 * 
 * 婵犵數濮甸鏍闯椤栨粌绶ら柣锝呮湰瀹曟煡鎮楅敐搴℃灍闁绘挸鍊圭换婵囩節閸屾粌顣哄┑鈽嗗亝閿曘垽骞冨畡鎵虫瀻闊洦鎼╂导鈧梻渚€娼уΛ妤呮晝椤忓嫷娼栨繛宸憾閺佸﹪鏌涢…鎴濇殨闁规儳澧庣壕鐓幟归敐鍛棌闁搞倗鍠栭弻鐔割槹鎼粹檧鏋呴悗娈垮枟濞兼瑩锝炲┑瀣闁绘劕鐡ㄩ悵锕傛⒒閸屾瑧顦﹂柟纰卞亰钘濇い鏍仜閻ゎ噣鏌℃径瀣劸闁诲骸鐖奸弻娑樷攽閸℃寮稿┑鐐村灟閸ㄥ綊鎮為崹顐犱簻闁圭儤鍨甸鈺呮煢閸愵亜鏋涢柡灞炬礃瀵板嫬鈽夊顒傜厳闂備胶顭堥鍡涘箰婵犳艾绠柛娑樼摠閸婄粯淇婇婊呭笡妞ゆ挸缍婂缁樻媴閸涘﹨纭€濡炪們鍎查崝鏇㈠Υ閹烘鎹舵い鎾跺Х閻ｈ埖绻濋姀锝嗙【濠㈣泛娲畷鎴﹀箻閺傘儲顫嶅┑鐐叉媼閸欏孩绂嶅鍫熸櫇闁靛繈鍊ら弫鍌炴煕椤愶絾鍎曢柕澶堝剭瑜版帗鏅查柛銉ュ閸旂鈹戦埥鍡椾簼闁挎洏鍨藉濠氬焺閸愨晛顎撻悗鐟板濠㈡绮婇鈧铏圭磼濡皷濮囬梺鑽ゅ暀閸パ咁唵闂佺粯顭堝▍鏇熷垔閹绢喗鐓ｉ煫鍥ㄥ嚬濞兼劙鏌熼銈囩М婵﹥妞藉畷銊︾節閸屾粎鎳嗘繝纰樻閸嬪懘鎮烽埡渚囧殨?
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AdminUserControllerTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private LoginRiskService loginRiskService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private I18nService i18nService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AdminUserController adminUserController;

    @BeforeEach
    void setUp() {
        // mock I18nService fallback
        when(i18nService.msg(any(HttpServletRequest.class), anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(1);
            return key;
        });
        when(request.getAttribute("authTenantId")).thenReturn(TENANT_TEST);
    }

    // ==================== listUsers 婵犵數濮烽弫鎼佸磻閻愬樊鐒芥繛鍡樻惄閺佸嫰鏌涢鐘插姕闁?====================

    /**
     * 婵犵數濮烽弫鎼佸磻閻愬樊鐒芥繛鍡樻惄閺佸嫰鏌涢鐘插姕闁?listUsers - 闂傚倸鍊风粈渚€骞栭锕€鐤柣妤€鐗婇崣蹇涙⒒閸喍绶遍柣鎺嶇矙閺屾盯顢曢悩鎻掑闂佹娊鏀卞Λ鍐蓟閻斿吋鐒介柨鏇楀亾闁诲繘浜堕弻娑㈡偄妞嬪海顔掑┑?     * 濠电姴鐥夐弶搴撳亾濡や焦鍙忛柟缁㈠枟閸庢銆掑锝呬壕闂佽鍨悞锕€顕ラ崟顖氱疀妞ゆ挆鍕垫?ADMIN 闂傚倷娴囧畷鐢稿窗閹扮増鍋￠柨鏃傚亾閺嗘粓鏌ｉ弬鎸庢喐闁绘繆娉涢埞鎴︽偐閸欏鎮欑紓渚囧亜缁夊綊寮婚敐鍛傜喖鎼归惂鍝ョ闂備線娼уΛ妤呮晝椤忓牆绠栭悷娆忓婵挳鏌涘☉姗堝伐濞寸姵娼欓埞鎴﹀煡閸℃ぞ绨婚梺缁樼墪閵堟悂鐛崘鈺冾浄閻庯綆鈧厸鏅犻弻銊╁即濡も偓娴滈箖姊虹紒妯诲鞍婵炶尙鍠栧璇测槈閵忕姷顔婇梺鐟扮仢閸燁垶鎮炬禒瀣拺?
     */
    @Test
    void testListUsers_Forbidden() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("SALES");

        // Act
        ResponseEntity<?> response = adminUserController.listUsers(request);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCodeValue());
        verify(userAccountRepository, never()).findAll();
    }

    /**
     * 婵犵數濮烽弫鎼佸磻閻愬樊鐒芥繛鍡樻惄閺佸嫰鏌涢鐘插姕闁?listUsers - 闂傚倸鍊烽懗鍫曞箠閹剧粯鍋ら柕濞炬櫅缁€澶愭煛閸モ晛鏋戦柛娆忕箲缁绘盯骞嬪▎蹇曚痪闂佹悶鍊栧濠氬焵椤掑倹鍤€閻庢凹鍘奸…鍨熼悡搴ｇ瓘闁荤姵浜介崝搴ｅ婵傚憡鍊堕柣鎰硾琚氶梺璇″灠閻楁捇寮诲☉鈶┾偓锕傚箣濠婂懏鏅奸梻?     */
    @Test
    void testListUsers_Success_EmptyList() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(userAccountRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<?> response = adminUserController.listUsers(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertTrue(items.isEmpty());
        verify(userAccountRepository).findAll();
    }

    /**
     * 婵犵數濮烽弫鎼佸磻閻愬樊鐒芥繛鍡樻惄閺佸嫰鏌涢鐘插姕闁?listUsers - 闂傚倸鍊烽懗鍫曞箠閹剧粯鍋ら柕濞炬櫅缁€澶愭煛閸モ晛鏋戦柛娆忕箲缁绘盯骞嬪▎蹇曚痪闂佹悶鍊栧濠氬焵椤掑倹鍤€閻庢凹鍘奸…鍨熼悡搴ｇ瓘闂佺鍕垫畷闁绘挾鍠愰妵鍕敃椤愩垹顫╂繝纰樷偓鑼煓闁哄本鐩崺锟犲磼濠婂嫬鍨遍柣搴ゎ潐濞叉﹢鎳濋幑鎰簷闂備礁鎲″ú宥夊棘娓氣偓瀹曟垿骞樼拠鑼槰濡炪倖姊归崕宕囩矈閿曞倹鈷戦柛蹇撳悑閸婃劖绻涙担鍐叉处閸庡﹤鈹戦悩宕囶暡闁绘挾鍠愰妵鍕敃椤愩垹顫╂繝纰樷偓鑼煓闁哄本鐩崺锟犲磼濠婂嫬鍨遍柣搴ゎ潐濞插繘宕曢柆宥庢晣濠靛倻顭堥獮銏′繆閵堝懎鏆熼柟鍐叉喘濮?     */
    @Test
    void testListUsers_Success_WithUsers() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");

        UserAccount user1 = createUser("zack", "Zack", "ADMIN", true);
        UserAccount user2 = createUser("alice", "Alice", "SALES", true);
        UserAccount user3 = createUser("bob", "Bob", "MANAGER", false);

        when(userAccountRepository.findAll()).thenReturn(Arrays.asList(user1, user2, user3));
        when(loginRiskService.isUserLocked(anyString(), anyString())).thenReturn(false);
        when(loginRiskService.remainingUserSeconds(anyString(), anyString())).thenReturn(0L);

        // Act
        ResponseEntity<?> response = adminUserController.listUsers(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertEquals(3, items.size());
        // 濠电姴鐥夐弶搴撳亾濡や焦鍙忛柟缁㈠枟閸庢銆掑锝呬壕闂佽鍨悞锕€顕ラ崟顖氱疀妞ゆ帒鍋嗛崬璺衡攽閻橆喖鐏辨繛澶嬬洴瀵敻顢楅崟顐ｈ緢闂婎偄娲︾粙鎺楁偂濞嗘挻鐓涘璺侯儏閻忋儲淇婇幓鎺濈吋闁哄瞼鍠栭、娆撴嚍閵壯屾Ч婵°倗濮烽崑鐐垫暜閿熺姷宓侀柟鐑橆殔缁狅綁鏌ｅΟ娲诲晱闁?
        assertEquals("alice", items.get(0).get("username"));
        assertEquals("bob", items.get(1).get("username"));
        assertEquals("zack", items.get(2).get("username"));
    }

    /**
     * 婵犵數濮烽弫鎼佸磻閻愬樊鐒芥繛鍡樻惄閺佸嫰鏌涢鐘插姕闁?listUsers - 闂傚倸鍊烽悞锕€顪冮崹顕呯劷闁秆勵殔缁€澶屸偓骞垮劚椤︻垶寮伴妷锔剧闁瑰鍋熼。鏌ユ煕鐎ｎ偆澧柕鍥у楠炲洭宕奸弴鐕佲偓宥夋⒑鏉炴壆鍔嶇€光偓閹间礁绠栨俊銈呭暞閸犲棝鏌涢弴銊ュ婵炲牄鍊曢埞鎴︽倷閼碱剙顤€闂佹悶鍔屽锟犳偘椤曗偓瀹曞崬鈽夊Ο铏圭崺婵＄偑鍊栭幐楣冨磻閻愮儤鍊?
     */
    @Test
    void testListUsers_Success_WithLockedUser() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");

        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        when(userAccountRepository.findAll()).thenReturn(Arrays.asList(user));
        when(loginRiskService.isUserLocked(TENANT_TEST, "testuser")).thenReturn(true);
        when(loginRiskService.remainingUserSeconds(TENANT_TEST, "testuser")).thenReturn(300L);

        // Act
        ResponseEntity<?> response = adminUserController.listUsers(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertEquals(1, items.size());
        assertTrue((Boolean) items.get(0).get("locked"));
        assertEquals(300L, items.get(0).get("lockRemainingSeconds"));
    }

    // ==================== updateUser 婵犵數濮烽弫鎼佸磻閻愬樊鐒芥繛鍡樻惄閺佸嫰鏌涢鐘插姕闁?====================

    /**
     * 婵犵數濮烽弫鎼佸磻閻愬樊鐒芥繛鍡樻惄閺佸嫰鏌涢鐘插姕闁?updateUser - 闂傚倸鍊风粈渚€骞栭锕€鐤柣妤€鐗婇崣蹇涙⒒閸喍绶遍柣鎺嶇矙閺屾盯顢曢悩鎻掑闂佹娊鏀卞Λ鍐蓟閻斿吋鐒介柨鏇楀亾闁诲繘浜堕弻娑㈡偄妞嬪海顔掑┑?     */
    @Test
    void testUpdateUser_Forbidden() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("SALES");
        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCodeValue());
        verify(userAccountRepository, never()).findByUsername(anyString());
    }

    /**
     * 婵犵數濮烽弫鎼佸磻閻愬樊鐒芥繛鍡樻惄閺佸嫰鏌涢鐘插姕闁?updateUser - 闂傚倸鍊烽悞锕€顪冮崹顕呯劷闁秆勵殔缁€澶屸偓骞垮劚椤︻垶寮伴妷锔剧闁瑰鍋熼幊鍛存煃缂佹ɑ鈷掗柍褜鍓欑粻宥夊磿闁秴绠犻柟鐗堟緲缁犳椽鏌ｅΟ鑲╁笡闁?     */
    @Test
    void testUpdateUser_UserNotFound() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(userAccountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());
        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setRole("ADMIN");

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "nonexistent", payload);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCodeValue());
    }

    /**
     * 婵犵數濮烽弫鎼佸磻閻愬樊鐒芥繛鍡樻惄閺佸嫰鏌涢鐘插姕闁?updateUser - 闂傚倸鍊风粈渚€骞栭锕€鐤柣妤€鐗婇崣蹇涙煟閵忋埄鐒鹃柣銈夌畺閺屾洘绔熼姘仼闁哄鍊垮娲捶椤撶偛濡哄┑鐐插级閻楃姾妫?
     */
    @Test
    void testUpdateUser_InvalidRole() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setRole("INVALID_ROLE");

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    /**
     * 婵犵數濮烽弫鎼佸磻閻愬樊鐒芥繛鍡樻惄閺佸嫰鏌涢鐘插姕闁?updateUser - 闂傚倸鍊烽懗鍫曞箠閹剧粯鍋ら柕濞炬櫅缁€澶愭煛閸モ晛鏋戦柛娆忕箲缁绘盯骞嬪▎蹇曚患闂佸憡顨嗘繛濠囧箖濡も偓閳藉鈻嶉搹顐㈢伌闁诡噯绻濋崺鈧い鎺戝閻撶喖鏌ｉ弬鎸庢喐闁瑰啿瀚伴幃浠嬵敍濠婂啯鐎剧紓浣规⒒閸犳劗鎹㈠┑瀣妞ゅ繋鐒﹂悘?ADMIN
     */
    @Test
    void testUpdateUser_Success_UpdateRoleToAdmin() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        user.setOwnerScope("testuser");
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loginRiskService.isUserLocked(TENANT_TEST, "testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds(TENANT_TEST, "testuser")).thenReturn(0L);

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setRole("ADMIN");

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userAccountRepository).save(any(UserAccount.class));
        verify(auditLogService).record(eq("admin"), eq("ADMIN"), eq("UPDATE"), eq("USER"), eq("testuser"), anyString(), eq(TENANT_TEST));
    }

    /**
     * 婵犵數濮烽弫鎼佸磻閻愬樊鐒芥繛鍡樻惄閺佸嫰鏌涢鐘插姕闁?updateUser - 闂傚倸鍊烽懗鍫曞箠閹剧粯鍋ら柕濞炬櫅缁€澶愭煛閸モ晛鏋戦柛娆忕箲缁绘盯骞嬪▎蹇曚患闂佸憡顨嗘繛濠囧箖濡も偓閳藉鈻嶉搹顐㈢伌闁诡噯绻濋崺鈧い鎺戝閻撶喖鏌ｉ弬鎸庢喐闁瑰啿瀚伴幃浠嬵敍濠婂啯鐎剧紓浣规⒒閸犳劗鎹㈠┑瀣妞ゅ繋鐒﹂悘?SALES闂傚倸鍊烽悞锔锯偓绗涘懐鐭欓柟杈鹃檮閸嬪鏌涘☉鍗炵仭鐎规洖寮堕幈銊ノ熼崹顔惧帿闂?ownerScope闂?     */
    @Test
    void testUpdateUser_Success_UpdateRoleToSales() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "ADMIN", true);
        user.setOwnerScope("");
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loginRiskService.isUserLocked(TENANT_TEST, "testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds(TENANT_TEST, "testuser")).thenReturn(0L);

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setRole("SALES");

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userAccountRepository).save(any(UserAccount.class));
    }

    /**
     * 婵犵數濮烽弫鎼佸磻閻愬樊鐒芥繛鍡樻惄閺佸嫰鏌涢鐘插姕闁?updateUser - 闂傚倸鍊烽懗鍫曞箠閹剧粯鍋ら柕濞炬櫅缁€澶愭煛閸モ晛鏋戦柛娆忕箲缁绘盯骞嬪▎蹇曚患闂佸憡顨嗘繛濠囧箖濡も偓閳藉鈻嶉搹顐㈢伌闁诡噯绻濋崺鈧?ownerScope闂傚倸鍊烽悞锔锯偓绗涘懐鐭欓柟杈鹃檮閸嬪鏌涘☉鍗炵仩闁?SALES 闂傚倷娴囧畷鐢稿窗閹扮増鍋￠柨鏃傚亾閺嗘粓鏌ｉ弬鎸庢喐闁绘繆娉涢埞鎴︽偐閸欏鎮欓柣搴㈣壘椤︻垶鈥︾捄銊﹀磯濞撴凹鍨伴崜鍗炩攽閻愬弶鍣圭紒缁橈耿瀵?     */
    @Test
    void testUpdateUser_Success_UpdateOwnerScopeForSales() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        user.setOwnerScope("oldscope");
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loginRiskService.isUserLocked(TENANT_TEST, "testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds(TENANT_TEST, "testuser")).thenReturn(0L);

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setOwnerScope("newscope");

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userAccountRepository).save(any(UserAccount.class));
    }

    /**
     * 婵犵數濮烽弫鎼佸磻閻愬樊鐒芥繛鍡樻惄閺佸嫰鏌涢鐘插姕闁?updateUser - 闂?SALES 闂傚倷娴囧畷鐢稿窗閹扮増鍋￠柨鏃傚亾閺嗘粓鏌ｉ弬鎸庢喐闁绘繆娉涢埞鎴︽偐閸欏鎮欓梺鍛婎殕婵炲﹪骞冨Δ鈧埥澶娾枍閾忣偄鐏撮柟顕嗙節閸┾偓?ownerScope 闂傚倸鍊风粈渚€骞栭锕€鐤柣妤€鐗婇崣蹇涙煟閵忋埄鐒鹃柣?
     */
    @Test
    void testUpdateUser_Success_OwnerScopeIgnoredForNonSales() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "ADMIN", true);
        user.setOwnerScope("adminscope");
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loginRiskService.isUserLocked(TENANT_TEST, "testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds(TENANT_TEST, "testuser")).thenReturn(0L);

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setOwnerScope("newscope");

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // ADMIN 闂傚倷娴囧畷鐢稿窗閹扮増鍋￠柨鏃傚亾閺嗘粓鏌ｉ弬鎸庢喐闁绘繆娉涢埞鎴︽偐閸欏顦╅梺?ownerScope 濠电姷鏁搁崑鐐哄垂閸洖绠伴柛婵勫劤閻捇鎮楅崹顐ゆ憙濠殿喗濞婇弻銈囧枈閸楃偛顫梺绋款儐閻楁濡甸崟顖氱疀闁告挷鑳惰摫缂傚倷鑳堕搹搴ㄥ垂閸洖钃?        verify(userAccountRepository).save(any(UserAccount.class));
    }

    /**
     * 婵犵數濮烽弫鎼佸磻閻愬樊鐒芥繛鍡樻惄閺佸嫰鏌涢鐘插姕闁?updateUser - 闂傚倸鍊烽懗鍫曞箠閹剧粯鍋ら柕濞炬櫅缁€澶愭煛閸モ晛鏋戦柛娆忕箲缁绘盯骞嬪▎蹇曚患闂佸憡顨嗘繛濠囧箖濡も偓閳藉鈻嶉搹顐㈢伌闁诡噯绻濋崺鈧?enabled 闂傚倸鍊烽懗鍓佸垝椤栫偐鈧箓宕奸妷銉︽К闂佸搫绋侀崢濂告倿?     */
    @Test
    void testUpdateUser_Success_UpdateEnabled() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loginRiskService.isUserLocked(TENANT_TEST, "testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds(TENANT_TEST, "testuser")).thenReturn(0L);

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setEnabled(false);

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userAccountRepository).save(any(UserAccount.class));
    }

    /**
     * 婵犵數濮烽弫鎼佸磻閻愬樊鐒芥繛鍡樻惄閺佸嫰鏌涢鐘插姕闁?updateUser - SALES 闂傚倷娴囧畷鐢稿窗閹扮増鍋￠柨鏃傚亾閺嗘粓鏌ｉ弬鎸庢喐闁绘繆娉涢埞鎴︽偐閹绘帗娈堕梺?ownerScope 濠电姷鏁搁崑鐐哄垂閸洖绠插ù锝囩《閺嬪秹鏌ㄥ┑鍡╂Ц闁绘挻锕㈤弻鈥愁吋鎼粹€崇缂備緡鍋勭粔褰掑蓟閻旂⒈鏁嶆繛鎴炵懅閸戔€斥攽閻愯尙澧旈柛妤佸▕瀵鈽夐姀鐘愁棟闁荤姴娲﹁ぐ鍐綖閸涱収娓婚柕鍫濇閳锋劖绻涢崣澶岀煂婵″弶鍔欏顕€宕奸锝嗘珦闂備礁鎼€氼剛鎹㈤幒妞濆洦瀵肩€涙ǚ鎷哄┑鐐跺皺缁垱绻涢崶顒佺厱闁哄倽娉曟晥閻庤娲栭幖顐﹀煡婢舵劕顫呴柣妯兼磪?     */
    @Test
    void testUpdateUser_Success_AutoSetOwnerScopeForSales() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        user.setOwnerScope(null);
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> {
            UserAccount saved = inv.getArgument(0);
            assertEquals("testuser", saved.getOwnerScope());
            return saved;
        });
        when(loginRiskService.isUserLocked(TENANT_TEST, "testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds(TENANT_TEST, "testuser")).thenReturn(0L);

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setEnabled(true);

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    /**
     * 婵犵數濮烽弫鎼佸磻閻愬樊鐒芥繛鍡樻惄閺佸嫰鏌涢鐘插姕闁?updateUser - 闂傚倷娴囧畷鐢稿窗閹扮増鍋￠柨鏃傚亾閺嗘粓鏌ｉ弬鎸庢喐闁绘繆娉涢埞鎴︽偐閹绘帗娈跺?SALES 闂傚倸鍊峰ù鍥Υ閳ь剟鏌涚€ｎ偅宕屾慨濠冩そ椤㈡洟鏁愰崶鍓佷紘缂傚倷璁查崑?ADMIN 闂傚倸鍊风粈渚€骞栭锕€鐤柟鎯版閺勩儵鏌″搴′簽闁告宀搁弻銈嗘叏閹邦兘鍋撳Δ鍛剹?ownerScope
     */
    @Test
    void testUpdateUser_Success_ClearOwnerScopeWhenNotSales() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        user.setOwnerScope("testscope");
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> {
            UserAccount saved = inv.getArgument(0);
            assertEquals("", saved.getOwnerScope());
            return saved;
        });
        when(loginRiskService.isUserLocked(TENANT_TEST, "testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds(TENANT_TEST, "testuser")).thenReturn(0L);

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setRole("ADMIN");

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    /**
     * 婵犵數濮烽弫鎼佸磻閻愬樊鐒芥繛鍡樻惄閺佸嫰鏌涢鐘插姕闁?updateUser - 闂傚倷娴囧畷鐢稿窗閹扮増鍋￠柨鏃傚亾閺嗘粓鏌ｉ弬鎸庢喐闁绘繆娉涢埞鎴︽偐閹绘帗娈紓浣稿閸嬬喖骞夌粙娆惧悑濠㈣泛顑呴崜顓烆渻閵堝棗绗掗悗姘煎墰缁牓宕橀埡鍐啎闂佺硶鍓濋〃鍡涘箲閿濆鐓熼柣鏂挎憸缁犵偤鏌＄仦绋垮⒉闁瑰嘲鎳樺畷顐﹀Ψ瑜滈崬褰掓⒒?
     */
    @Test
    void testUpdateUser_Success_RoleCaseInsensitive() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loginRiskService.isUserLocked(TENANT_TEST, "testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds(TENANT_TEST, "testuser")).thenReturn(0L);

        AdminUpdateUserRequest payload = new AdminUpdateUserRequest();
        payload.setRole("manager"); // 闂傚倷娴囬褏鎹㈤幇顔藉床闁归偊鍠楀畷鏌ユ煙閻楀牊绶查柣?

        // Act
        ResponseEntity<?> response = adminUserController.updateUser(request, "testuser", payload);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ==================== unlockUser 婵犵數濮烽弫鎼佸磻閻愬樊鐒芥繛鍡樻惄閺佸嫰鏌涢鐘插姕闁?====================

    /**
     * 婵犵數濮烽弫鎼佸磻閻愬樊鐒芥繛鍡樻惄閺佸嫰鏌涢鐘插姕闁?unlockUser - 闂傚倸鍊风粈渚€骞栭锕€鐤柣妤€鐗婇崣蹇涙⒒閸喍绶遍柣鎺嶇矙閺屾盯顢曢悩鎻掑闂佹娊鏀卞Λ鍐蓟閻斿吋鐒介柨鏇楀亾闁诲繘浜堕弻娑㈡偄妞嬪海顔掑┑?     */
    @Test
    void testUnlockUser_Forbidden() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("MANAGER");

        // Act
        ResponseEntity<?> response = adminUserController.unlockUser(request, "testuser");

        // Assert
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCodeValue());
        verify(loginRiskService, never()).clearUser(anyString(), anyString());
    }

    /**
     * 婵犵數濮烽弫鎼佸磻閻愬樊鐒芥繛鍡樻惄閺佸嫰鏌涢鐘插姕闁?unlockUser - 闂傚倸鍊烽悞锕€顪冮崹顕呯劷闁秆勵殔缁€澶屸偓骞垮劚椤︻垶寮伴妷锔剧闁瑰鍋熼幊鍛存煃缂佹ɑ鈷掗柍褜鍓欑粻宥夊磿闁秴绠犻柟鐗堟緲缁犳椽鏌ｅΟ鑲╁笡闁?     */
    @Test
    void testUnlockUser_UserNotFound() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(userAccountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = adminUserController.unlockUser(request, "nonexistent");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCodeValue());
        verify(loginRiskService, never()).clearUser(anyString(), anyString());
    }

    /**
     * 婵犵數濮烽弫鎼佸磻閻愬樊鐒芥繛鍡樻惄閺佸嫰鏌涢鐘插姕闁?unlockUser - 闂傚倸鍊烽懗鍫曞箠閹剧粯鍋ら柕濞炬櫅缁€澶愭煛閸モ晛鏋戦柛娆忕箲缁绘盯骞嬮悙娈挎殹闂佸摜濮村Λ婵嬪蓟閵娿儮鏀介柛鈾€鏅滄晥闂備浇妗ㄩ悞锕€鐣濋幖浣歌摕闁挎繂顧€缂嶆牗銇勯幒鍡椾壕婵犵鈧尙鐭欓柡?
     */
    @Test
    void testUnlockUser_Success() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        when(request.getAttribute("authUsername")).thenReturn("admin");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(loginRiskService.isUserLocked(TENANT_TEST, "testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds(TENANT_TEST, "testuser")).thenReturn(0L);

        // Act
        ResponseEntity<?> response = adminUserController.unlockUser(request, "testuser");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(loginRiskService).clearUser(TENANT_TEST, "testuser");
        verify(auditLogService).record(eq("admin"), eq("ADMIN"), eq("UNLOCK"), eq("USER"), eq("testuser"), anyString(), eq(TENANT_TEST));
    }

    // ==================== toView 缂傚倸鍊搁崐椋庣矆娓氣偓钘濇い鏇楀亾闁诡喚鍋ら弫鍐焵椤掑嫭鏅濋柕蹇ョ磿閻熷綊鏌嶈閸撴瑩顢氶敐澶樻晢闁稿本绋戦弸鍌炴⒑閸涘﹥澶勯柛妯虹秺閸┾偓妞ゆ巻鍋撻柨鏇樺劤閹广垹鈽夐姀鐘甸獓闂佽鎯岄崢濂稿礈閻㈠憡鈷戝ù鍏肩懃閻︽粓鏌涢妸銉у煟鐎殿噮鍋勯鍏煎緞婵犲洤鏁归梻渚€娼чˇ顓㈠垂鐟欏嫮顩查柣鎰靛墯閸欏繑淇婇婵囶仩闁挎稑绉剁槐鎺楁偐瀹曞洤鈷岄梺鍝勬湰缁嬫牜绮诲☉銏℃櫜闁告侗鍙庡鑽ょ磽閸屾瑦绁板瀛樻倐瀹曞綊宕归鍛濡炪倖甯掗崐褰掑窗閸℃稒鐓曢柡鍥ュ妼楠炴垿鏌嶈閸撴瑩鏁冮鍕垫綎濠电姵鑹剧壕鍏兼叏濡潡鍝洪柟鎻掋偢閺岋綁濮€閳轰胶浠┑鐐插悑閻熲晛锕㈡笟鈧弻锝嗘償椤栨粎校闂佸憡蓱閸庡啿宓勯梺褰掓？閻掞箓鎮?===================

    /**
     * 婵犵數濮烽弫鎼佸磻閻愬樊鐒芥繛鍡樻惄閺佸嫰鏌涢鐘插姕闁?toView - ownerScope 濠?null 闂傚倸鍊风粈渚€骞栭锕€鐤い鏍仜绾惧潡鏌ゅù瀣珕鐎规洘鐓￠弻鐔告綇閸撗呮殸闂佺粯鍔曢敃顏堝蓟閿濆绠涙い鏇炴噺濮ｅ矂姊烘潪鎵槮閻庢矮鍗抽獮鍐ㄎ旈埀顒勫煡婢跺ň鏋庢俊顖濆吹閺嗩厽淇婇悙顏勨偓鏍垂閹惰姤鍊块柨鏂垮⒔閻?     */
    @Test
    void testToView_NullOwnerScope() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        UserAccount user = createUser("testuser", "Test User", "SALES", true);
        user.setOwnerScope(null);
        when(userAccountRepository.findAll()).thenReturn(Arrays.asList(user));
        when(loginRiskService.isUserLocked(TENANT_TEST, "testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds(TENANT_TEST, "testuser")).thenReturn(0L);

        // Act
        ResponseEntity<?> response = adminUserController.listUsers(request);

        // Assert
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertEquals("", items.get(0).get("ownerScope"));
    }

    /**
     * 婵犵數濮烽弫鎼佸磻閻愬樊鐒芥繛鍡樻惄閺佸嫰鏌涢鐘插姕闁?toView - enabled 濠?null 闂傚倸鍊风粈渚€骞栭锕€鐤い鏍仜绾惧潡鏌ゅù瀣珕鐎规洘鐓￠弻鐔告綇閸撗呮殸闂?false
     */
    @Test
    void testToView_NullEnabled() {
        // Arrange
        when(request.getAttribute("authRole")).thenReturn("ADMIN");
        UserAccount user = createUser("testuser", "Test User", "SALES", null);
        when(userAccountRepository.findAll()).thenReturn(Arrays.asList(user));
        when(loginRiskService.isUserLocked(TENANT_TEST, "testuser")).thenReturn(false);
        when(loginRiskService.remainingUserSeconds(TENANT_TEST, "testuser")).thenReturn(0L);

        // Act
        ResponseEntity<?> response = adminUserController.listUsers(request);

        // Assert
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertFalse((Boolean) items.get(0).get("enabled"));
    }

    // ==================== 闂傚倷绀侀幖顐λ囬鐐村亱闁糕剝绋戠粻鐘诲箹濞ｎ剙鐏柛娆忕箲閵囧嫯绠涢幘璺侯暫濠碘槅鍋呴敃銏ゅ箖瀹勬壋鏋庨煫鍥ㄦ惄娴尖偓闂?====================

    private UserAccount createUser(String username, String displayName, String role, Boolean enabled) {
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setEnabled(enabled);
        user.setPassword("hashedpassword");
        user.setTenantId(TENANT_TEST);
        return user;
    }
}



