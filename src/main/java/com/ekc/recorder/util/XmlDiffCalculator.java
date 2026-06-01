package com.ekc.recorder.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class XmlDiffCalculator {

    public List<XmlChange> diff(String beforeXml, String afterXml) throws IOException {
        try {
            Document beforeDocument = parse(beforeXml);
            Document afterDocument = parse(afterXml);
            List<XmlChange> changes = new ArrayList<>();
            diffElements("/" + beforeDocument.getDocumentElement().getTagName(),
                    beforeDocument.getDocumentElement(), afterDocument.getDocumentElement(), changes);
            return changes;
        } catch (Exception e) {
            throw new IOException("Impossible de comparer les documents XML.", e);
        }
    }

    private Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private void diffElements(String xpath, Element before, Element after, List<XmlChange> changes) throws Exception {
        if (!normalize(before.getTextContent()).equals(normalize(after.getTextContent()))
                && isLeaf(before) && isLeaf(after)) {
            changes.add(new XmlChange(xpath, XmlChangeType.UPDATE, normalize(before.getTextContent()),
                    normalize(after.getTextContent())));
            return;
        }

        Map<String, List<Element>> beforeChildren = groupChildren(before);
        Map<String, List<Element>> afterChildren = groupChildren(after);

        for (String childName : union(beforeChildren, afterChildren)) {
            List<Element> beforeList = beforeChildren.getOrDefault(childName, List.of());
            List<Element> afterList = afterChildren.getOrDefault(childName, List.of());
            int max = Math.max(beforeList.size(), afterList.size());

            for (int i = 0; i < max; i++) {
                String childXPath = xpath + "/" + childName + "[" + (i + 1) + "]";
                if (i >= beforeList.size()) {
                    changes.add(new XmlChange(childXPath, XmlChangeType.CREATE, "", serialize(afterList.get(i))));
                    continue;
                }

                if (i >= afterList.size()) {
                    changes.add(new XmlChange(childXPath, XmlChangeType.DELETE, serialize(beforeList.get(i)), ""));
                    continue;
                }

                Element beforeChild = beforeList.get(i);
                Element afterChild = afterList.get(i);
                if (isLeaf(beforeChild) && isLeaf(afterChild)) {
                    String beforeValue = normalize(beforeChild.getTextContent());
                    String afterValue = normalize(afterChild.getTextContent());
                    if (!beforeValue.equals(afterValue)) {
                        changes.add(new XmlChange(childXPath, XmlChangeType.UPDATE, beforeValue, afterValue));
                    }
                } else {
                    diffElements(childXPath, beforeChild, afterChild, changes);
                }
            }
        }
    }

    private boolean isLeaf(Element element) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                return false;
            }
        }
        return true;
    }

    private Map<String, List<Element>> groupChildren(Element element) {
        Map<String, List<Element>> grouped = new LinkedHashMap<>();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element childElement = (Element) child;
            grouped.computeIfAbsent(childElement.getTagName(), key -> new ArrayList<>()).add(childElement);
        }
        return grouped;
    }

    private List<String> union(Map<String, List<Element>> first, Map<String, List<Element>> second) {
        List<String> keys = new ArrayList<>(first.keySet());
        for (String key : second.keySet()) {
            if (!keys.contains(key)) {
                keys.add(key);
            }
        }
        return keys;
    }

    private String serialize(Element element) throws IOException {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(element), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new IOException("Impossible de sérialiser un fragment XML.", e);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}

