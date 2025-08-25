package com.example.ragollama.qaagent.tools;

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
import java.util.HashMap;
import java.util.Map;

/**
 * Инфраструктурный сервис для парсинга XML-отчетов JaCoCo.
 * <p>
 * Преобразует сложный XML-формат в простую карту, сопоставляющую
 * путь к файлу с процентом покрытия его методов.
 */
@Slf4j
@Service
public class JacocoReportParser {

    private final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    /**
     * Парсит XML-отчет JaCoCo.
     *
     * @param xmlContent Строка с содержимым отчета.
     * @return Карта, где ключ - путь к Java-файлу, а значение - процент покрытия методов.
     */
    public Map<String, Double> parse(String xmlContent) {
        Map<String, Double> coverageData = new HashMap<>();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));
            NodeList classNodes = doc.getElementsByTagName("class");

            for (int i = 0; i < classNodes.getLength(); i++) {
                Element classElement = (Element) classNodes.item(i);
                String className = classElement.getAttribute("name"); // e.g., com/example/ragollama/MyClass
                String filePath = "src/main/java/" + className + ".java";

                NodeList counterNodes = classElement.getElementsByTagName("counter");
                for (int j = 0; j < counterNodes.getLength(); j++) {
                    Element counterElement = (Element) counterNodes.item(j);
                    if ("METHOD".equals(counterElement.getAttribute("type"))) {
                        double missed = Double.parseDouble(counterElement.getAttribute("missed"));
                        double covered = Double.parseDouble(counterElement.getAttribute("covered"));
                        double total = missed + covered;
                        double percentage = (total == 0) ? 100.0 : (covered / total) * 100.0;
                        coverageData.put(filePath, percentage);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Не удалось распарсить JaCoCo XML отчет", e);
            throw new ProcessingException("Ошибка парсинга JaCoCo XML отчета.", e);
        }
        return coverageData;
    }
}
