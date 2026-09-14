package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.TriageMatchLevel;
import edu.seu.vcampus.common.hospital.TriageRecommendationView;
import edu.seu.vcampus.common.hospital.TriageRequest;
import edu.seu.vcampus.common.hospital.TriageResultView;
import edu.seu.vcampus.common.hospital.TriageFollowUpAnswer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Deterministic department guidance using only local, explainable keyword rules. */
final class HospitalTriageEngine {

    private final HospitalRedFlagEngine redFlagEngine = new HospitalRedFlagEngine();

    private static final List<DepartmentRule> RULES = List.of(
            rule("dept-respiratory",
                    group(3, "描述中有咳嗽、咽部或发热等呼吸系统相关不适",
                            "咳嗽", "咽痛", "喉咙痛", "鼻塞", "流鼻涕", "发热", "发烧"),
                    group(3, "描述中有气喘、胸闷等呼吸相关表现",
                            "气喘", "喘不上气", "胸闷", "呼吸不畅")),
            rule("dept-gastroenterology",
                    group(3, "描述中有腹部、胃部或消化相关不适",
                            "腹痛", "肚子痛", "胃痛", "胃胀", "反酸", "消化不良"),
                    group(3, "描述中有恶心、呕吐或排便异常",
                            "恶心", "呕吐", "腹泻", "拉肚子", "便秘", "便血")),
            rule("dept-cardiovascular",
                    group(4, "描述中有心跳、心律或心前区相关不适",
                            "心慌", "心悸", "心跳快", "心跳乱", "心律不齐", "心前区不适"),
                    group(2, "描述中提到血压或脉搏异常",
                            "血压高", "血压低", "血压不稳", "脉搏快")),
            rule("dept-general-surgery",
                    group(4, "描述中有伤口、包块或普通外科相关不适",
                            "伤口", "切口", "皮下肿块", "包块", "脓肿", "疝气"),
                    group(3, "描述中提到甲状腺或乳房包块",
                            "甲状腺肿大", "甲状腺结节", "乳房肿块", "乳房包块")),
            rule("dept-joint-surgery",
                    group(3, "描述中有骨骼或关节部位不适",
                            "关节痛", "骨头痛", "骨折", "膝盖痛", "肩膀痛", "腰痛", "颈椎"),
                    group(2, "描述中有持续活动受限或关节肿胀",
                            "活动受限", "关节肿", "走路疼")),
            rule("dept-sports-medicine",
                    group(4, "描述中有运动导致的扭伤、拉伤或软组织不适",
                            "运动受伤", "打球受伤", "扭伤", "拉伤", "崴脚", "韧带", "肌肉拉伤")),
            rule("dept-psychology",
                    group(3, "描述中有情绪、压力、焦虑或睡眠方面的困扰",
                            "焦虑", "情绪低落", "压力大", "紧张", "失眠", "睡不着", "抑郁"),
                    group(2, "描述中有持续的注意力或情绪调节困扰",
                            "注意力", "情绪波动", "容易哭", "恐慌")),
            rule("dept-dental",
                    group(4, "描述中有牙齿、牙龈或口腔相关不适",
                            "牙痛", "牙疼", "牙龈", "牙齿", "智齿", "口腔溃疡", "口腔")),
            rule("dept-eye",
                    group(4, "描述中有眼部或视力相关不适",
                            "眼睛痛", "眼痛", "眼睛红", "红眼", "视力模糊", "看不清", "眼睛干", "眼痒")),
            rule("dept-ent",
                    group(4, "描述中有耳部、鼻部或咽喉相关不适",
                            "耳痛", "耳鸣", "听不清", "听力下降", "鼻出血", "流鼻血",
                            "鼻窦", "声音嘶哑", "扁桃体")),
            rule("dept-dermatology",
                    group(4, "描述中有皮肤、皮疹或瘙痒相关不适",
                            "皮疹", "红疹", "皮肤痒", "瘙痒", "湿疹", "荨麻疹", "痘痘", "脱发")),
            rule("dept-gynecology",
                    group(4, "描述中有月经或妇科相关不适",
                            "月经不调", "月经推迟", "经期提前", "痛经", "白带异常", "妇科")),
            rule("dept-tcm",
                    group(4, "明确希望由中医科进行评估",
                            "中医调理", "看中医", "中医问诊", "针灸评估", "中药咨询")),
            rule("dept-rehabilitation",
                    group(4, "描述中有术后或损伤后的康复评估需求",
                            "术后康复", "损伤康复", "康复评估", "功能训练", "恢复训练"),
                    group(3, "明确希望咨询理疗或长期功能恢复",
                            "康复理疗", "理疗", "物理治疗")));

