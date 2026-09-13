package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.TriageFollowUpAnswer;
import edu.seu.vcampus.common.hospital.TriageFollowUpOptionView;
import edu.seu.vcampus.common.hospital.TriageRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Server-owned, patient-readable safety questions shown before free-form AI questions. */
final class HospitalSafetyQuestionCatalog {

    private static final List<SafetyQuestion> QUESTIONS = List.of(
            question("safety_breathing", "你现在呼吸是否明显费力？",
                    List.of("咳嗽", "咳痰", "发烧", "发热", "喘", "胸闷", "呼吸", "感冒"),
                    option("normal", "没有，能够正常说话"),
                    option("mild", "有一点费力，但能完整说话"),
                    option("severe", "很费力，说话需要停下来喘气"),
                    option("unknown", "不确定")),
            question("safety_temperature", "你最近一次测得的体温在哪个范围？",
                    List.of("发烧", "发热", "高热", "体温"),
                    option("below_385", "低于 38.5℃"),
                    option("from_385_to_404", "38.5℃至 40.4℃"),
                    option("at_least_405", "40.5℃及以上"),
                    option("not_measured", "还没有测量")),
            question("safety_chest_pain", "胸口疼痛或不适是否已持续两分钟以上？",
                    List.of("胸痛", "胸口痛", "心口痛", "胸部不适"),
                    option("under_two_minutes", "没有，很快就缓解了"),
                    option("over_two_minutes", "是，已持续两分钟以上"),
                    option("worsening", "正在加重"),
                    option("unknown", "不确定")),
            question("safety_neurological", "请选择现在突然出现的情况。",
                    List.of("头痛", "头晕", "嘴歪", "说话", "手脚无力", "看不清", "视力"),
                    option("none", "以上情况都没有"),
                    option("one_side_weak", "一边胳膊或腿突然抬不起来"),
                    option("face_or_speech", "嘴角突然歪斜或说话含糊"),
                    option("sudden_vision_loss", "一只或两只眼睛突然看不见"),
                    option("unknown", "不确定")),
            question("safety_allergy", "过敏后是否出现以下情况？",
                    List.of("过敏", "皮疹", "荨麻疹", "嘴唇肿", "舌头肿"),
                    option("none", "没有这些情况"),
                    option("mouth_swelling", "嘴唇、舌头或喉咙突然肿起来"),
                    option("breathing_or_swallowing", "呼吸或吞咽明显困难"),
                    option("unknown", "不确定")),
            question("safety_abdominal", "请选择最符合现在腹部不适的情况。",
                    List.of("腹痛", "肚子痛", "胃痛", "呕吐", "腹泻", "拉肚子"),
                    option("not_severe", "不是突然剧痛，也没有持续呕吐"),
                    option("sudden_severe_pain", "突然发生并且疼得非常厉害"),
                    option("persistent_vomiting", "一直呕吐，无法停下"),
                    option("vomiting_blood", "呕吐物中有鲜红或咖啡色血样内容"),
                    option("unknown", "不确定")),
            question("safety_injury", "这次受伤后是否出现以下情况？",
                    List.of("受伤", "摔伤", "撞伤", "出血", "烧伤", "烫伤"),
                    option("minor", "都没有，只是局部疼痛或肿胀"),
                    option("uncontrolled_bleeding", "出血很多，按压后仍停不下来"),
                    option("head_neck_spine", "头、颈部或背部受到严重撞击"),
                    option("large_burn", "烧伤或烫伤面积很大"),
                    option("unknown", "不确定")));

    Optional<SafetyQuestion> nextQuestion(TriageRequest request) {
        Set<String> answered = request.getFollowUpAnswers().stream()
                .filter(TriageFollowUpAnswer::hasStructuredAnswer)
                .filter(this::isKnownAnswer)
                .map(TriageFollowUpAnswer::getQuestionId)
                .collect(Collectors.toSet());
        String text = conversationText(request);
        return QUESTIONS.stream()
                .filter(question -> !answered.contains(question.questionId()))
                .filter(question -> containsAny(text, question.triggers()))
                .findFirst();
    }

    Optional<String> redFlagReason(TriageFollowUpAnswer answer) {
        if (!answer.hasStructuredAnswer()) {
            return Optional.empty();
        }
        Map<String, String> dangerousAnswers = DANGEROUS_ANSWERS.get(answer.getQuestionId());
        if (dangerousAnswers == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(dangerousAnswers.get(answer.getAnswerId()));
    }

    boolean isKnownAnswer(TriageFollowUpAnswer answer) {
        return QUESTIONS.stream()
                .filter(question -> question.questionId().equals(answer.getQuestionId()))
                .flatMap(question -> question.options().stream())
                .anyMatch(option -> option.getOptionId().equals(answer.getAnswerId()));
    }

    private static final Map<String, Map<String, String>> DANGEROUS_ANSWERS =
            dangerousAnswers();

    private static Map<String, Map<String, String>> dangerousAnswers() {
        Map<String, Map<String, String>> answers = new LinkedHashMap<>();
        answers.put("safety_breathing", Map.of(
                "severe", "严重呼吸费力的危险信号"));
        answers.put("safety_temperature", Map.of(
                "at_least_405", "体温达到需要立即就医的高热范围"));
        answers.put("safety_chest_pain", Map.of(
                "over_two_minutes", "持续胸痛或胸部不适的危险信号",
                "worsening", "正在加重的胸痛或胸部不适"));
        answers.put("safety_neurological", Map.of(
                "one_side_weak", "突发单侧肢体活动异常",
                "face_or_speech", "突发面部或语言异常",
                "sudden_vision_loss", "突发视力丧失"));
        answers.put("safety_allergy", Map.of(
                "mouth_swelling", "唇、舌或咽喉突然肿胀的危险信号",
                "breathing_or_swallowing", "过敏后呼吸或吞咽明显困难"));
        answers.put("safety_abdominal", Map.of(
                "sudden_severe_pain", "突发剧烈腹痛",
                "persistent_vomiting", "持续无法停止的呕吐",
                "vomiting_blood", "呕血的危险信号"));
        answers.put("safety_injury", Map.of(
                "uncontrolled_bleeding", "无法停止的大量出血",
                "head_neck_spine", "严重头颈或脊柱创伤",
                "large_burn", "大面积烧伤或烫伤"));
        return Map.copyOf(answers);
    }

    private static String conversationText(TriageRequest request) {
        StringBuilder text = new StringBuilder(request.getSymptomDescription());
        request.getFollowUpAnswers().stream()
                .filter(answer -> !answer.hasStructuredAnswer())
                .forEach(answer -> text.append(' ').append(answer.getAnswer()));
        return normalize(text.toString());
    }

    private static boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private static SafetyQuestion question(
            String id,
            String prompt,
            List<String> triggers,
            TriageFollowUpOptionView... options) {
        return new SafetyQuestion(id, prompt, triggers, List.of(options));
    }

    private static TriageFollowUpOptionView option(String id, String label) {
        return new TriageFollowUpOptionView(id, label);
    }

    record SafetyQuestion(
            String questionId,
            String prompt,
            List<String> triggers,
            List<TriageFollowUpOptionView> options) {
    }
}
