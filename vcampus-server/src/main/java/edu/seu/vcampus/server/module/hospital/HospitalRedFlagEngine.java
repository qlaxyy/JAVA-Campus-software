package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.TriageFollowUpAnswer;
import edu.seu.vcampus.common.hospital.TriageRequest;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deterministic red-flag screening; it identifies danger signals, not diseases. */
final class HospitalRedFlagEngine {

    private final HospitalSafetyQuestionCatalog safetyQuestions =
            new HospitalSafetyQuestionCatalog();

    private static final double DANGEROUS_TEMPERATURE_CELSIUS = 40.5;
    private static final Pattern TEMPERATURE_WITH_UNIT = Pattern.compile(
            "(?:体温|温度)?(\\d{2}(?:\\.\\d)?)(?:摄氏度|度|℃|°c)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBER_ONLY = Pattern.compile(
            "^\\D*(\\d{2}(?:\\.\\d)?)\\D*$");
    private static final List<String> NEGATION_PREFIXES = List.of(
            "没有", "并无", "并没有", "否认", "未出现", "不伴", "不是",
            "无", "没", "未");
    private static final List<String> AFFIRMATIVE_ANSWERS = List.of(
            "有", "有的", "是", "是的", "对", "出现了", "存在");
    private static final List<RedFlagRule> RULES = List.of(
            rule("呼吸方面的危险信号",
                    "严重呼吸困难", "呼吸困难", "无法呼吸", "喘不过气",
                    "呼吸很困难", "窒息", "嘴唇发紫", "口唇发紫"),
            rule("胸痛、晕厥等循环方面的危险信号",
                    "持续胸痛", "剧烈胸痛", "胸部压榨痛", "胸痛超过两分钟",
                    "晕厥", "失去意识", "心脏骤停"),
            rule("意识、语言或肢体活动方面的危险信号",
                    "意识不清", "昏迷", "无法唤醒", "抽搐", "一侧肢体无力",
                    "单侧肢体无力", "半边身体无力", "口角歪斜", "嘴歪",
                    "言语不清", "说话含糊", "无法说话", "突然失明", "突然看不见"),
            rule("严重出血或创伤方面的危险信号",
                    "大量出血", "止不住血", "出血不止", "呕血", "咳血",
                    "头部重伤", "颈部重伤", "脊柱受伤", "大面积烧伤",
                    "严重烧伤", "溺水"),
            rule("严重过敏方面的危险信号",
                    "严重过敏", "喉咙肿胀", "咽喉肿胀", "舌头肿胀",
                    "嘴唇突然肿胀", "口唇突然肿胀"),
            rule("腹部疼痛或持续呕吐方面的危险信号",
                    "突然剧烈腹痛", "剧烈腹痛", "严重腹痛", "持续呕吐",
                    "呕吐不止", "严重腹泻", "腹泻不止"),
            rule("中毒或过量服药的危险信号",
                    "误食毒物", "服用过量药物", "药物过量", "食物中毒",
                    "农药中毒", "一氧化碳中毒", "吸入浓烟"),
            rule("自伤或伤害他人的紧急风险",
                    "想自杀", "准备自杀", "自杀计划", "想伤害自己",
                    "想伤害别人", "想杀人"),
            rule("突发剧烈疼痛的危险信号",
                    "突然剧烈头痛", "突发剧烈头痛", "突然剧痛"));

    Optional<RedFlagAlert> evaluate(TriageRequest request) {
        if (request.hasUrgentConcern()) {
            return Optional.of(new RedFlagAlert("患者主动表示可能存在紧急情况"));
        }
        Optional<RedFlagAlert> original = evaluateText(request.getSymptomDescription());
        if (original.isPresent()) {
            return original;
        }
        if (containsDangerousTemperature(request.getSymptomDescription(), false)) {
            return Optional.of(new RedFlagAlert("体温达到需要立即就医的高热范围"));
        }
        for (TriageFollowUpAnswer followUp : request.getFollowUpAnswers()) {
            Optional<String> structuredReason = safetyQuestions.redFlagReason(followUp);
            if (structuredReason.isPresent()) {
                return Optional.of(new RedFlagAlert(structuredReason.orElseThrow()));
            }
            if (isTemperatureQuestion(followUp.getQuestion())
                    && containsDangerousTemperature(followUp.getAnswer(), true)) {
                return Optional.of(new RedFlagAlert("体温达到需要立即就医的高热范围"));
            }
            Optional<RedFlagAlert> answerAlert = evaluateText(followUp.getAnswer());
            if (answerAlert.isPresent()) {
                return answerAlert;
            }
            if (isAffirmative(followUp.getAnswer())) {
                Optional<RedFlagAlert> confirmedQuestion = evaluateConfirmedQuestion(
                        followUp.getQuestion());
                if (confirmedQuestion.isPresent()) {
                    return confirmedQuestion;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<RedFlagAlert> evaluateText(String rawText) {
        String text = normalize(rawText);
        for (RedFlagRule rule : RULES) {
            if (rule.phrases().stream().anyMatch(phrase -> containsUnnegated(text, phrase))) {
                return Optional.of(new RedFlagAlert(rule.patientReason()));
            }
        }
        return Optional.empty();
    }

    private static Optional<RedFlagAlert> evaluateConfirmedQuestion(String rawQuestion) {
        String question = normalize(rawQuestion)
                .replace("有没有", "")
                .replace("是否", "")
                .replace("有无", "");
        for (RedFlagRule rule : RULES) {
            if (rule.phrases().stream().anyMatch(question::contains)) {
                return Optional.of(new RedFlagAlert(rule.patientReason()));
            }
        }
        return Optional.empty();
    }

    private static boolean containsUnnegated(String text, String phrase) {
        int searchFrom = 0;
        while (searchFrom < text.length()) {
            int index = text.indexOf(phrase, searchFrom);
            if (index < 0) {
                return false;
            }
            String prefix = text.substring(Math.max(0, index - 6), index);
            boolean negated = NEGATION_PREFIXES.stream().anyMatch(prefix::endsWith);
            if (!negated) {
                return true;
            }
            searchFrom = index + phrase.length();
        }
        return false;
    }

    private static boolean isTemperatureQuestion(String question) {
        String normalized = normalize(question);
        return List.of("体温", "温度", "多少度", "发烧", "发热").stream()
                .anyMatch(normalized::contains);
    }

    private static boolean containsDangerousTemperature(
            String rawText,
            boolean allowNumberOnly) {
        String text = normalize(rawText);
        Matcher matcher = TEMPERATURE_WITH_UNIT.matcher(text);
        while (matcher.find()) {
            if (Double.parseDouble(matcher.group(1)) >= DANGEROUS_TEMPERATURE_CELSIUS) {
                return true;
            }
        }
        if (!allowNumberOnly) {
            return false;
        }
        Matcher numberOnly = NUMBER_ONLY.matcher(text);
        return numberOnly.matches()
                && Double.parseDouble(numberOnly.group(1)) >= DANGEROUS_TEMPERATURE_CELSIUS;
    }

    private static boolean isAffirmative(String answer) {
        return AFFIRMATIVE_ANSWERS.contains(normalize(answer));
    }

    private static String normalize(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT)
                        .replaceAll("[\\s。，,！!？?]", "");
    }

    private static RedFlagRule rule(String reason, String... phrases) {
        return new RedFlagRule(reason, List.of(phrases));
    }

    record RedFlagAlert(String patientReason) {
    }

    private record RedFlagRule(String patientReason, List<String> phrases) {
    }
}
