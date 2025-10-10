package com.example.ragollama.agent.metrics.tools;

import com.example.ragollama.agent.metrics.model.TestResult;
import com.example.ragollama.shared.exception.ProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Инфраструктурный сервис для парсинга XML-отчетов в формате JUnit.
 * <p>
 * Преобразует XML-структуру в список типизированных Java-объектов,
 * извлекая всю необходимую информацию о каждом тест-кейсе, включая
 * статус, детали ошибки и время выполнения. Эта версия включает
 * "защиту от дурака" для обработки невалидных входных данных.
 */
@Slf4j
@Service
public class JUnitXmlParser {

    private final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    /**
     * Парсит содержимое JUnit XML и преобразует его в список объектов {@link TestResult}.
     *
     * @param xmlContent Строка, содержащая XML-отчет.
     * @return Список результатов тестов. Возвращает пустой список, если xmlContent пуст или невалиден.
     * @throws ProcessingException если происходит критическая, непредвиденная ошибка парсинга.
     */
    public List<TestResult> parse(String xmlContent) {
        List<TestResult> results = new ArrayList<>();
        if (xmlContent == null || xmlContent.isBlank()) {
            log.warn("В JUnitXmlParser передан пустой xmlContent. Возвращен пустой список.");
            return results;
        }

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));
            NodeList testcaseNodes = doc.getElementsByTagName("testcase");

            for (int i = 0; i < testcaseNodes.getLength(); i++) {
                Element testcase = (Element) testcaseNodes.item(i);
                String className = testcase.getAttribute("classname");
                String testName = testcase.getAttribute("name");
                String timeStr = testcase.getAttribute("time");

                long durationMs = (long) (Double.parseDouble(timeStr.replace(",", ".")) * 1000);
                String failureDetails = null;
                TestResult.Status status = TestResult.Status.PASSED;

                NodeList failureNodes = testcase.getElementsByTagName("failure");
                if (failureNodes.getLength() > 0) {
                    status = TestResult.Status.FAILED;
                    failureDetails = failureNodes.item(0).getTextContent();
                }

                NodeList errorNodes = testcase.getElementsByTagName("error");
                if (errorNodes.getLength() > 0) {
                    status = TestResult.Status.FAILED;
                    failureDetails = errorNodes.item(0).getTextContent();
                }

                NodeList skippedNodes = testcase.getElementsByTagName("skipped");
                if (skippedNodes.getLength() > 0) {
                    status = TestResult.Status.SKIPPED;
                }

                results.add(new TestResult(className, testName, status, failureDetails, durationMs));
            }
        } catch (org.xml.sax.SAXParseException e) {
            log.warn("Не удалось распарсить JUnit XML, так как он не является валидным XML. Сообщение: '{}'. Возвращен пустой список.", e.getMessage());
            return List.of(); // Возвращаем пустой список вместо падения
        } catch (Exception e) {
            log.error("Произошла непредвиденная ошибка при парсинге JUnit XML отчета", e);
            throw new ProcessingException("Критическая ошибка парсинга JUnit XML отчета.", e);
        }
        return results;
    }
}
