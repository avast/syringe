package com.avast.syringe.config.internal;

import com.avast.syringe.config.ConfigException;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.apache.xerces.jaxp.validation.XMLSchemaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.*;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.ValidatorHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class XmlConfigParser {

    private static Logger LOGGER = LoggerFactory.getLogger(XmlConfigParser.class);

    public static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    public static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
    public static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    public static final String SCHEMA_USER_DATA_KEY = "schema";
    public static final String SCHEMAS_PREFIX = "/schemas";

    @Deprecated
    private final URL schemaUrl;

    public XmlConfigParser() {
        // the schema url will be derived from the root element namespace
        schemaUrl = null;
    }

    /**
     * The schema URL should be derived from the root element namespace URI
     */
    @Deprecated
    public XmlConfigParser(URL schemaUrl) {
        Preconditions.checkNotNull(schemaUrl, "schemaUrl");
        this.schemaUrl = schemaUrl;
    }

    public Map<String, Property> loadProperties(File file) {
        Preconditions.checkNotNull(file, "file");

        Document document;
        try {
            InputStream input = new FileInputStream(file);
            try {
                document = loadDocument(input, file.getAbsolutePath());
            } finally {
                input.close();
            }
        } catch (ConfigException e) {
            String message = String.format("Error while parsing XML config file %s", file.getAbsolutePath());
            throw new IllegalArgumentException(message, e);
        } catch (IOException e) {
            String message = String.format("Error while loading XML config file %s", file.getAbsolutePath());
            throw new IllegalArgumentException(message, e);
        }

        return loadProperties(document);
    }

    public XmlConfig loadConfig(InputStream input) throws Exception {
        return parseConfig(input);
    }

    @Deprecated
    public Map<String, Property> loadProperties(InputStream input) {
        Preconditions.checkNotNull(input, "input");

        // We don't know the file name :-(
        Document document = loadDocument(input, "");
        return loadProperties(document);
    }


    @Deprecated
    private static Map<String, Property> loadProperties(Document document) {
        Map<String, Property> props = Maps.newHashMap();

        Node node = (Node) document.getDocumentElement().getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Element.ELEMENT_NODE) {
                Property prop = parsePropElement((Element) node);
                props.put(prop.getName(), prop);
            }
            node = node.getNextSibling();
        }

        return props;
    }

    @Deprecated
    private static Property parsePropElement(Element propElement) {
        String propName = propElement.getLocalName();
        Node child = propElement.getFirstChild();
        while (child != null) {
            switch (child.getNodeType()) {
                case Element.CDATA_SECTION_NODE:
                case Element.TEXT_NODE:
                    String text = child.getTextContent();
                    // While we're seeing whitespace only we don't know whether
                    // propElement contains sub-elements or just text.
                    if (!isWhitespace(text)) {
                        String propValue = propElement.getTextContent();
                        return new Property(propName, new Value(propValue));
                    }
                    break;
                case Element.ELEMENT_NODE:
                    return new Property(propName, parsePropChildElements((Element) child));
            }
            child = child.getNextSibling();
        }
        return new Property(propName);
    }

    @Deprecated
    private static List<Value> parsePropChildElements(Element firstElement) {
        List<Value> propValues = new ArrayList<Value>();
        Node child = firstElement;
        while (child != null) {
            if (child.getNodeType() == Element.ELEMENT_NODE) {
                propValues.add(parsePropChildElement((Element) child));
            }
            child = child.getNextSibling();
        }
        return propValues;
    }

    @Deprecated
    private static Value parsePropChildElement(Element element) {
        String nodeName = element.getLocalName();
        if (nodeName.equals("value")) {
            return new Value(element.getTextContent());
        } else if (nodeName.equals("entry")) {
            return new MapEntry(element.getAttribute("key"), element.getTextContent());
        }
        throw new AssertionError();
    }

    private XmlConfig parseConfig(InputStream xmlStream) throws Exception {
        XMLReader parser = XMLReaderFactory.createXMLReader();
        ConfigParserContentHandler handler = new ConfigParserContentHandler();
        parser.setContentHandler(handler);
        parser.setErrorHandler(handler);
        parser.parse(new InputSource(xmlStream));

        String namespaceURI = handler.getNamespaceURI();
        List<XmlConfig.Decorator> decorators = handler.getDecorators();
        return new XmlConfig(handler.getProperties(), namespaceURI, getClassNameForNamespaceURI(namespaceURI),
                decorators);
    }

    private static boolean isWhitespace(String text) {
        return Pattern.matches("[ \\t\\r\\n]*", text);
    }

    @Deprecated
    private Document loadDocument(InputStream input, String fileName) {
        try {
            return loadDocumentImpl(input);
        } catch (ParserConfigurationException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        } catch (SAXException e) {
            String message = String.format("Error while parsing XML config file %s", fileName);
            throw new IllegalArgumentException(message, e);
        } catch (Exception e) {
            String message = String.format("Error while loading XML config file %s", fileName);
            throw new IllegalArgumentException(message);
        }
    }

    @Deprecated
    private Document loadDocumentImpl(InputStream in) throws Exception {
        if (schemaUrl != null) {
            return loadDocumentWithExplicitSchema(in);
        } else {
            throw new IllegalArgumentException("No explicit schema specified");
        }
    }

    @Deprecated
    private Document loadDocumentWithExplicitSchema(InputStream in) throws IOException, ParserConfigurationException, SAXException {
        InputStream schema = schemaUrl.openConnection().getInputStream();

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setValidating(true);
            dbf.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
            dbf.setAttribute(JAXP_SCHEMA_SOURCE, schema);

            DocumentBuilder db = dbf.newDocumentBuilder();
            db.setErrorHandler(new ErrorHandlerImplementation());
            return db.parse(in);
        } finally {
            Closeables.closeQuietly(schema);
        }
    }

    private static String getClassNameForNamespaceURI(String namespaceURI) {
        URI uri = URI.create(namespaceURI);

        // todo: look into the predefined uris

        // Example: http://www.avast.com/schemas/com/avast/cloud/FileRepRequestHandler
        String path = uri.getPath();
        if (path.startsWith(SCHEMAS_PREFIX)) {
            String className = path.substring(SCHEMAS_PREFIX.length() + 1);
            className = className.replace('/', '.');
            return className;
        }

        return null;
    }

    private static InputStream findSchemaFromNamespaceURI(String namespaceURI) {
        URI uri = URI.create(namespaceURI);
        String path = uri.getPath() + ".xsd";

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // Try to find the schema in the path as is
        InputStream schemaStream = classLoader.getResourceAsStream(path);
        if (schemaStream == null) {
            // Try to find the schema in the path prefixed /com/avast
            String p = "/com/avast/" + path;
            schemaStream = classLoader.getResourceAsStream(p);
        }

        if (schemaStream == null && path.startsWith(SCHEMAS_PREFIX)) {
            // Try to find the schema in the path prefixed /schemas/
            String p = path.substring(SCHEMAS_PREFIX.length() + 1);
            schemaStream = classLoader.getResourceAsStream(p);
        }

        if (schemaStream == null) {
            throw new IllegalArgumentException("No schema associated with namespace " + namespaceURI);
        }

        return schemaStream;
    }

    private static class ConfigParserContentHandler implements ContentHandler, ErrorHandler {

        private ValidatorHandler vHandler;
        private Locator locator;
        private String startedPrefix;
        private String starterUri;
        private final DocumentBuilder db;
        private XmlConfigHandler configHandler;
        private String namespaceURI;

        public ConfigParserContentHandler() throws Exception {
            DocumentBuilderFactory dbf = DocumentBuilderFactoryImpl.newInstance(); // Xerces
            db = dbf.newDocumentBuilder();
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;

            if (vHandler == null) return;
            vHandler.setDocumentLocator(locator);
        }

        @Override
        public void startDocument() throws SAXException {
            if (vHandler == null) return;
            vHandler.startDocument();
        }

        @Override
        public void endDocument() throws SAXException {
            if (vHandler == null) return;
            vHandler.endDocument();
        }

        @Override
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            startedPrefix = prefix;
            starterUri = uri;

            if (vHandler == null) return;
            vHandler.startPrefixMapping(prefix, uri);
        }

        @Override
        public void endPrefixMapping(String prefix) throws SAXException {
            if (vHandler == null) return;
            vHandler.endPrefixMapping(prefix);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            if (vHandler == null) {

                this.namespaceURI = uri;
                InputStream schemaStream = findSchemaFromNamespaceURI(uri);
                XMLSchemaFactory sf = new XMLSchemaFactory(); // Xerces
                Schema schema = sf.newSchema(new StreamSource(schemaStream));
                this.vHandler = schema.newValidatorHandler();

                configHandler = new XmlConfigHandler(vHandler.getTypeInfoProvider());
                vHandler.setContentHandler(configHandler);
                vHandler.setErrorHandler(this);

                vHandler.setDocumentLocator(locator);
                vHandler.startDocument();
                startPrefixMapping(startedPrefix, starterUri);
                vHandler.startElement(uri, localName, qName, atts);
            } else {
                vHandler.startElement(uri, localName, qName, atts);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (vHandler == null) return;
            vHandler.endElement(uri, localName, qName);
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (vHandler == null) return;
            vHandler.characters(ch, start, length);
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            if (vHandler == null) return;
            vHandler.ignorableWhitespace(ch, start, length);
        }

        @Override
        public void processingInstruction(String target, String data) throws SAXException {
            if (vHandler == null) return;
            vHandler.processingInstruction(target, data);
        }

        @Override
        public void skippedEntity(String name) throws SAXException {
            if (vHandler == null) return;
            vHandler.skippedEntity(name);
        }

        public String getNamespaceURI() {
            return namespaceURI;
        }

        public Map<String, Property> getProperties() {
            return configHandler.getProperties();
        }

        // ErrorHandler

        @Override
        public void warning(SAXParseException exception) throws SAXException {
            LOGGER.warn("Warning when parsing document with namespace URI:" + this.getNamespaceURI(), exception);
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            LOGGER.error("Error when parsing document with namespace URI:" + this.getNamespaceURI(), exception);
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            LOGGER.error("Fatal error when parsing document with namespace URI:" + this.getNamespaceURI(), exception);
        }

        public List<XmlConfig.Decorator> getDecorators() {
            return configHandler.getDecorators();
        }
    }

    @Deprecated
    private final class ErrorHandlerImplementation implements ErrorHandler {

        @Override
        public void warning(SAXParseException exception) throws SAXException {
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }
    }