    TriageResultView triage(
            TriageRequest request,
            List<HospitalDepartment> activeDepartments) {
        String description = conversationText(request);
        Optional<HospitalRedFlagEngine.RedFlagAlert> redFlag =
                redFlagEngine.evaluate(request);
        if (redFlag.isPresent()) {
            return new TriageResultView(
                    true,
                    "描述中出现" + redFlag.orElseThrow().patientReason()
                            + "，不适合等待普通预约。"
                            + "请立即寻求线下医护人员帮助；"
                            + "情况危急时联系当地急救服务。",
                    List.of());
        }

        Map<String, HospitalDepartment> bookableDepartments = new LinkedHashMap<>();
        activeDepartments.stream()
                .filter(HospitalDepartment::bookable)
                .forEach(department -> bookableDepartments.put(
                        department.departmentId(), department));
        List<ScoredDepartment> scored = new ArrayList<>();
        for (DepartmentRule rule : RULES) {
            HospitalDepartment department = bookableDepartments.get(rule.departmentId());
            if (department == null) {
                continue;
            }
            int score = 0;
            List<String> reasons = new ArrayList<>();
            for (KeywordGroup group : rule.groups()) {
                if (containsAny(description, group.keywords())) {
                    score += group.score();
                    reasons.add(group.reason());
                }
            }
            if (score > 0) {
                scored.add(new ScoredDepartment(department, score, reasons));
            }
        }
        scored.sort(Comparator.comparingInt(ScoredDepartment::score).reversed()
                .thenComparing(item -> item.department().departmentName()));
        List<TriageRecommendationView> recommendations = scored.stream()
                .limit(3)
                .map(this::toRecommendation)
                .toList();
        if (recommendations.isEmpty()) {
            HospitalDepartment general = bookableDepartments.get("dept-general");
            if (general != null) {
                recommendations = List.of(new TriageRecommendationView(
                        general.departmentId(),
                        general.departmentName(),
                        TriageMatchLevel.GENERAL,
                        List.of("现有描述没有明确指向单一专科，可先由全科医生进行初步评估")));
            }
        }
        return new TriageResultView(
                false,
                "以下建议仅用于选择就诊科室，不构成疾病诊断或治疗意见。",
                recommendations);
    }

    private TriageRecommendationView toRecommendation(ScoredDepartment item) {
        TriageMatchLevel level = item.score() >= 6
                ? TriageMatchLevel.HIGH : TriageMatchLevel.MEDIUM;
        return new TriageRecommendationView(
                item.department().departmentId(),
                item.department().departmentName(),
                level,
                item.reasons());
    }

    private static DepartmentRule rule(String departmentId, KeywordGroup... groups) {
        return new DepartmentRule(departmentId, List.of(groups));
    }

    private static KeywordGroup group(int score, String reason, String... keywords) {
        return new KeywordGroup(score, reason, List.of(keywords));
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private static String conversationText(TriageRequest request) {
        StringBuilder text = new StringBuilder(request.getSymptomDescription());
        for (TriageFollowUpAnswer answer : request.getFollowUpAnswers()) {
            text.append(' ').append(answer.getAnswer());
        }
        return normalize(text.toString());
    }

    private static boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    private record DepartmentRule(String departmentId, List<KeywordGroup> groups) {
    }

    private record KeywordGroup(int score, String reason, List<String> keywords) {
    }

    private record ScoredDepartment(
            HospitalDepartment department,
            int score,
            List<String> reasons) {
    }
}
