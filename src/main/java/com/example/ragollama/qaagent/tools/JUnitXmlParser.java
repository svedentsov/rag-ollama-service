package com.example.ragollama.qaagent.tools;

import com.example.ragollama.qaagent.model.TestResult;
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
 * В этой версии парсер был улучшен для извлечения полного текста ошибки
 * и стек-трейса из тегов `<failure>` и `<error>`.
 */
@Slf4j
@Service
public class JUnitXmlParser {

    private final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    /**
     * Парсит содержимое JUnit XML и преобразует его в список объектов {@link TestResult}.
     *
     * @param xmlContent Строка, содержащая XML-отчет.
     * @return Список результатов тестов.
     * @throws ProcessingException если происходит ошибка парсинга XML.
     */
    public List<TestResult> parse(String xmlContent) {
        List<TestResult> results = new ArrayList<>();
        if (xmlContent == null || xmlContent.isBlank()) {
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

                results.add(new TestResult(className, testName, status, failureDetails));
            }
        } catch (Exception e) {
            log.error("Не удалось распарсить JUnit XML отчет", e);
            throw new ProcessingException("Ошибка парсинга JUnit XML отчета.", e);
        }
        return results;
    }
}
