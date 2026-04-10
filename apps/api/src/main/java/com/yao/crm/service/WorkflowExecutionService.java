package com.yao.crm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yao.crm.entity.*;
import com.yao.crm.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import com.yao.crm.enums.WorkflowStatus;
import com.yao.crm.enums.ApprovalStatus;
import com.yao.crm.enums.NodeType;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 闂佽姘﹂～澶愬箖閸洖纾块柟娈垮枤缁€濠囨煛閸屾冻绱╅悷娆忓婵挳鏌ｉ悢绋款棎闁圭鍟撮弻锝夋倷鐎电硶妲堥梺鍏兼た閸ㄥ啿危閹版澘鐓涢柛娑卞幘椤?
 * 闂備浇宕垫慨鐢稿礉濡ゅ懎绐楅柡鍥ュ灪閸庢淇婇妶鍛櫣缂佺姰鍎抽幉鎼佸箣閿旇　鍋撴笟鈧獮瀣攽閹邦剚娅婇梻渚€娼чˇ顓㈠磹濡ゅ啰鐭欏┑鐘插暟缁犲墽绱撻崼銏犘ｆい锔惧厴閺屻倝宕归銏紝闂佽鍨伴崯鏉戠暦閻旂⒈鏁冮柨婵嗘噳閸嬫挸螖閳ь剟鍩ユ径鎰闁告剬鍕闯濠电姷顣藉Σ鍛村矗閸愵喖绠栨繛鍡樻尰閸庡矂鏌涘┑鍕姢妞わ富鍠楃换婵嬪閿濆棛銆愰柣搴㈠嚬閸ｏ綁鐛崘鈺冪瘈闁搞儜鍛寸崜闂備胶鍋ㄩ崕鍗烆嚕閸洦鏁冨ù鐘差儐閻撴洟鏌熼悜妯虹仸妞ゃ儳鍋ら弻鈩冪瑹閸パ勭彎婵犵鈧磭鎽犻柟宄版嚇瀹曟粓骞撻幒鎾斥偓搴ㄦ⒒娓氣偓閳ь剚绋撶粊閿嬨亜椤愩埄妲虹紒顔硷躬閺佸倿宕滆閻撴捇鏌ｆ惔顖滅У闁告挻鐩獮?
 */
