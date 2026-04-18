package com.vn.backend.services.impl;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.question.*;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.response.answer.AnswerResponse;
import com.vn.backend.dto.response.question.QuestionDetailResponse;
import com.vn.backend.dto.response.question.QuestionSearchResponse;
import com.vn.backend.dto.response.topic.TopicResponse;
import com.vn.backend.dto.request.answer.AnswerCreateRequest;
import com.vn.backend.entities.Answer;
import com.vn.backend.entities.Question;
import com.vn.backend.entities.Topic;
import com.vn.backend.entities.User;
import com.vn.backend.enums.QuestionType;
import com.vn.backend.enums.Role;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.AnswerRepository;
import com.vn.backend.repositories.QuestionRepository;
import com.vn.backend.repositories.TopicRepository;
import com.vn.backend.services.ApprovalRequestService;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.QuestionService;
import com.vn.backend.utils.MessageUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.vn.backend.dto.response.common.PagingMeta;
import com.vn.backend.dto.response.common.ResponseListData;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.io.ByteArrayOutputStream;

@Service
public class QuestionServiceImpl extends BaseService implements QuestionService {
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final ApprovalRequestService approvalRequestService;
    private final AuthService authService;
    private final TopicRepository topicRepository;

    public QuestionServiceImpl(MessageUtils messageUtils, QuestionRepository questionRepository, AnswerRepository answerRepository, ApprovalRequestService approvalRequestService, AuthService authService, TopicRepository topicRepository) {
        super(messageUtils);
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.approvalRequestService = approvalRequestService;
        this.authService = authService;
        this.topicRepository = topicRepository;
    }

    @Override
    public QuestionDetailResponse createQuestion(QuestionCreateRequest request) {
        User currentUser = authService.getCurrentUser();
        Topic topic = topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(request.getTopicId())
                .orElseThrow(() -> new AppException(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND),
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
        // Validate answers presence and correctness
        if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
            throw new AppException(AppConst.MessageConst.INVALID_LOGIC_QUESTION,
                    messageUtils.getMessage(AppConst.MessageConst.INVALID_LOGIC_QUESTION), HttpStatus.BAD_REQUEST);
        }
        long providedCorrect = request.getAnswers().stream()
                .filter(a -> a.getIsCorrect() != null && a.getIsCorrect())
                .count();
        if (providedCorrect == 0) {
            throw new AppException(AppConst.MessageConst.INVALID_LOGIC_QUESTION,
                    messageUtils.getMessage(AppConst.MessageConst.INVALID_LOGIC_QUESTION), HttpStatus.BAD_REQUEST);
        }
        Question q = Question.builder()
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .type(request.getType())
                .difficultyLevel(request.getDifficultyLevel())
                .topicId(request.getTopicId())
                .createdBy(currentUser.getId())
                .isDeleted(false)
                .isReviewQuestion(false)
                .build();
        // Extra rule: SINGLE_CHOICE must have at least 2 answers and only 1 correct
        if (q.getType() == QuestionType.SINGLE_CHOICE) {
            if (request.getAnswers().size() <= 1) {
                throw new AppException(AppConst.MessageConst.INVALID_LOGIC_QUESTION,
                        messageUtils.getMessage(AppConst.MessageConst.INVALID_LOGIC_QUESTION), HttpStatus.BAD_REQUEST);
            }
            if (providedCorrect > 1) {
                throw new AppException(AppConst.MessageConst.INVALID_LOGIC_QUESTION,
                        messageUtils.getMessage(AppConst.MessageConst.INVALID_LOGIC_QUESTION), HttpStatus.BAD_REQUEST);
            }
        }

        Question saved = questionRepository.saveAndFlush(q);
        // Persist answers
        int order = 0;
        for (var aReq : request.getAnswers()) {
            Answer ans = Answer.builder()
                    .questionId(saved.getQuestionId())
                    .content(aReq.getContent())
                    .isCorrect(aReq.getIsCorrect() != null && aReq.getIsCorrect())
                    .displayOrder(aReq.getDisplayOrder() != null ? aReq.getDisplayOrder() : order)
                    .isDeleted(false)
                    .build();
            answerRepository.save(ans);
            order++;
        }
        return mapToDetailResponse(saved);
    }