//    public static void main(String[] args) throws Exception {
//
//        File f = new File("/Users/slajchrt/Projects/avast/trunk/deployments/filerep-ma-proxy-deployment/src/main/resources/config/Train.xml");
//        final File schemaFile = new File("/Users/slajchrt/Projects/avast/trunk/netty-common/src/main/resources/com/avast/cloud/train-configuration.xsd");
//
//        //DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//        DocumentBuilderFactory dbf = DocumentBuilderFactoryImpl.newInstance(); // Xerces
//        dbf.setNamespaceAware(true);
//        //dbf.setValidating(true);
//        //dbf.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
//        //dbf.setAttribute(JAXP_SCHEMA_SOURCE, new FileInputStream(schemaFile));
//
//        final DocumentBuilder db = dbf.newDocumentBuilder();
//        db.setEntityResolver(new EntityResolver() {
//            @Override
//            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
//                return new InputSource(new FileReader(schemaFile));
//            }
//        });
//        db.setErrorHandler(new ErrorHandler() {
//            @Override
//            public void warning(SAXParseException exception) throws SAXException {
//                exception.printStackTrace();
//            }
//
//            @Override
//            public void error(SAXParseException exception) throws SAXException {
//                exception.printStackTrace();
//            }
//
//            @Override
//            public void fatalError(SAXParseException exception) throws SAXException {
//                exception.printStackTrace();
//            }
//        });
//
//        Document document = db.parse(f);
//
//        String namespaceURI = document.getDocumentElement().getNamespaceURI();
//        //String language =  XMLConstants.W3C_XML_SCHEMA_NS_URI ;
//        XMLSchemaFactory sf = new XMLSchemaFactory(); // Xerces
//        //sf.set.newInstance(language); // Xerces
//        Schema schema = sf.newSchema(schemaFile);
//        DOMSource ds = new DOMSource(document);
//        final ValidatorHandler validatorHandler = schema.newValidatorHandler();
//
//        TypeInfo schemaTypeInfo = document.getDocumentElement().getSchemaTypeInfo();
//
//        XMLReader parser = XMLReaderFactory.createXMLReader();
//        //parser.setContentHandler(validatorHandler);
//        parser.setContentHandler(new ConfigParserContentHandler());
//        parser.parse(new InputSource(new FileInputStream(f)));
//
//        System.out.println(document.getDocumentElement().getNodeName());
//
//    }

}