@Service
public class WorkflowExecutionService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionService.class);

    private final WorkflowDefinitionRepository workflowRepository;
    private final WorkflowNodeRepository nodeRepository;
    private final WorkflowConnectionRepository connectionRepository;
    private final WorkflowExecutionRepository executionRepository;
    private final ApprovalNodeRepository approvalNodeRepository;
    private final ObjectMapper objectMapper;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final WorkflowConditionEvaluator workflowConditionEvaluator;
    private final WorkflowActionExecutor workflowActionExecutor;
    private final WorkflowNotificationExecutor workflowNotificationExecutor;

    // 闂傚倷绀佸﹢閬嶆偡閹惰棄骞㈤柍鍝勫€归弶绋库攽閻愭潙鐏﹂柟灏栨櫊瀹曘垽骞嗚閺嬫棃鏌熺€涙绠ラ柛鐔锋嚇閺屾洘寰勯崼婵冨亾濡ゅ啰鐭欏┑鐘插暟缁犲墽绱撻崼銏犘柛蹇撶灱缁辨帡骞夌€ｎ亶娲梺閫炲苯澧紒瀣浮閺佸啴顢曢妶鍡╂綗濠电偛妫欓幐濠氬磻閵娾晜鐓忓┑鐐茬仢閳ь剚顨婇幊婵嬪箚瑜滈悢鍡涙偣閸濆嫭鎯堟い蹇ｅ亰閺岀喖顢欑涵宄颁紣濡炪倖娲╃紞浣哥暦閹烘垟妲堥柛妤冨仜宸嶉梻浣筋嚙妤犳悂宕㈠鍫濈婵犻潧娲ㄩ悵鍫曟煙閻戞ɑ鈷掑鐟板暱椤法鎹勯悮瀛樻暰缂備降鍔嶅畝鎼佸蓟閿熺姴妞藉ù锝呮憸娴煎牏绱?
    private final Cache<String, WorkflowExecutionContext> activeExecutions = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(24, TimeUnit.HOURS)
            .build();

    public WorkflowExecutionService(
            WorkflowDefinitionRepository workflowRepository,
            WorkflowNodeRepository nodeRepository,
            WorkflowConnectionRepository connectionRepository,
            WorkflowExecutionRepository executionRepository,
            ApprovalNodeRepository approvalNodeRepository,
            ObjectMapper objectMapper,
            @Qualifier("taskExecutor") ThreadPoolTaskExecutor taskExecutor,
            WorkflowConditionEvaluator workflowConditionEvaluator,
            WorkflowActionExecutor workflowActionExecutor,
            WorkflowNotificationExecutor workflowNotificationExecutor) {
        this.workflowRepository = workflowRepository;
        this.nodeRepository = nodeRepository;
        this.connectionRepository = connectionRepository;
        this.executionRepository = executionRepository;
        this.approvalNodeRepository = approvalNodeRepository;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
        this.workflowConditionEvaluator = workflowConditionEvaluator;
        this.workflowActionExecutor = workflowActionExecutor;
        this.workflowNotificationExecutor = workflowNotificationExecutor;
    }

    /**
     * 闂傚倷绀侀幉锟犲礄瑜版帒鍨傞柣妤€鐗婇崣蹇涙煃鏉炴壆璐伴柛鐔锋嚇閺屾洘寰勯崼婵冨亾濡ゅ啰鐭欏┑鐘插暟缁犲墽绱撻崼銏犘ラ柛鏂跨Т椤儻顦撮柡鍜佸亰楠?
     */
    @Transactional(timeout = 30)
    public WorkflowExecution startExecution(String tenantId, String workflowId, String triggerType, String triggerSource,
                                            String triggerPayload, Map<String, Object> triggerData) {
        WorkflowDefinition workflow = workflowRepository.findByIdAndTenantId(workflowId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        if (!WorkflowStatus.isActive(workflow.getStatus())) {
            throw new IllegalStateException("Workflow is not active: " + workflow.getStatus());
        }

        // 闂傚倷绀侀幉锛勬暜濡ゅ啰鐭欓柟瀵稿Х绾句粙鏌熼幑鎰靛殭缂佺姰鍎抽幉鎼佸箣閿旇　鍋撴笟鈧獮瀣偐閸愬樊鈧盯姊洪崫鍕垫Ъ婵炲娲滅划?
        WorkflowExecution execution = new WorkflowExecution();
        execution.setId(UUID.randomUUID().toString());
        execution.setTenantId(tenantId);
        execution.setWorkflowId(workflowId);
        execution.setWorkflowVersion(workflow.getVersion());
        execution.setTriggerType(triggerType);
        execution.setTriggerSource(triggerSource);
        execution.setTriggerPayload(triggerPayload);
        execution.setStatus(WorkflowStatus.RUNNING.name());
        execution.setStartedAt(LocalDateTime.now());

        // 闂傚倷绀侀幉锛勬暜濡ゅ啯宕查柛宀€鍎戠紞鏍煙閻楀牊绶茬紒鈧畝鍕厸鐎广儱鍟俊濂告煃闁垮顥堥柟顔挎硾閳藉螣娓氼垱顔勭紓鍌欑劍椤ㄥ懘宕愰崷顓犵煓濠㈣泛澶囬崑鎾绘晲鎼粹€茬凹闂?
        WorkflowExecutionContext context = new WorkflowExecutionContext();
        context.setExecutionId(execution.getId());
        context.setWorkflowId(workflowId);
        context.setTriggerData(triggerData != null ? triggerData : new HashMap<>());
        context.setVariables(new HashMap<>());
        context.setNodeResults(new HashMap<>());
        context.setCurrentNodeId(null);
        context.setNextNodeIds(new ArrayList<>());

        try {
            execution.setExecutionContext(objectMapper.writeValueAsString(context));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize context", e);
            execution.setExecutionContext("{}");
        }

        execution = executionRepository.save(execution);
        activeExecutions.put(execution.getId(), context);

        // 闂傚倷绀侀幖顐⒚洪妶澶嬪仱闁靛ň鏅涢拑鐔封攽閻樻彃浜為柛鐔锋嚇閺屾洘寰勯崼婵冨亾濡ゅ啰鐭欏┑鐘插暟缁犲墽绱撻崼銏犘ラ柛鏂跨Т椤儻顦撮柡鍜佸亰楠炲啴寮剁捄銊︽畷闂佸憡鍔栭崕鎶藉箖娓氣偓濮?
        workflow.setExecutionCount(workflow.getExecutionCount() + 1);
        workflowRepository.save(workflow);

        final String savedExecutionId = execution.getId();
        final String savedTenantId = tenantId;
        // Submit to async executor to avoid blocking request thread.
        taskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                executeAsync(savedExecutionId, savedTenantId);
            }
        });

        return execution;
    }

    /**
     * 闂佽瀛╅鏍窗閺嶎厼纾圭憸鐗堝俯閺佸棝鏌ｉ幇顒佹儓缂佺姰鍎抽幉鎼佸箣閿旇　鍋撴笟鈧獮瀣攽閹邦剚娅婇梻渚€娼чˇ顓㈠磹濡ゅ啰鐭欏┑鐘插暟缁?
     */
    @Transactional(timeout = 30)
    public void executeAsync(String executionId) {
        executeAsync(executionId, null);
    }

    @Transactional(timeout = 30)
    public void executeAsync(String executionId, String tenantId) {
        try {
            executeNextNodes(executionId, tenantId);
        } catch (Exception e) {
            log.error("Workflow execution failed", e);
            failExecutionInternal(executionId, e.getMessage(), e.toString(), tenantId);
        }
    }

    /**
     * 闂傚倷绀佸﹢閬嶆偡閹惰棄骞㈤柍鍝勫€归弶绋库攽閻愭潙鐏﹂柟鍛婃倐椤㈡牠宕熼鐘插触闁诲孩绋掗…鍥煝閺冣偓閵囧嫯绠涢幘瀛樻倷闁句紮绻濆?
     */
    @Transactional(timeout = 30)
    public void executeNextNodes(String executionId) {
        executeNextNodes(executionId, null);
    }

    @Transactional(timeout = 30)
    public void executeNextNodes(String executionId, String tenantId) {
        while (true) {
            WorkflowExecution execution = loadExecution(executionId, tenantId);

            if (!WorkflowStatus.isRunning(execution.getStatus())) {
                return;
            }

            WorkflowExecutionContext context = getContext(execution);
            if (context == null) {
                context = parseContext(execution);
                activeExecutions.put(executionId, context);
            }

            // 婵犵數濮烽。浠嬪焵椤掆偓閸熷潡鍩€椤掆偓缂嶅﹪骞冨Ο璇茬窞濠电偟鍋撻悡銏ゆ⒑閺傘儲娅呴柛鐕佸灣缁骞掑Δ浣镐化閻熸粌绉瑰畷鏌ュ蓟閵夘垳绋忛梺瑙勫婢ф宕靛澶嬬厱閻忕偛澧介惌濠偯归悪鈧崹鍫曞蓟閵娿儮妲堟俊顖滅帛閹疯鲸绻濋姀锝庢綈闁挎洏鍨归悾宄邦潩鐠鸿櫣顔嗛柣蹇曞仦缁诲啫煤閻旈鏆﹂柨婵嗩槸缁狅絾绻濋棃娑欏窛鐟滅増绮撳娲偂鎼淬垺鍎撻梺绋匡工椤嘲鐣?
            if (context.getCurrentNodeId() == null) {
                List<WorkflowNode> triggerNodes = nodeRepository.findByWorkflowIdAndNodeType(execution.getWorkflowId(), NodeType.TRIGGER.name());
                if (triggerNodes.isEmpty()) {
                    completeExecutionInternal(executionId, execution.getTenantId());
                    return;
                }
                List<String> nextNodeIds = new ArrayList<>();
                nextNodeIds.add(triggerNodes.get(0).getId());
                context.setNextNodeIds(nextNodeIds);
            } else {
                // 婵犵數鍋涢顓熸叏鐎涙﹩娈介柛婵勫劤缁€濠囨倵閿濆骸鏋涚紒鈧崼鈶╁亾楠炲灝鍔氶柣妤€妫滈悘鎰版⒒娴ｈ櫣甯涢柛鏃€娲栭敃銏ゅ础閻愨晜鐏侀梺鐟邦嚟閸嬬偞绂嶉妶澶嬬厽闁归偊鍘界紞鎴濐熆鐠鸿櫣孝闁宠棄顦甸獮娆撳礃閵娿儳鍝楁俊銈囧Х閸嬫稓鎹㈠Ο铏规殾闁挎繂顦崘鈧梺鎸庢煥婢т粙骞夐鐣岀闁瑰鍋炵亸銊╂煕鐎ｎ偅灏柍钘夘樀楠炴瑩宕樿閹癸絾绻濈喊妯活潑闁告瑥鍟撮獮?
                List<WorkflowConnection> outgoing = connectionRepository.findBySourceNodeId(context.getCurrentNodeId());
                if (outgoing.isEmpty()) {
                    completeExecutionInternal(executionId, execution.getTenantId());
                    return;
                }
                context.setNextNodeIds(outgoing.stream().map(WorkflowConnection::getTargetNodeId).collect(Collectors.toList()));
            }

            // 闂傚倷绀佸﹢閬嶆偡閹惰棄骞㈤柍鍝勫€归弶绋库攽閻愭潙鐏﹂柟鍛婃倐椤㈡牠宕熼鐘插触闁诲孩绋掗…鍥煝閺冣偓閵囧嫯绠涢幘瀛樻倷闁句紮绻濆?
            List<String> nextNodeIds = context.getNextNodeIds();
            List<WorkflowNode> nodes = nodeRepository.findAllById(nextNodeIds);
            Map<String, WorkflowNode> nodeLookup = nodes.stream()
                    .collect(Collectors.toMap(WorkflowNode::getId, node -> node));

            for (String nodeId : nextNodeIds) {
                WorkflowNode node = nodeLookup.get(nodeId);
                if (node == null) continue;

                context.setCurrentNodeId(nodeId);
                execution.setCurrentNodeId(nodeId);
                saveContext(execution, context);

                NodeExecutionResult result = executeNode(node, context, execution);

                context.getNodeResults().put(nodeId, result);

                if (!result.isSuccess()) {
                    failExecutionInternal(executionId, result.getErrorMessage(), result.getErrorDetails(), execution.getTenantId());
                    return;
                }

                // 婵犵數濮烽。浠嬪焵椤掆偓閸熷潡鍩€椤掆偓缂嶅﹪骞冨Ο璇茬窞闁归偊鍓欏宄邦渻閵堝棛澧紒顔奸閳绘棃濮€閵堝棛鍘告繝銏ｆ硾閼活垶寮稿▎蹇婃斀闁宠棄妫楀顕€鏌熼鐣屾噰闁诡垰鍊垮畷顐﹀Ψ瑜滃Σ鐑芥⒑鐠囧弶鎹ｉ柟铏崌瀵敻顢楅崟顐㈢€┑顔筋焾濞夋稓绮婚妷褎鍠愰柟杈捐礋閳?
                if (NodeType.END.name().equals(node.getNodeType())) {
                    completeExecutionInternal(executionId, execution.getTenantId());
                    return;
                }

                // 婵犵數濮烽。浠嬪焵椤掆偓閸熷潡鍩€椤掆偓缂嶅﹪骞冨Ο璇茬窞闁归偊鍘煎▓鐔兼⒑缁嬫寧婀扮紒顔兼湰缁傛帡顢涢悙绮规嫽闂佹悶鍎荤徊鑺ョ妤ｅ啯鐓熼柣鏂挎啞绾惧鏌ら崘鑼煟闁诡喗妞介獮瀣攽閹邦剚顔傞梻浣告啞缁矂宕幍顔剧焼闁告洦鍋€閺€浠嬫煃閵夈儱鏆遍柍褜鍓氶幃鍌炪€佸鈧獮鏍ㄦ媴閸濄儺鍚嬮梻浣界毇閸愨晜鍠愮紓浣插亾闁糕剝绋掗悡銉︾箾閹寸倖鎴濓耿閻楀牏绠鹃柛顐ｇ箘缁犺崵鈧娲橀崹鐢糕€﹂妸鈺佺妞ゆ劗鍠愮€垫牠姊?
                if (result.isWaiting()) {
                    return;
                }
            }

            // 闂佽娴烽弫濠氬磻婵犲啰顩查柣鎰瀹撲線鏌涢埄鍐噮缁炬崘鍋愰幉姝岀疀濞戞瑥浠烘繝鐢靛У閼瑰墽绮婚妷褎鍠愰柟杈捐礋閳ь兛绶氶獮瀣倷绾版ɑ鐏冮梺璇插嚱缁叉椽寮插┑瀣垫晜闁告鍋愬Σ鍫ユ煙閸喖鏆曟繛鏉戝槻閳规垿鍩ラ崱妤€绫嶉梺璇″枟閻熲晠骞嗛崒鐐蹭紶闁靛／灞拘熼梻鍌欑閹碱偄煤閵堝桅婵せ鍋撶€规洦鍓熼、妤呭礋椤掑倸濮烽梻浣告贡閸庛倝宕归崷顓犵煋婵炲樊浜濋悡?
        }
    }

    /**
     * 闂傚倷绀佸﹢閬嶆偡閹惰棄骞㈤柍鍝勫€归弶鎼佹⒒娴ｅ憡鍟炴い銊ユ鐓ら柟杈鹃檮閻撳倹绻濇繝鍌滃闁搞倕绉归弻娑氫沪閸撗呯厐婵?
     */
    private NodeExecutionResult executeNode(WorkflowNode node, WorkflowExecutionContext context,
                                            WorkflowExecution execution) {
        NodeExecutionResult result = new NodeExecutionResult();
        result.setNodeId(node.getId());
        result.setNodeType(node.getNodeType());
        result.setNodeSubtype(node.getNodeSubtype());

        try {
            NodeType nodeType = NodeType.fromString(node.getNodeType());
            if (nodeType == null) {
                result.setSuccess(true);
                return result;
            }
            switch (nodeType) {
                case TRIGGER:
                    return executeTriggerNode(node, context, result);
                case CONDITION:
                    return executeConditionNode(node, context, result);
                case ACTION:
                    return executeActionNode(node, context, result);
                case NOTIFICATION:
                    return executeNotificationNode(node, result);
                case APPROVAL:
                    return executeApprovalNode(node, context, result, execution);
                case WAIT:
                    return executeWaitNode(node, context, result);
                case CC:
                    return executeCcNode(node, context, result);
                case END:
                    return executeEndNode(node, context, result);
                default:
                    result.setSuccess(true);
                    return result;
            }
        } catch (Exception e) {
            log.error("Node execution failed: nodeId={}", node.getId(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setErrorDetails(e.toString());
            return result;
        }
    }

    /**
     * 闂傚倷绀佸﹢閬嶆偡閹惰棄骞㈤柍鍝勫€归弶鎼佹⒑閼姐倕鏋戦柣鐔讳含缁骞嬮敐鍐ф睏闂佺鐬奸崑娑㈡⒒椤栫偞鐓忓┑鐐茬仢婵″ジ鎽堕敓鐘斥拺?
     */
    private NodeExecutionResult executeTriggerNode(WorkflowNode node, WorkflowExecutionContext context,
                                                   NodeExecutionResult result) {
        // 闂備浇宕甸崰鎰版偡鏉堚晝涓嶉柟杈剧祷娴滃綊鏌涘畝鈧崑娑㈡⒒椤栫偞鐓忓┑鐐茬仢婵″ジ鎽堕敓鐘斥拺缂備焦锚婵秹鏌嶈閸撴氨绮欓幘缁樺仭婵犻潧顑嗛悡娆戠磼濡や胶鈽夋俊顐弮瀵劍绂掔€ｎ偆鍘遍棅顐㈡处绾板秹鎳欓崷顓犵＝鐎广儱鎷嬮崕鏃堟煛鐏炶姤鍤囩€殿喗濯界粻娑㈠即閻愭浼栭梻鍌欑劍鐎笛呯矙閹达附鍤愭い鏍ㄧ缚娴滃湱鎲告惔銊ョ闁告侗鍙庨悡銉╂煃瑜滈崜娑㈠箯瑜版帗鍋勯悹浣藉劵椤骞忛崨顖涘仒闁斥晛鍟伴崢楣冩⒒娴ｄ警鐒剧紒璇茬墦瀹曟洘娼忛鐘辨睏?
        result.setSuccess(true);
        Map<String, Object> outputData = new HashMap<>();
        outputData.put("triggerType", node.getNodeSubtype() != null ? node.getNodeSubtype() : "MANUAL");
        outputData.put("triggerTime", LocalDateTime.now().toString());
        result.setOutputData(outputData);
        return result;
    }

    /**
     * 闂傚倷绀佸﹢閬嶆偡閹惰棄骞㈤柍鍝勫€归弶鎼佹⒒娴ｅ憡鎯堟い锔诲亰閸┾偓妞ゆ巻鍋撶€殿喖鐖奸、鏇熺鐎ｎ偆鍘鹃梺鍛婄箓鐎氼剟鍩€椤掆偓椤嘲鐣?
     */
    private NodeExecutionResult executeConditionNode(WorkflowNode node, WorkflowExecutionContext context,
                                                     NodeExecutionResult result) {
        Map<String, Object> config = parseConfig(node.getConfigJson());
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) config.getOrDefault("conditions", new ArrayList<>());
        String logic = String.valueOf(config.getOrDefault("logic", "AND"));

        boolean matchResult = evaluateConditions(conditions, context, logic);

        result.setSuccess(true);
        Map<String, Object> outputData = new HashMap<>();
        outputData.put("matched", matchResult);
        outputData.put("selectedBranch", matchResult ? "true" : "false");
        result.setOutputData(outputData);

        // 闂傚倷绀侀幖顐⒚洪妶澶嬪仱闁靛ň鏅涢拑鐔封攽閻樻彃鏆熼柍缁樻煥閳规垿鎮╂潏顐㈠帯闂佽崵鍠愰幑鍥蓟閿熺姴纾兼慨姗嗗幘閻撴垿姊洪悡搴ｄ粵闁搞劌娼￠獮鍡涘礃椤旇偐顦板銈呯箰濡盯骞夐鐣岀闁瑰鍋炵亸銊╂煕鐎ｎ偅灏柍钘夘樀楠炴瑩宕樿閹癸絾绻濈喊妯活潑闁告瑥鍟撮獮?
        context.setNextNodeIds(new ArrayList<>());
        List<WorkflowConnection> outgoing = connectionRepository.findBySourceNodeId(node.getId());

        for (WorkflowConnection conn : outgoing) {
            String label = conn.getLabel();
            if (matchResult && ("true".equals(label) || "YES".equalsIgnoreCase(label) || "DEFAULT".equals(label))) {
                context.getNextNodeIds().add(conn.getTargetNodeId());
            } else if (!matchResult && ("false".equals(label) || "NO".equalsIgnoreCase(label))) {
                context.getNextNodeIds().add(conn.getTargetNodeId());
            }
        }

        return result;
    }

    /**
     * 闂備浇宕垫慨鏉懨洪妶鍥ｅ亾濮橀棿閭€规洩绲剧换婵嬪炊瑜忔鍥⒑閻愯棄鍔氱€殿喖鐖奸、?
     */
    private boolean evaluateConditions(List<Map<String, Object>> conditions, WorkflowExecutionContext context, String logic) {
        return workflowConditionEvaluator.evaluateConditions(
                conditions,
                context == null ? null : context.getTriggerData(),
                context == null ? null : context.getVariables(),
                logic
        );
    }

    /**
     * 闂備浇宕垫慨鏉懨洪妶鍥ｅ亾濮橀棿閭€规洩绲剧换婵嬪炊瑜忛、鍛存⒑閸濆嫭澶勭€光偓閹间礁鍚归悗锝庡枟閻撴稑顭跨捄鐑橆棏闁稿鎹囧畷锝嗗緞濡桨缂?
     */
    private boolean evaluateSingleCondition(Object fieldValue, String operator, Object compareValue) {
        return workflowConditionEvaluator.evaluateSingleCondition(fieldValue, operator, compareValue);
    }

    /**
     * 闂傚倷绀佸﹢閬嶆偡閹惰棄骞㈤柍鍝勫€归弶鎼佹⒒娴ｅ憡鍟為柡宀嬬秮瀹曟繄鈧綆鍓涚粈濠囨煛閸愩劎澧涢柛銈呯Ч閺屾稓浠﹂崜褏鐓€婵?
     */
    /**
     * 閹笛嗩攽閸斻劋缍旈懞鍌滃仯
     */
    private NodeExecutionResult executeActionNode(WorkflowNode node, WorkflowExecutionContext context,
                                                  NodeExecutionResult result) {
        Map<String, Object> config = parseConfig(node.getConfigJson());
        String actionType = node.getNodeSubtype();

        Map<String, Object> output = workflowActionExecutor.execute(actionType, config, context.getVariables());

        result.setSuccess(true);
        result.setOutputData(output);
        return result;
    }

    /**
     * 閹笛嗩攽闁氨鐓￠懞鍌滃仯
     */
    private NodeExecutionResult executeNotificationNode(WorkflowNode node, NodeExecutionResult result) {
        Map<String, Object> config = parseConfig(node.getConfigJson());
        String notificationType = node.getNodeSubtype();

        Map<String, Object> output = workflowNotificationExecutor.execute(notificationType, config);

        result.setSuccess(true);
        result.setOutputData(output);
        return result;
    }

    /**
     * 闂傚倷绀佸﹢閬嶆偡閹惰棄骞㈤柍鍝勫€归弶鎼佹⒑鐠囨煡顎楅柛妯荤矒瀹曟粌鈹戦崶鈺冨骄闂佸湱鍎ら〃鍡涘吹瀹ュ鐓曢悘鐐插⒔閻﹤霉?
     */
    private NodeExecutionResult executeApprovalNode(WorkflowNode node, WorkflowExecutionContext context,
                                                    NodeExecutionResult result, WorkflowExecution execution) {
        Map<String, Object> config = parseConfig(node.getConfigJson());
        String approvalType = node.getNodeSubtype();

        // 闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柛娆忣槺閻濊埖淇婇姘辨癁闁稿鎹囧畷妤佸緞婵犲倵鎷ゅ┑鐘灱椤鏁敓鐘茶摕濠电姴鍋嗛崥瀣煕閵夈垺娅嗛柣?
        ApprovalNode approvalNode = approvalNodeRepository.findByWorkflowNodeId(node.getId())
                .orElse(null);

        if (approvalNode == null) {
            result.setSuccess(false);
            result.setErrorMessage("Approval configuration not found");
            return result;
        }

        Map<String, Object> output = new HashMap<>();
        output.put("approvalId", approvalNode.getId());
        output.put("approvalType", approvalType);
        output.put("status", "PENDING");

        // 闂傚倷绀侀幖顐ょ矓閻戞枻缍栧璺猴功閺嗐倕霉閿濆牄鈧偓闁稿鎹囧畷妤佸緞婵犲倵鎷ゅ┑鐘灱椤鏁悙鐑樺仏闁告挆鍕彴闂佽偐鈷堥崗姗€宕戦幘缁樼叆閻庯綆浜炵粣鐐烘⒑閸涘﹥澶勯柛妯圭矙瀹?
        switch (approvalType) {
            case "SINGLE":
                // 闂傚倷绀侀幉锟犮€冮崱妞曟椽骞嬪顑嫬绶為悘鐐殿焾娴滈箖鏌涜箛鎿冩Ц濠⒀勫絻椤?- 闂傚倷绀侀幖顐ゆ偖椤愶箑纾块柛娆忣槺閻濊埖淇婇姘辨癁闁稿鎹囧畷妤佸緞婵犲倵鎷ゅ┑鐘灱椤鏁Δ鍐焿?
                String approverId = (String) config.getOrDefault("approverId", "");
                if (approverId.isEmpty() && approvalNode.getApproverIds() != null) {
                    approverId = approvalNode.getApproverIds().split(",")[0];
                }
                output.put("approverId", approverId);
                break;

            case "SERIAL":
                // 闂傚倸鍊风欢锟犲磻閸涙潙绀夋俊銈勭贰濞堜粙鏌涢妷銏℃澒闁稿鎹囧畷妤佸緞婵犲倵鎷ゅ┑?
                output.put("approvalOrder", approvalNode.getApproverIds() != null ?
                        approvalNode.getApproverIds().split(",") : new String[]{});
                output.put("currentLevel", 0);
                break;

            case "PARALLEL":
                // 婵犵數鍋炲娆撳触鐎ｎ亶娼╅柕濞炬櫓閺佸棗顪冪€ｎ亞宀搁柛瀣崌瀹曟寰勬繝鍌楁嫟濠?
                String[] approvers = approvalNode.getApproverIds() != null ?
                        approvalNode.getApproverIds().split(",") : new String[]{};
                output.put("approverIds", approvers);
                output.put("requiredApprovals", approvers.length);
                output.put("currentApprovals", 0);
                break;
        }

        // 闂備浇宕垫慨鎶芥倿閿曗偓椤灝螣閼测晝顦悗骞垮劚濞层劍銇欓崘宸唵閻犺櫣灏ㄩ崝鐔虹磼閹板墎绡€闁哄矉绲借灒闁割煈鍠氶崢顐︽⒑?
        context.getVariables().put("approval_" + node.getId(), output);
        result.setSuccess(true);
        result.setWaiting(true);
        result.setOutputData(output);

        return result;
    }

    /**
     * 婵犵數濮伴崹鐓庘枖濞戞埃鍋撳鐓庢珝妤犵偛鍟换婵嬪礋閵娿儰澹曢梺绋跨箰椤︻垱鏅堕幓鎺濈唵閻熸瑥瀚ù顔锯偓瑙勬礀閻栧ジ骞冨▎鎾崇濞达綀顕栭崯鈧?
     */
    @Transactional(timeout = 30)
    public void handleApprovalCallback(String tenantId, String executionId, String nodeId, String action,
                                       String approverId, String comments) {
        WorkflowExecution execution = executionRepository.findByIdAndTenantId(executionId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found"));

        WorkflowExecutionContext context = parseContext(execution);
        WorkflowNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Node not found"));

        Map<String, Object> approvalData = (Map<String, Object>) context.getVariables()
                .getOrDefault("approval_" + nodeId, new HashMap<>());

        if ("APPROVE".equals(action)) {
            approvalData.put("status", ApprovalStatus.APPROVED.name());
            approvalData.put("approverId", approverId);
            approvalData.put("approvedAt", LocalDateTime.now().toString());
            approvalData.put("comments", comments);

            // 缂傚倸鍊搁崐椋庣礊閳ь剟鏌涘☉鍗炵仭闁哄棔鍗冲娲捶椤撶姵鍤勯梺绯曟閺呮粓寮埀顒佺節閻㈤潧浠х紒缁樼箞閹虫宕奸弴鐐靛幒?
            context.setCurrentNodeId(nodeId);
            saveContext(execution, context);
            executeNextNodes(executionId, tenantId);
        } else if ("REJECT".equals(action)) {
            approvalData.put("status", ApprovalStatus.REJECTED.name());
            approvalData.put("approverId", approverId);
            approvalData.put("rejectedAt", LocalDateTime.now().toString());
            approvalData.put("rejectionReason", comments);

            // 闂佽姘﹂～澶愬箖閸洖纾块柟娈垮枤缁€濠囨煛閸屾冻绱╅悷娆忓婵挳鏌熼懖鈺佷粧闁哄鍙冮弻?
            failExecutionInternal(executionId, "Approval rejected by " + approverId, comments, tenantId);
        }
    }

    /**
     * 闂傚倷绀佸﹢閬嶆偡閹惰棄骞㈤柍鍝勫€归弶鍝ョ磽閸屾瑧鍔嶇紒瀣灴钘濇い鏍ㄧ矋椤愪粙鏌ｉ弬鍨倯闁搞倕绉归弻娑氫沪閸撗呯厐婵?
     */
    private NodeExecutionResult executeWaitNode(WorkflowNode node, WorkflowExecutionContext context,
                                                 NodeExecutionResult result) {
        Map<String, Object> config = parseConfig(node.getConfigJson());
        String waitType = node.getNodeSubtype();

        switch (waitType) {
            case "DELAY":
                // 闂佽娴烽崑锝夊磹濞戙垹鏄ラ柡宓本瀵岄梺绋挎湰缁孩銇欓崘宸唵閻犺櫣灏ㄩ崝鐔虹磼?- 闂傚倷绶氬鑽ゆ嫻閻旂厧绀夌€光偓閸曨剙浠鹃梺鍛婃处閸ㄩ亶鍩涢弮鍫熺厱婵炴垵宕獮姗€鏌℃径澶岀煓闁哄矉绲介…銊╁醇椤愶絾顓奸梻浣哄帶閻忔岸宕归崼鏇炵畾闁哄啫鐗嗙猾宥夋煕閵夈劍纭炬繛鍫熺懄缁绘稒娼忛崜褏袣濠电偛顦板ú鐔奉嚕椤愶箑围闁搞儻绲芥禍楣冩偡濞嗗繐顏い锝呭级娣囧﹪鎮欓幓鎺嗗亾閸︻厾鐭夐柟鐑樻⒐鐎氭岸鏌熺紒妯轰刊婵?
                long delaySeconds = Long.parseLong(config.getOrDefault("delaySeconds", "0").toString());
                result.setSuccess(true);
                result.setWaiting(true);
                Map<String, Object> outputData = new HashMap<>();
                outputData.put("waitType", "DELAY");
                outputData.put("delaySeconds", delaySeconds);
                outputData.put("resumeAt", LocalDateTime.now().plusSeconds(delaySeconds).toString());
                result.setOutputData(outputData);
                break;

            case "CONDITION":
                // Condition wait: continue only when condition expressions evaluate to true.
                List<Map<String, Object>> conditions = (List<Map<String, Object>>) config.get("conditions");
                String logic = String.valueOf(config.getOrDefault("logic", "AND"));
                boolean satisfied = evaluateConditions(conditions, context, logic);

                if (satisfied) {
                    result.setSuccess(true);
                    result.setWaiting(false);
                } else {
                    result.setSuccess(true);
                    result.setWaiting(true);
                }
                Map<String, Object> conditionOutput = new HashMap<>();
                conditionOutput.put("conditionSatisfied", satisfied);
                result.setOutputData(conditionOutput);
                break;
        }

        return result;
    }

    /**
     * 闂傚倷绀佸﹢閬嶆偡閹惰棄骞㈤柍鍝勫€归弶鎼佹⒒娴ｄ警鏀版繛澶嬬〒閳ь剚鐭崡鎶藉春閳ь剚銇勯幋鐐差嚋缂佷焦婢橀埞鎴﹀煡閸℃绫嶉梺?
     */
    private NodeExecutionResult executeCcNode(WorkflowNode node, WorkflowExecutionContext context,
                                               NodeExecutionResult result) {
        Map<String, Object> config = parseConfig(node.getConfigJson());
        String ccType = node.getNodeSubtype();

        Map<String, Object> output = new HashMap<>();
        output.put("ccType", ccType);
        output.put("ccUsers", config.getOrDefault("ccUserIds", ""));

        result.setSuccess(true);
        result.setOutputData(output);
        return result;
    }

    /**
     * 闂傚倷绀佸﹢閬嶆偡閹惰棄骞㈤柍鍝勫€归弶鍝ョ磽閸屾艾鈧兘鎮為敃鍌氱閻犲洤妯婇悞鑺ャ亜閹板爼妾柛銈呯Ч閺屾稓浠﹂崜褏鐓€婵?
     */
    private NodeExecutionResult executeEndNode(WorkflowNode node, WorkflowExecutionContext context,
                                               NodeExecutionResult result) {
        result.setSuccess(true);
        Map<String, Object> outputData = new HashMap<>();
        outputData.put("endType", node.getNodeSubtype() != null ? node.getNodeSubtype() : "NORMAL");
        outputData.put("endTime", LocalDateTime.now().toString());
        result.setOutputData(outputData);
        return result;
    }

    /**
     * 闂備浇顕уù鐑藉箠閹捐瀚夋い鎺戝閸ㄥ倹鎱ㄥΟ鎸庣【缂佺姰鍎抽幉鎼佸箣閿旇　鍋?
     */
    @Transactional(timeout = 30)
    public void completeExecution(String executionId) {
        completeExecutionInternal(executionId, null);
    }

    private void completeExecutionInternal(String executionId, String tenantId) {
        WorkflowExecution execution = loadExecution(executionId, tenantId);

        execution.setStatus(WorkflowStatus.COMPLETED.name());
        execution.setCompletedAt(LocalDateTime.now());
        execution.setCurrentNodeId(null);

        // 闂備浇宕垫慨宕囨閵堝洦顫曢柡鍥ュ灪閸嬧晛鈹戦悩瀹犲缂佺姰鍎抽幉鎼佸箣閿旇　鍋撴笟鈧獮瀣晝閳ь剛鐚惧澶嬬厱妞ゆ劧绲剧粈鍐╂叏?
        if (execution.getStartedAt() != null) {
            long durationMs = java.time.Duration.between(
                    execution.getStartedAt(), execution.getCompletedAt()).toMillis();
            execution.setExecutionDurationMs((int) durationMs);
        }

        executionRepository.save(execution);
        activeExecutions.invalidate(executionId);

        log.info("Workflow execution completed: {}", executionId);
    }

    /**
     * 闂傚倷绀侀幖顐ょ矓閺夋嚚娲煛閸滀焦鏅╅梺鎼炲労閸撴瑧绮婚妷褎鍠愰柟杈捐礋閳ь兛绶氶獮瀣倷閼碱剛鐛梺鍝勵槸閻楁粓宕戞径搴澓
     */
    @Transactional(timeout = 30)
    public void failExecution(String executionId, String errorMessage, String errorDetails) {
        failExecutionInternal(executionId, errorMessage, errorDetails, null);
    }

    private void failExecutionInternal(String executionId, String errorMessage, String errorDetails, String tenantId) {
        WorkflowExecution execution = loadExecution(executionId, tenantId);

        execution.setStatus(WorkflowStatus.FAILED.name());
        execution.setCompletedAt(LocalDateTime.now());
        execution.setErrorMessage(errorMessage);
        execution.setErrorDetails(errorDetails);

        if (execution.getStartedAt() != null) {
            long durationMs = java.time.Duration.between(
                    execution.getStartedAt(), execution.getCompletedAt()).toMillis();
            execution.setExecutionDurationMs((int) durationMs);
        }

        executionRepository.save(execution);
        activeExecutions.invalidate(executionId);

        log.error("Workflow execution failed: {} - {}", executionId, errorMessage);
    }

    /**
     * 闂傚倷绀侀幉锟犳偡閿曞倹鍋嬫俊銈呭暟閻捇鏌ｉ幋锝嗩棄缂佺姰鍎抽幉鎼佸箣閿旇　鍋?
     */
    @Transactional(timeout = 30)
    public void cancelExecution(String tenantId, String executionId, String cancelledBy) {
        WorkflowExecution execution = executionRepository.findByIdAndTenantId(executionId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found"));

        if (!WorkflowStatus.isRunning(execution.getStatus())) {
            throw new IllegalStateException("Cannot cancel execution with status: " + execution.getStatus());
        }

        execution.setStatus(WorkflowStatus.CANCELLED.name());
        execution.setCompletedAt(LocalDateTime.now());
        execution.setErrorMessage("Cancelled by " + cancelledBy);

        if (execution.getStartedAt() != null) {
            long durationMs = java.time.Duration.between(
                    execution.getStartedAt(), execution.getCompletedAt()).toMillis();
            execution.setExecutionDurationMs((int) durationMs);
        }

        executionRepository.save(execution);
        activeExecutions.invalidate(executionId);

        log.info("Workflow execution cancelled: {} by {}", executionId, cancelledBy);
    }

    /**
     * 闂傚倷绀侀崥瀣磿閹惰棄搴婇柤鑹扮堪娴滃綊鏌涢妷顔煎缂佺姰鍎抽幉鎼佸箣閿旇　鍋撴笟鈧獮瀣偐閸愯尙褰撮梻浣瑰濡礁螞閸曨垰绀?
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getExecutionDetail(String tenantId, String executionId) {
        WorkflowExecution execution = executionRepository.findByIdAndTenantId(executionId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found"));

        Map<String, Object> detail = new HashMap<>();
        detail.put("execution", execution);

        // 闂備浇宕甸崰鎰版偡鏉堚晛绶ゅΔ锝呭暞閸婄敻鏌ら幁鎺戝姢闁崇粯鏌ㄩ埞鎴︽偐鏉堫偄鍘￠梺鑽ゅ枑閹瑰洭寮?
        WorkflowExecutionContext context = parseContext(execution);
        detail.put("context", context);

        // 闂傚倷绀侀崥瀣磿閹惰棄搴婇柤鑹扮堪娴滃綊鏌涢妷顔煎闁搞倕绉归弻娑氫沪閸撗呯厐婵炲濯崹宕囨閹惧瓨濯撮悷娆忓闂夊秹姊?
        if (context != null && context.getNodeResults() != null) {
            detail.put("nodeResults", context.getNodeResults());
        }

        // 闂傚倷绀侀崥瀣磿閹惰棄搴婇柤鑹扮堪娴滃綊鏌涢妷鎴濊嫰閺呯娀姊洪棃娑辨Ф闁稿﹥顨堢划娆愮節閸愵亞顔曠紓鍌氱墢婵敻藝瀹勬墥搴ㄥ炊瑜濋煬顒侇殽?
        WorkflowDefinition workflow = workflowRepository.findByIdAndTenantId(execution.getWorkflowId(), tenantId).orElse(null);
        detail.put("workflow", workflow);

        return detail;
    }

    /**
     * 闂傚倷绀侀崥瀣磿閹惰棄搴婇柤鑹扮堪娴滃綊鏌涢妷顔煎缂佺姰鍎抽幉鎼佸箣閿旇　鍋撴笟鈧獮瀣晝閳ь剟宕掗妸鈺傜厱闁斥晛鍠氬▓鏃堟煢?
     */
    @Transactional(readOnly = true)
    public List<WorkflowExecution> getExecutionHistory(String tenantId, String workflowId, String status, int page, int size) {
        workflowRepository.findByIdAndTenantId(workflowId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found"));
        Pageable pageable = PageRequest.of(page, size);
        if (status != null && !status.isEmpty()) {
            return executionRepository.findByWorkflowIdAndStatusOrderByStartedAtDesc(workflowId, status, pageable);
        }
        return executionRepository.findByWorkflowIdOrderByStartedAtDesc(workflowId, pageable);
    }

    /**
     * 闂傚倸鍊烽悞锕併亹閸愵亞鐭撻柣鎴ｅГ閸庡﹥銇勯弽銊р槈缂佸墎鍋ゅ鍫曞醇椤愵澀鍑介悶姘箞濮婂搫效閸パ冾瀳闁诲孩鍑归崢浠嬪疾閸洘鍋╅悘鐐村劤娴?
     */
    @Transactional(timeout = 30)
    public WorkflowExecution retryExecution(String tenantId, String executionId) {
        WorkflowExecution oldExecution = executionRepository.findByIdAndTenantId(executionId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found"));

        if (!"FAILED".equals(oldExecution.getStatus())) {
            throw new IllegalStateException("Can only retry failed executions");
        }

        // 闂傚倷绀侀幉锛勬暜濡ゅ啰鐭欓柟瀵稿Х绾句粙鏌熼幑鎰靛殭婵☆偅锕㈤弻鐔封枔閸喗鐏嶉梺浼欑秮娴滃爼寮婚悢铏圭當婵炴垶蓱閿涘秹鏌￠埀?
        return startExecution(
                tenantId,
                oldExecution.getWorkflowId(),
                oldExecution.getTriggerType(),
                oldExecution.getTriggerSource(),
                oldExecution.getTriggerPayload(),
                null
        );
    }

    // ========== Helper Methods ==========

    private Map<String, Object> parseConfig(String configJson) {
        if (configJson == null || configJson.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(configJson, Map.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse config JSON", e);
            return new HashMap<>();
        }
    }

    private WorkflowExecutionContext getContext(WorkflowExecution execution) {
        return activeExecutions.getIfPresent(execution.getId());
    }

    private WorkflowExecutionContext parseContext(WorkflowExecution execution) {
        if (execution.getExecutionContext() == null || execution.getExecutionContext().isEmpty()) {
            return new WorkflowExecutionContext();
        }
        try {
            return objectMapper.readValue(execution.getExecutionContext(), WorkflowExecutionContext.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse context JSON", e);
            return new WorkflowExecutionContext();
        }
    }

    private void saveContext(WorkflowExecution execution, WorkflowExecutionContext context) {
        try {
            execution.setExecutionContext(objectMapper.writeValueAsString(context));
            executionRepository.save(execution);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize context", e);
        }
    }

    private WorkflowExecution loadExecution(String executionId, String tenantId) {
        if (tenantId != null) {
            return executionRepository.findByIdAndTenantId(executionId, tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));
        }
        return executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));
    }

    /**
     * 闂傚倷绀佸﹢閬嶆偡閹惰棄骞㈤柍鍝勫€归弶绋库攽閻愭潙鐏﹂柟鍛婃倐閺佸啴鏁愰崶鈺冪暥闂佸憡绋掑娆徫涘鈧弻娑㈠Ψ閹存繂鏆為柡鍡愬劦濮婄粯绗熼崶褎鐏侀梺鍛婃煥濞寸兘骞?
     */
    public static class WorkflowExecutionContext {
        private String executionId;
        private String workflowId;
        private String currentNodeId;
        private List<String> nextNodeIds;
        private Map<String, Object> triggerData;
        private Map<String, Object> variables;
        private Map<String, NodeExecutionResult> nodeResults;
        private LocalDateTime startedAt;

        public String getExecutionId() { return executionId; }
        public void setExecutionId(String executionId) { this.executionId = executionId; }

        public String getWorkflowId() { return workflowId; }
        public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

        public String getCurrentNodeId() { return currentNodeId; }
        public void setCurrentNodeId(String currentNodeId) { this.currentNodeId = currentNodeId; }

        public List<String> getNextNodeIds() {
            if (nextNodeIds == null) nextNodeIds = new ArrayList<>();
            return nextNodeIds;
        }
        public void setNextNodeIds(List<String> nextNodeIds) { this.nextNodeIds = nextNodeIds; }

        public Map<String, Object> getTriggerData() {
            if (triggerData == null) triggerData = new HashMap<>();
            return triggerData;
        }
        public void setTriggerData(Map<String, Object> triggerData) { this.triggerData = triggerData; }

        public Map<String, Object> getVariables() {
            if (variables == null) variables = new HashMap<>();
            return variables;
        }
        public void setVariables(Map<String, Object> variables) { this.variables = variables; }

        public Map<String, NodeExecutionResult> getNodeResults() {
            if (nodeResults == null) nodeResults = new HashMap<>();
            return nodeResults;
        }
        public void setNodeResults(Map<String, NodeExecutionResult> nodeResults) { this.nodeResults = nodeResults; }

        public LocalDateTime getStartedAt() { return startedAt; }
        public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    }

    /**
     * 闂傚倷鑳堕崢褔骞栭锕€纾瑰┑鐘宠壘绾惧鏌ㄥ┑鍡╂Ц缂佺姰鍎抽幉鎼佸箣閿旇　鍋撴笟鈧獮瀣偐閻㈢绱查梻浣筋潐閸庢娊顢氶鐔侯洸妞ゆ牜鍋為悡鏇㈡煙闁箑澧柛銈呮喘閺屾稒鎯旈埥鍡楁闂?
     */
    public static class NodeExecutionResult {
        private String nodeId;
        private String nodeType;
        private String nodeSubtype;
        private boolean success;
        private boolean waiting;
        private String errorMessage;
        private String errorDetails;
        private Map<String, Object> outputData;

        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }

        public String getNodeType() { return nodeType; }
        public void setNodeType(String nodeType) { this.nodeType = nodeType; }

        public String getNodeSubtype() { return nodeSubtype; }
        public void setNodeSubtype(String nodeSubtype) { this.nodeSubtype = nodeSubtype; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public boolean isWaiting() { return waiting; }
        public void setWaiting(boolean waiting) { this.waiting = waiting; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public String getErrorDetails() { return errorDetails; }
        public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }

        public Map<String, Object> getOutputData() { return outputData; }
        public void setOutputData(Map<String, Object> outputData) { this.outputData = outputData; }
    }
}