    @Override
    public List<QuestionDetailResponse> createQuestions(List<QuestionBulkCreateItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new AppException(AppConst.MessageConst.INVALID_LOGIC_QUESTION,
                    messageUtils.getMessage(AppConst.MessageConst.INVALID_LOGIC_QUESTION), HttpStatus.BAD_REQUEST);
        }
        User currentUser = authService.getCurrentUser();
        List<QuestionDetailResponse> responses = new ArrayList<>();
        for (QuestionBulkCreateItemRequest request : requests) {
            QuestionDetailResponse detailResponse = createQuestionFromBulkRequest(request, currentUser);
            responses.add(detailResponse);
        }
        return responses;
    }

    private QuestionDetailResponse createQuestionFromBulkRequest(QuestionBulkCreateItemRequest request, User currentUser) {
        Topic topic = topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(request.getTopicId())
                .orElseThrow(() -> new AppException(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND),
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
        List<QuestionBulkAnswerCreateRequest> answers = request.getAnswers();
        if (answers == null || answers.isEmpty()) {
            throw new AppException(AppConst.MessageConst.INVALID_LOGIC_QUESTION,
                    messageUtils.getMessage(AppConst.MessageConst.INVALID_LOGIC_QUESTION), HttpStatus.BAD_REQUEST);
        }
        long providedCorrect = answers.stream()
                .filter(a -> a.getIsCorrect() != null && a.getIsCorrect())
                .count();
        if (providedCorrect == 0) {
            throw new AppException(AppConst.MessageConst.INVALID_LOGIC_QUESTION,
                    messageUtils.getMessage(AppConst.MessageConst.INVALID_LOGIC_QUESTION), HttpStatus.BAD_REQUEST);
        }
        if (request.getType() == QuestionType.SINGLE_CHOICE) {
            if (answers.size() <= 1 || providedCorrect > 1) {
                throw new AppException(AppConst.MessageConst.INVALID_LOGIC_QUESTION,
                        messageUtils.getMessage(AppConst.MessageConst.INVALID_LOGIC_QUESTION), HttpStatus.BAD_REQUEST);
            }
        }
        Question q = Question.builder()
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .type(request.getType())
                .difficultyLevel(request.getDifficultyLevel())
                .topicId(request.getTopicId())
                .createdBy(currentUser.getId())
                .isDeleted(false)
                .isReviewQuestion(false)
                .build();
        Question saved = questionRepository.saveAndFlush(q);
        int order = 0;
        for (QuestionBulkAnswerCreateRequest ansReq : answers) {
            Answer ans = Answer.builder()
                    .questionId(saved.getQuestionId())
                    .content(ansReq.getContent())
                    .isCorrect(Boolean.TRUE.equals(ansReq.getIsCorrect()))
                    .displayOrder(ansReq.getDisplayOrder() != null ? ansReq.getDisplayOrder() : order)
                    .isDeleted(false)
                    .build();
            answerRepository.save(ans);
            order++;
        }
        return mapToDetailResponse(saved);
    }

    @Override
    public QuestionDetailResponse getQuestionDetail(Long questionId) {
        User currentUser = authService.getCurrentUser();
        Long userId = currentUser.getId();
        Optional<Question> questionOpt = questionRepository.findByQuestionIdAndIsDeletedFalse(questionId);
        if (questionOpt.isEmpty()){
            throw new AppException(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND),
                    messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST);
        }
        Question q = questionOpt.get();
        return mapToDetailResponse(q);
    }

    @Override
    public QuestionDetailResponse updateQuestion(Long questionId, QuestionUpdateRequest request) {
        User currentUser = authService.getCurrentUser();
        
        // Không cho Admin sửa câu hỏi
        if (currentUser.getRole() != Role.TEACHER) {
            throw new AppException(AppConst.MessageConst.UNAUTHORIZED,
                    "Cannot update questions", HttpStatus.FORBIDDEN);
        }
        
        Long userId = currentUser.getId();
        Optional<Question> opt = questionRepository.findByQuestionIdAndCreatedByAndIsDeletedFalse(questionId,userId);
        if (opt.isEmpty()) {
            throw new AppException(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND),
                    messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST);
        }
        Question q = opt.get();
        
        // Nếu sửa câu hỏi ôn tập thì set isReviewQuestion = false
        if (Boolean.TRUE.equals(q.getIsReviewQuestion())) {
            q.setIsReviewQuestion(false);
        }
        
        if (request.getContent() != null) q.setContent(request.getContent());
        if (request.getImageUrl() != null) q.setImageUrl(request.getImageUrl());
        if (request.getType() != null) q.setType(request.getType());
        if (request.getDifficultyLevel() != null) q.setDifficultyLevel(request.getDifficultyLevel());
        if (request.getTopicId() != null) q.setTopicId(request.getTopicId());
        Question updated = questionRepository.saveAndFlush(q);
        return mapToDetailResponse(updated);
    }

    @Override
    public void softDeleteQuestion(Long questionId) {
        Optional<Question> qOpt = questionRepository.findById(questionId);
        qOpt.ifPresent(q -> {
            q.setIsDeleted(true);
            questionRepository.save(q);
        });
    }

    @Override
    public ResponseListData<QuestionSearchResponse> searchQuestions(BaseFilterSearchRequest<QuestionSearchRequest> request) {
        QuestionSearchRequest filters = request.getFilters();
        if (filters == null) {
            filters = new QuestionSearchRequest();
        }
        User  currentUser = authService.getCurrentUser();
        Long currentUserId = currentUser.getId();
        PagingMeta paging = request.getPagination().getPagingMeta();
        Pageable pageable = paging.toPageable();

        Page<Question> page;
        if (currentUser.getRole() == Role.ADMIN) {
            page = questionRepository.searchQuestionsForAdmin(
                    filters.getType(),
                    filters.getDifficultyLevel(),
                    filters.getTopicId(),
                    filters.getSubjectId(),
                    filters.getContent(),
                    pageable
            );
        } else {
            page = questionRepository.searchQuestions(
                    filters.getType(),
                    filters.getDifficultyLevel(),
                    filters.getTopicId(),
                    filters.getSubjectId(),
                    currentUserId,
                    filters.getContent(),
                    pageable
            );
        }

        List<QuestionSearchResponse> content = new ArrayList<>();
        for (Question q : page.getContent()) {
            QuestionSearchResponse r = new QuestionSearchResponse();
            r.setId(q.getQuestionId());
            r.setContent(q.getContent());
            r.setType(q.getType() != null ? q.getType().name() : null);
            r.setDifficultyLevel(q.getDifficultyLevel());
            r.setTopicId(q.getTopicId());
            r.setTopicName(q.getTopic().getTopicName());
            r.setIsReviewQuestion(q.getIsReviewQuestion());
            content.add(r);
        }

        paging.setTotalRows(page.getTotalElements());
        paging.setTotalPages(page.getTotalPages());

        return new ResponseListData<>(content, paging);
    }

    @Override
    public QuestionBulkCreateRequest importQuestionsFromExcel(MultipartFile file, QuestionImportExcelRequest request) {
        List<QuestionBulkCreateItemRequest> questions = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheet("Import");
            if (sheet == null && workbook.getNumberOfSheets() > 1) {
                sheet = workbook.getSheetAt(1); // fallback to second sheet
            }
            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // skip header (row 0)
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                // Index 0: STT (bỏ qua)
                // Index 1: Chủ đề (topicId)
                Cell topicCell = row.getCell(1);
                // Index 2: Độ khó (difficultyLevel)
                Cell difficultyCell = row.getCell(2);
                // Index 3: Câu hỏi
                Cell questionCell = row.getCell(3);
                // Index 4-7: Đáp án 1-4
                // Index 8: Đáp án đúng (1, 2, 3, hoặc 4)
                Cell correctAnswerCell = row.getCell(8);
                
                if (topicCell == null || questionCell == null || correctAnswerCell == null) continue;

                Long topicId = null;
                if (topicCell.getCellType() == CellType.NUMERIC) {
                    topicId = (long) topicCell.getNumericCellValue();
                } else if (topicCell.getCellType() == CellType.STRING) {
                    try { topicId = Long.parseLong(topicCell.getStringCellValue().trim()); } catch (NumberFormatException ignored) {}
                }
                if (topicId == null) continue;

                String content = getCellValueAsString(questionCell);
                if (content == null || content.trim().isEmpty()) continue;

                // Parse độ khó (mặc định là 1 nếu không có)
                Integer difficultyLevel = 1;
                if (difficultyCell != null) {
                    if (difficultyCell.getCellType() == CellType.NUMERIC) {
                        difficultyLevel = (int) difficultyCell.getNumericCellValue();
                    } else if (difficultyCell.getCellType() == CellType.STRING) {
                        try {
                            difficultyLevel = Integer.parseInt(difficultyCell.getStringCellValue().trim());
                        } catch (NumberFormatException ignored) {}
                    }
                }

                // Parse đáp án đúng (1, 2, 3, hoặc 4)
                Integer correctAnswerIndex = null;
                if (correctAnswerCell.getCellType() == CellType.NUMERIC) {
                    correctAnswerIndex = (int) correctAnswerCell.getNumericCellValue();
                } else if (correctAnswerCell.getCellType() == CellType.STRING) {
                    try {
                        correctAnswerIndex = Integer.parseInt(correctAnswerCell.getStringCellValue().trim());
                    } catch (NumberFormatException ignored) {}
                }
                
                if (correctAnswerIndex == null || correctAnswerIndex < 1 || correctAnswerIndex > 4) {
                    throw new AppException(AppConst.MessageConst.INVALID_LOGIC_QUESTION,
                            "Đáp án đúng phải là 1, 2, 3, hoặc 4 (dòng " + (i + 1) + ")", HttpStatus.BAD_REQUEST);
                }

                Topic topic = topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(topicId)
                        .orElse(null);
                if (topic == null) {
                    // skip if topic not found or not active
                    continue;
                }

                // Đọc 4 đáp án từ index 4-7 (Đáp án 1-4)
                List<String> answers = new ArrayList<>();
                for (int c = 4; c <= 7; c++) {
                    Cell ansCell = row.getCell(c);
                    String ansContent = getCellValueAsString(ansCell);
                    if (ansContent == null || ansContent.trim().isEmpty()) {
                        answers.add(""); // Cho phép đáp án trống
                    } else {
                        answers.add(ansContent.trim());
                    }
                }

                // Kiểm tra đáp án tại vị trí correctAnswerIndex phải có nội dung
                if (answers.get(correctAnswerIndex - 1) == null || answers.get(correctAnswerIndex - 1).isEmpty()) {
                    throw new AppException(AppConst.MessageConst.INVALID_LOGIC_QUESTION,
                            "Đáp án đúng (Đáp án " + correctAnswerIndex + ") phải có nội dung (dòng " + (i + 1) + ")", HttpStatus.BAD_REQUEST);
                }

                // Xác định loại câu hỏi: luôn là SINGLE_CHOICE vì chỉ có 1 đáp án đúng
                QuestionType derivedType = QuestionType.SINGLE_CHOICE;

                // Tạo QuestionBulkCreateItemRequest
                QuestionBulkCreateItemRequest questionItem = new QuestionBulkCreateItemRequest();
                questionItem.setContent(content);
                questionItem.setType(derivedType);
                questionItem.setDifficultyLevel(difficultyLevel);
                questionItem.setTopicId(topicId);
                questionItem.setImageUrl(null);

                // Tạo danh sách đáp án
                List<QuestionBulkAnswerCreateRequest> answerList = new ArrayList<>();
                for (int idx = 0; idx < 4; idx++) {
                    String ansContent = answers.get(idx);
                    if (ansContent == null || ansContent.isEmpty()) {
                        continue; // Bỏ qua đáp án trống
                    }
                    boolean isCorrect = (idx + 1 == correctAnswerIndex);
                    QuestionBulkAnswerCreateRequest answerItem = new QuestionBulkAnswerCreateRequest();
                    answerItem.setContent(ansContent);
                    answerItem.setIsCorrect(isCorrect);
                    answerItem.setDisplayOrder(idx);
                    answerList.add(answerItem);
                }
                questionItem.setAnswers(answerList);
                questions.add(questionItem);
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(AppConst.MessageConst.FILE_UPLOAD_FAILED,
                    messageUtils.getMessage(AppConst.MessageConst.FILE_UPLOAD_FAILED), HttpStatus.BAD_REQUEST);
        }
        
        QuestionBulkCreateRequest result = new QuestionBulkCreateRequest();
        result.setQuestions(questions);
        return result;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            // Kiểm tra xem có phải là số nguyên không
            double numValue = cell.getNumericCellValue();
            if (numValue == (long) numValue) {
                return String.valueOf((long) numValue);
            } else {
                return String.valueOf(numValue);
            }
        }
        return null;
    }

    @Override
    public byte[] exportQuestionsToExcel(QuestionSearchRequest request) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Questions");
            User currentUser = authService.getCurrentUser();
            Page<Question> page = questionRepository.searchQuestions(
                    request.getType(),
                    request.getDifficultyLevel(),
                    request.getTopicId(),
                    request.getSubjectId(),
                    currentUser.getId(),
                    request.getContent(),
                    Pageable.unpaged()
            );
            List<Question> questions = page.getContent();
            int maxAnswers = 0;
            List<List<Answer>> allAnswers = new ArrayList<>();
            for (Question q : questions) {
                List<Answer> ans = answerRepository.findByQuestionIdOrderByDisplayOrder(q.getQuestionId());
                ans.removeIf(a -> Boolean.TRUE.equals(a.getIsDeleted()));
                allAnswers.add(ans);
                if (ans.size() > maxAnswers) maxAnswers = ans.size();
            }

            // Header
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("STT");
            header.createCell(1).setCellValue("Câu hỏi");
            for (int i = 0; i < maxAnswers; i++) {
                header.createCell(2 + i).setCellValue("Đáp án " + (i + 1));
            }

            // Data
            for (int i = 0; i < questions.size(); i++) {
                Question q = questions.get(i);
                List<Answer> ans = allAnswers.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(q.getContent());
                for (int j = 0; j < ans.size(); j++) {
                    row.createCell(2 + j).setCellValue(ans.get(j).getContent());
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new AppException(AppConst.MessageConst.FILE_DOWNLOAD_FAILED,
                    messageUtils.getMessage(AppConst.MessageConst.FILE_DOWNLOAD_FAILED), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public byte[] downloadImportTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Sheet 1: Rule import
            Sheet rule = workbook.createSheet("Rule import");
            int r = 0;
            rule.createRow(r++).createCell(0).setCellValue("Quy tắc import câu hỏi");
            rule.createRow(r++).createCell(0).setCellValue("- Sheet 'Import' có cột: STT | Chủ đề (topicId) | Độ khó | Câu hỏi | Đáp án 1 | Đáp án 2 | Đáp án 3 | Đáp án 4 | Đáp án đúng");
            rule.createRow(r++).createCell(0).setCellValue("- Độ khó: Nhập số từ 1-5 (1 = Dễ nhất, 5 = Khó nhất), mặc định là 1 nếu để trống");
            rule.createRow(r++).createCell(0).setCellValue("- Mỗi câu hỏi có 4 đáp án (có thể để trống các đáp án không dùng)");
            rule.createRow(r++).createCell(0).setCellValue("- Cột 'Đáp án đúng': Nhập số 1, 2, 3, hoặc 4 để chỉ định đáp án nào là đúng");
            rule.createRow(r++).createCell(0).setCellValue("- Ví dụ: Nếu Đáp án 2 là đúng, thì nhập số 2 vào cột 'Đáp án đúng'");
            rule.createRow(r++).createCell(0).setCellValue("- TopicId phải thuộc về người dùng hiện tại, nếu không sẽ bỏ qua");
            rule.createRow(r++).createCell(0).setCellValue("- Đáp án đúng bắt buộc phải có nội dung");

            // Sheet 2: Import
            Sheet imp = workbook.createSheet("Import");
            Row header = imp.createRow(0);
            header.createCell(0).setCellValue("STT");
            header.createCell(1).setCellValue("Chủ đề");
            header.createCell(2).setCellValue("Độ khó");
            header.createCell(3).setCellValue("Câu hỏi");
            header.createCell(4).setCellValue("Đáp án 1");
            header.createCell(5).setCellValue("Đáp án 2");
            header.createCell(6).setCellValue("Đáp án 3");
            header.createCell(7).setCellValue("Đáp án 4");
            header.createCell(8).setCellValue("Đáp án đúng");
            
            // Tạo 1 dòng ví dụ
            Row exampleRow = imp.createRow(1);
            exampleRow.createCell(0).setCellValue(1);
            exampleRow.createCell(1).setCellValue("1"); // TopicId
            exampleRow.createCell(2).setCellValue(1); // Độ khó
            exampleRow.createCell(3).setCellValue("2 + 3 = ?"); // Câu hỏi
            exampleRow.createCell(4).setCellValue("4");
            exampleRow.createCell(5).setCellValue("5");
            exampleRow.createCell(6).setCellValue("6");
            exampleRow.createCell(7).setCellValue("7");
            exampleRow.createCell(8).setCellValue(2); // Đáp án 2 là đúng

            // Sheet 3: Chủ đề
            Sheet topicSheet = workbook.createSheet("Chủ đề");
            Row th = topicSheet.createRow(0);
            th.createCell(0).setCellValue("STT");
            th.createCell(1).setCellValue("ID");
            th.createCell(2).setCellValue("Tên chủ đề");
            th.createCell(3).setCellValue("Tên môn học");
            th.createCell(4).setCellValue("Code môn học");
            int rowIdx = 1;
            List<Object[]> topics = topicRepository.listTopicsWithSubject();
            for (int i = 0; i < topics.size(); i++) {
                Object[] row = topics.get(i);
                Row tr = topicSheet.createRow(rowIdx++);
                tr.createCell(0).setCellValue(i + 1);
                tr.createCell(1).setCellValue(((Number) row[0]).longValue());
                tr.createCell(2).setCellValue((String) row[1]);
                tr.createCell(3).setCellValue((String) row[2]);
                tr.createCell(4).setCellValue((String) row[3]);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new AppException(AppConst.MessageConst.FILE_DOWNLOAD_FAILED,
                    messageUtils.getMessage(AppConst.MessageConst.FILE_DOWNLOAD_FAILED), HttpStatus.BAD_REQUEST);
        }
    }

    private QuestionDetailResponse mapToDetailResponse(Question q) {
        QuestionDetailResponse detail = new QuestionDetailResponse();
        detail.setId(q.getQuestionId());
        detail.setContent(q.getContent());
        detail.setImageUrl(q.getImageUrl());
        detail.setType(q.getType() != null ? q.getType().name() : null);
        detail.setDifficultyLevel(q.getDifficultyLevel());
        Topic topic = topicRepository.findByTopicIdAndIsDeletedFalse(q.getTopicId());
        TopicResponse topicResponse = new TopicResponse();
        topicResponse.setTopicId(topic.getTopicId());
        topicResponse.setSubjectId(topic.getSubjectId());
        topicResponse.setCreatedAt(topic.getCreatedAt());
        topicResponse.setTopicName(topic.getTopicName());
        topicResponse.setUpdatedAt(topic.getUpdatedAt());
        topicResponse.setIsDeleted(topic.getIsDeleted());
        detail.setTopic(topicResponse);
        detail.setCreatedBy(q.getCreatedBy());
        var answers = answerRepository.findByQuestionIdOrderByDisplayOrder(q.getQuestionId());
        detail.setAnswers(mapAnswers(answers));
        return detail;
    }

    private List<AnswerResponse> mapAnswers(List<Answer> answers) {
        List<AnswerResponse> responseList = new ArrayList<>();
        for (Answer ans : answers) {
            if (Boolean.TRUE.equals(ans.getIsDeleted())) continue;
            AnswerResponse ar = new AnswerResponse();
            ar.setId(ans.getAnswerId());
            ar.setContent(ans.getContent());
            ar.setIsCorrect(ans.getIsCorrect());
            ar.setDisplayOrder(ans.getDisplayOrder());
            responseList.add(ar);
        }
        return responseList;
    }

    @Override
    @Transactional
    public void createApprovalQuestion(CreateApprovalQuestionRequest request) {
        User currentUser = authService.getCurrentUser();
        
        // Validate chỉ TEACHER mới được tạo đề xuất
        if (currentUser.getRole() != Role.TEACHER) {
            throw new AppException(AppConst.MessageConst.UNAUTHORIZED,
                    messageUtils.getMessage(AppConst.MessageConst.UNAUTHORIZED), HttpStatus.BAD_REQUEST);
        }
        
        // Validate question IDs
        for (Long questionId : request.getQuestionIds()) {
            // Lấy question gốc từ database
            Question originalQuestion = questionRepository.findById(questionId)
                    .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                            messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
            
            // Kiểm tra question không bị xóa
            if (Boolean.TRUE.equals(originalQuestion.getIsDeleted())) {
                throw new AppException(AppConst.MessageConst.NOT_FOUND,
                        "Question not found or deleted", HttpStatus.BAD_REQUEST);
            }
            
            // Validate question thuộc về user hiện tại
            if (!originalQuestion.getCreatedBy().equals(currentUser.getId())) {
                throw new AppException(AppConst.MessageConst.UNAUTHORIZED,
                        "You can only create approval request for your own questions", HttpStatus.FORBIDDEN);
            }
        }
        
        // Tạo approval request với các question IDs gốc (không clone)
        approvalRequestService.createRequest(
                request.getRequestType(),
                request.getDescription(),
                currentUser.getId(),
                request.getQuestionIds()
        );
    }
}
