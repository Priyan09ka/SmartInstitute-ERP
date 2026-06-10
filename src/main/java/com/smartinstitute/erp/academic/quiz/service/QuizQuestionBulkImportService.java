package com.smartinstitute.erp.academic.quiz.service;

import com.smartinstitute.erp.academic.quiz.dto.QuizQuestionImportResultDto;
import com.smartinstitute.erp.academic.quiz.entity.Quiz;
import com.smartinstitute.erp.academic.quiz.entity.QuizQuestion;
import com.smartinstitute.erp.academic.quiz.repository.QuizQuestionRepository;
import com.smartinstitute.erp.academic.quiz.repository.QuizRepository;
import com.smartinstitute.erp.academic.teacher.service.TeacherPermissionService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class QuizQuestionBulkImportService {

    private static final int MAX_ERROR_LINES = 40;
    private static final Pattern LINE_SPLIT = Pattern.compile("\\r?\\n");

    private final QuizQuestionRepository questionRepository;
    private final QuizRepository quizRepository;
    private final TeacherPermissionService teacherPermissionService;

    @Transactional
    public QuizQuestionImportResultDto importFromCsv(Long quizId, MultipartFile file) {
        requireFile(file);
        Quiz quiz = loadQuizAndAssert(quizId);
        List<String> errors = new ArrayList<>();
        List<QuizQuestion> toSave = new ArrayList<>();

        try (var reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .setIgnoreEmptyLines(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                String label = "Row " + record.getRecordNumber();
                try {
                    Map<String, String> norm = normalizedRowMap(record);
                    QuizQuestion q = buildFromNamedColumns(norm, quiz, label, errors);
                    if (q != null) {
                        toSave.add(q);
                    }
                } catch (Exception e) {
                    addError(errors, label + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not read CSV: " + e.getMessage());
        }

        questionRepository.saveAll(toSave);
        return QuizQuestionImportResultDto.builder()
                .importedCount(toSave.size())
                .failedCount(errors.size())
                .errors(errors)
                .build();
    }

    /**
     * Extracts text from PDF, then treats each non-empty line as one CSV row (same column order as CSV without header):
     * question,optionA,optionB,optionC,optionD,correctAnswer[,marks]
     * If the first parsed row looks like a header (e.g. first cell is "question"), it is skipped.
     */
    @Transactional
    public QuizQuestionImportResultDto importFromPdf(Long quizId, MultipartFile file) {
        requireFile(file);
        Quiz quiz = loadQuizAndAssert(quizId);
        List<String> errors = new ArrayList<>();
        List<QuizQuestion> toSave = new ArrayList<>();

        String text;
        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            text = stripper.getText(doc);
        } catch (Exception e) {
            throw new RuntimeException("Could not read PDF: " + e.getMessage());
        }

        String[] rawLines = LINE_SPLIT.split(text);
        boolean firstData = true;
        int lineNum = 0;
        for (String raw : rawLines) {
            lineNum++;
            if (raw == null || raw.isBlank()) {
                continue;
            }
            List<String> cells = parseOneCsvLine(raw.trim());
            if (cells.size() < 6) {
                addError(errors, "Line " + lineNum + ": need at least 6 columns (question through correctAnswer)");
                continue;
            }
            if (firstData && looksLikeHeaderRow(cells)) {
                firstData = false;
                continue;
            }
            firstData = false;
            String label = "PDF line " + lineNum;
            try {
                QuizQuestion q = buildFromPositionalCells(cells, quiz, label, errors);
                if (q != null) {
                    toSave.add(q);
                }
            } catch (Exception e) {
                addError(errors, label + ": " + e.getMessage());
            }
        }

        questionRepository.saveAll(toSave);
        return QuizQuestionImportResultDto.builder()
                .importedCount(toSave.size())
                .failedCount(errors.size())
                .errors(errors)
                .build();
    }

    private static void requireFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is required");
        }
    }

    private Quiz loadQuizAndAssert(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        teacherPermissionService.assertCanManageQuiz(
                quiz.getSubject().getId(), quiz.getClassroom().getId());
        return quiz;
    }

    private Map<String, String> normalizedRowMap(CSVRecord record) {
        Map<String, String> norm = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : record.toMap().entrySet()) {
            if (e.getKey() == null) {
                continue;
            }
            norm.put(normalizeHeader(e.getKey()), e.getValue() != null ? e.getValue().trim() : "");
        }
        return norm;
    }

    private static String normalizeHeader(String h) {
        return h.toLowerCase(Locale.ROOT).replaceAll("[\\s_-]+", "");
    }

    private QuizQuestion buildFromNamedColumns(Map<String, String> norm, Quiz quiz, String label, List<String> errors) {
        String qtext = first(norm, "question", "questiontext", "stem", "prompt");
        String a = first(norm, "optiona", "a", "choicea");
        String b = first(norm, "optionb", "b", "choiceb");
        String c = first(norm, "optionc", "c", "choicec");
        String d = first(norm, "optiond", "d", "choiced");
        String correctRaw = first(norm, "correctanswer", "correct", "answer", "key");
        String marksStr = first(norm, "marks", "mark", "points");

        if (qtext.isEmpty() || a.isEmpty() || b.isEmpty() || correctRaw.isEmpty()) {
            addError(errors, label + ": missing question, optionA, optionB, or correctAnswer");
            return null;
        }
        int marks = parseMarks(marksStr, 1);
        return assemble(quiz, qtext, a, b, c, d, correctRaw, marks, label, errors);
    }

    private static String first(Map<String, String> norm, String... keys) {
        for (String k : keys) {
            String v = norm.get(k);
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }

    private QuizQuestion buildFromPositionalCells(List<String> cells, Quiz quiz, String label, List<String> errors) {
        String qtext = cells.get(0);
        String a = cells.get(1);
        String b = cells.get(2);
        String c = cells.size() > 3 ? cells.get(3) : "";
        String d = cells.size() > 4 ? cells.get(4) : "";
        String correctRaw = cells.get(5);
        String marksStr = cells.size() > 6 ? cells.get(6) : "";
        if (qtext.isBlank() || a.isBlank() || b.isBlank() || correctRaw.isBlank()) {
            addError(errors, label + ": empty question, options A/B, or correctAnswer");
            return null;
        }
        int marks = parseMarks(marksStr, 1);
        return assemble(quiz, qtext.trim(), a.trim(), b.trim(),
                c != null ? c.trim() : "",
                d != null ? d.trim() : "",
                correctRaw.trim(), marks, label, errors);
    }

    private QuizQuestion assemble(
            Quiz quiz,
            String qtext,
            String a,
            String b,
            String c,
            String d,
            String correctRaw,
            int marks,
            String label,
            List<String> errors
    ) {
        String correct = resolveCorrectAnswer(correctRaw, a, b, c, d);
        if (!matchesOption(correct, a, b, c, d)) {
            addError(errors, label + ": correctAnswer must be A–D or match one option text");
            return null;
        }
        QuizQuestion q = new QuizQuestion();
        q.setQuiz(quiz);
        q.setQuestion(qtext);
        q.setOptionA(a);
        q.setOptionB(b);
        q.setOptionC(c.isEmpty() ? "" : c);
        q.setOptionD(d.isEmpty() ? "" : d);
        q.setCorrectAnswer(correct);
        q.setMarks(marks);
        return q;
    }

    private static boolean matchesOption(String correct, String a, String b, String c, String d) {
        String t = correct.trim();
        return t.equalsIgnoreCase(a.trim())
                || t.equalsIgnoreCase(b.trim())
                || (!c.isBlank() && t.equalsIgnoreCase(c.trim()))
                || (!d.isBlank() && t.equalsIgnoreCase(d.trim()));
    }

    /** Accepts full option text or single letter A–D (case-insensitive). */
    private static String resolveCorrectAnswer(String raw, String a, String b, String c, String d) {
        String t = raw.trim();
        if (t.length() == 1) {
            return switch (Character.toUpperCase(t.charAt(0))) {
                case 'A' -> a;
                case 'B' -> b;
                case 'C' -> c.isEmpty() ? t : c;
                case 'D' -> d.isEmpty() ? t : d;
                default -> t;
            };
        }
        return t;
    }

    private static int parseMarks(String s, int def) {
        if (s == null || s.isBlank()) {
            return def;
        }
        try {
            int m = Integer.parseInt(s.trim());
            return Math.max(1, m);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** Avoid skipping real questions that contain the word "question" in the stem. */
    private static boolean looksLikeHeaderRow(List<String> cells) {
        if (cells.isEmpty()) {
            return false;
        }
        String h = normalizeHeader(cells.get(0));
        return h.equals("question") || h.equals("stem") || h.equals("optiona") || h.equals("prompt");
    }

    private static List<String> parseOneCsvLine(String line) {
        try (CSVParser p = CSVFormat.DEFAULT.parse(new StringReader(line))) {
            for (CSVRecord r : p) {
                List<String> out = new ArrayList<>();
                for (int i = 0; i < r.size(); i++) {
                    out.add(r.get(i) != null ? r.get(i).trim() : "");
                }
                return out;
            }
        } catch (Exception e) {
            return List.of();
        }
        return List.of();
    }

    private static void addError(List<String> errors, String msg) {
        if (errors.size() < MAX_ERROR_LINES) {
            errors.add(msg);
        }
    }
}
