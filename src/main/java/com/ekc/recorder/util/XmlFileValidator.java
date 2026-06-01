package com.ekc.recorder.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
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
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public final class XmlFileValidator {

    private final RecorderCategory category;
    private final Schema schema;

    public XmlFileValidator(RecorderCategory category) throws IOException {
        this.category = category;
        this.schema = loadSchema(category);
    }

    public void validate(Path xmlFile) throws IOException {
        Document document = loadDocument(xmlFile);

        Validator validator = schema.newValidator();
        try {
            validator.validate(new DOMSource(document));
        } catch (SAXException e) {
            throw new IOException("Fichier XML invalide pour la catégorie '" + category + "' : " + xmlFile.toAbsolutePath(), e);
        }

        System.out.printf("Fichier XML valide (%s) : %s%n", category.name().toLowerCase(java.util.Locale.ROOT), xmlFile.toAbsolutePath());
    }

    public String canonicalContent(Path xmlFile) throws IOException {
        Document document = loadDocument(xmlFile);
        return serialize(document);
    }

    private Document loadDocument(Path xmlFile) throws IOException {
        if (!Files.exists(xmlFile)) {
            throw new IOException("Fichier XML introuvable : " + xmlFile.toAbsolutePath());
        }

        try (InputStream inputStream = Files.newInputStream(xmlFile)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(inputStream);
            normalize(document.getDocumentElement());
            return document;
        } catch (Exception e) {
            if (e instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Impossible de lire le fichier XML : " + xmlFile.toAbsolutePath(), e);
        }
    }

    private String serialize(Document document) throws IOException {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new IOException("Impossible de sérialiser le fichier XML canonique.", e);
        }
    }

    private void normalize(Element element) {
        List<String> order = orderedChildrenFor(element.getTagName());
        if (order == null) {
            return;
        }

        Map<String, List<Element>> grouped = new LinkedHashMap<>();
        List<Element> unknown = new ArrayList<>();

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element childElement = (Element) child;
            normalize(childElement);

            if (order.contains(childElement.getTagName())) {
                grouped.computeIfAbsent(childElement.getTagName(), key -> new ArrayList<>()).add(childElement);
            } else {
                unknown.add(childElement);
            }
        }

        while (element.hasChildNodes()) {
            element.removeChild(element.getFirstChild());
        }

        for (String childName : order) {
            List<Element> elements = grouped.get(childName);
            if (elements == null) {
                continue;
            }
            for (Element childElement : elements) {
                element.appendChild(childElement);
            }
        }

        for (Element childElement : unknown) {
            element.appendChild(childElement);
        }
    }

    private List<String> orderedChildrenFor(String tagName) {
        return switch (tagName) {
            case "teamFights" -> List.of("teamFight");
            case "teamFight" -> List.of(
                    "EventCode",
                    "FightID",
                    "Shiaijo",
                    "Pool",
                    "Fight",
                    "SortOrder",
                    "TeamIDR",
                    "TeamR",
                    "WinsR",
                    "SetR",
                    "CountryR",
                    "Hikiwake",
                    "Encho",
                    "CountryW",
                    "SetW",
                    "WinsW",
                    "TeamW",
                    "TeamIDW",
                    "Winner",
                    "FightOrder",
                    "Description",
                    "DescriptionShort",
                    "Status",
                    "StatusIndividual",
                    "FinishedAt",
                    "LastChanged",
                    "childFights");
            case "childFights" -> List.of("childFight");
            case "childFight" -> List.of(
                    "EventCode",
                    "FightID",
                    "Shiaijo",
                    "Pool",
                    "Fight",
                    "FightNumber",
                    "SortOrder",
                    "FighterIDR",
                    "CountryR",
                    "TareNumberR",
                    "FighterR",
                    "IpponR1",
                    "IpponR2",
                    "HansokuR",
                    "Hikiwake",
                    "Encho",
                    "HansokuW",
                    "IpponW2",
                    "IpponW1",
                    "FighterW",
                    "TareNumberW",
                    "CountryW",
                    "FighterIDW",
                    "Winner",
                    "FightOrder",
                    "Description",
                    "DescriptionShort",
                    "Status",
                    "StatusIndividual",
                    "FinishedAt",
                    "LastChanged");
            case "indFights" -> List.of("indFight");
            case "indFight" -> List.of(
                    "EventCode",
                    "FightID",
                    "Shiaijo",
                    "Pool",
                    "Fight",
                    "SortOrder",
                    "FighterIDR",
                    "CountryR",
                    "TareNumberR",
                    "FighterR",
                    "IpponR1",
                    "IpponR2",
                    "HansokuR",
                    "Hikiwake",
                    "Encho",
                    "HansokuW",
                    "IpponW2",
                    "IpponW1",
                    "FighterW",
                    "TareNumberW",
                    "CountryW",
                    "FighterIDW",
                    "FightOrder",
                    "Description",
                    "DescriptionShort",
                    "Status",
                    "Winner",
                    "FinishedAt");
            default -> null;
        };
    }

    private Schema loadSchema(RecorderCategory category) throws IOException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try (InputStream schemaStream = resource(category.schemaResource())) {
            return factory.newSchema(new javax.xml.transform.stream.StreamSource(schemaStream));
        } catch (SAXException e) {
            throw new IOException("Impossible de charger le schéma XSD : " + category.schemaResource(), e);
        }
    }

    private InputStream resource(String resourceName) throws IOException {
        InputStream inputStream = XmlFileValidator.class.getClassLoader().getResourceAsStream(resourceName);
        if (inputStream == null) {
            throw new IOException("Ressource XSD introuvable : " + resourceName);
        }
        return inputStream;
    }
}
