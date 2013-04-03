package com.avast.syringe.config.internal;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.xerces.impl.dv.xs.XSSimpleTypeDecl;
import org.apache.xerces.impl.xs.XSComplexTypeDecl;
import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.apache.xerces.xs.XSAnnotation;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSTypeDefinition;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.TypeInfo;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.TypeInfoProvider;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * User: slajchrt
 * Date: 3/12/12
 * Time: 5:38 PM
 */
public class XmlConfigHandler extends DefaultHandler {

    private static final String DECORATORS = "decorators";

    private final List<XmlConfig.Decorator> decorators = Lists.newArrayList();

    public List<XmlConfig.Decorator> getDecorators() {
        return decorators;
    }

    abstract class ScopeHandler extends DefaultHandler {
        int counter = 0;
        ScopeHandler parent;

        @Override
        public final void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (counter == 0) {
                parent = contentHandler.peek();
                beginScope(uri, localName, qName, attributes);
            } else {
                ScopeHandler scopeHandler = createScopeHandler(uri, localName, qName, attributes);
                scopeHandler.startElement(uri, localName, qName, attributes);
                contentHandler.push(scopeHandler);
            }
            counter++;
        }

        @Override
        public final void endElement(String uri, String localName, String qName) throws SAXException {
            counter--;
            if (counter == 0) {
                endScope(uri, localName, qName);
                if (parent != this) {
                    parent.endElement(uri, localName, qName);
                }
            } else {
                ScopeHandler scopeHandler = contentHandler.pop();
                useScopeHandler(uri, localName, qName, scopeHandler);
            }
        }

        protected void beginScope(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        }

        protected void endScope(String uri, String localName, String qName) throws SAXException {
        }

        abstract protected ScopeHandler createScopeHandler(String uri, String localName, String qName, Attributes attributes);

        abstract protected void useScopeHandler(String uri, String localName, String qName, ScopeHandler scopeHandler);

    }

    class ConfigScopeHandler extends ScopeHandler {
        final Map<String, Property> props = Maps.newHashMap();

        @Override
        protected ScopeHandler createScopeHandler(String uri, String localName, String qName, Attributes attributes) {
            return createValueScopeHandler();
        }

        @Override
        protected void beginScope(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            String decorAttrValue = attributes.getValue(DECORATORS);
            if (decorAttrValue != null) {
                Iterable<String> decorNames = Splitter.on(',')
                        .trimResults()
                        .omitEmptyStrings()
                        .split(decorAttrValue);

                Iterables.addAll(decorators,
                        Iterables.transform(decorNames, new Function<String, XmlConfig.Decorator>() {
                            @Override
                            public XmlConfig.Decorator apply(@Nullable String decorName) {
                                return new XmlConfig.Decorator(decorName);
                            }
                        }));
            }
        }

        @Override
        protected void useScopeHandler(String uri, String localName, String qName, ScopeHandler scopeHandler) {
            List<Value> values = ((ValueScopeHandler) scopeHandler).getValues();
            Property property = new Property(localName, values);
            props.put(property.getName(), property);
        }
    }

    abstract class ValueScopeHandler extends ScopeHandler {
        abstract List<Value> getValues();
    }

    class ScalarValueScopeHandler extends ValueScopeHandler {

        final StringBuilder stringBuilder = new StringBuilder();

        @Override
        protected ScopeHandler createScopeHandler(String uri, String localName, String qName, Attributes attributes) {
            throw new IllegalStateException("Scalar value cannot have inner elements");
        }

        @Override
        protected void useScopeHandler(String uri, String localName, String qName, ScopeHandler scopeHandler) {
            throw new IllegalStateException("Scalar value cannot have inner elements");
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            stringBuilder.append(ch, start, length);
        }

        @Override
        List<Value> getValues() {
            return ImmutableList.of(new Value(stringBuilder.toString()));
        }
    }

    class ListValueScopeHandler extends ValueScopeHandler {

        private final List<Value> values = Lists.newArrayList();

        @Override
        protected ScopeHandler createScopeHandler(String uri, String localName, String qName, Attributes attributes) {
            return createValueScopeHandler();
        }

        @Override
        protected void useScopeHandler(String uri, String localName, String qName, ScopeHandler scopeHandler) {
            List<Value> vv = ((ValueScopeHandler) scopeHandler).getValues();
            values.addAll(vv);
        }

        @Override
        List<Value> getValues() {
            return values;
        }

    }

    class MapValueScopeHandler extends ValueScopeHandler {

        private final List<Value> values = Lists.newArrayList();
        private String key;

        @Override
        protected ScopeHandler createScopeHandler(String uri, String localName, String qName, Attributes attributes) {
            key = attributes.getValue("key");
            return createValueScopeHandler();
        }

        @Override
        protected void useScopeHandler(String uri, String localName, String qName, ScopeHandler scopeHandler) {
            List<Value> vv = ((ValueScopeHandler) scopeHandler).getValues();
            for (Value value : vv) {
                values.add(new MapEntry(key, value.getValue(), value.getRefType()));
            }
        }

        @Override
        List<Value> getValues() {
            return values;
        }

    }

    class ReferenceValueScopeHandler extends ScalarValueScopeHandler {

        final String typeName;

        public ReferenceValueScopeHandler(String typeName) {
            this.typeName = typeName;
        }

        @Override
        List<Value> getValues() {
            return ImmutableList.of(new Value(stringBuilder.toString(), typeName));
        }
    }

    private final TypeInfoProvider typeInfoProvider;
    private final Stack<ScopeHandler> contentHandler = new Stack<ScopeHandler>();
    private Map<String, Property> props;

    private static final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactoryImpl.newInstance(); // Xerces

    public XmlConfigHandler(TypeInfoProvider typeInfoProvider) {
        this.typeInfoProvider = typeInfoProvider;
    }

    @Override
    public void startDocument() throws SAXException {
        this.contentHandler.push(new ConfigScopeHandler());
    }

    @Override
    public void endDocument() throws SAXException {
        props = ((ConfigScopeHandler) this.contentHandler.pop()).props;
    }

    public Map<String, Property> getProperties() {
        return props;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        contentHandler.peek().startElement(uri, localName, qName, attributes);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        contentHandler.peek().endElement(uri, localName, qName);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        contentHandler.peek().characters(ch, start, length);
    }

    enum ValueScopeHandlerType {
        LIST, MAP, REF, SCALAR
    }

    private ValueScopeHandler createValueScopeHandler() {
        switch (getValueScopeHandlerType(typeInfoProvider.getElementTypeInfo())) {
            case LIST:
                return new ListValueScopeHandler();
            case MAP:
                return new MapValueScopeHandler();
            case REF:
                return new ReferenceValueScopeHandler(typeInfoProvider.getElementTypeInfo().getTypeName());
            case SCALAR:
                return new ScalarValueScopeHandler();
            default:
                throw new UnsupportedOperationException();
        }
    }

    static ValueScopeHandlerType getValueScopeHandlerType(TypeInfo elementTypeInfo) {
        PropertyTypeInfoAnnotation ta = null;

        if (elementTypeInfo instanceof XSComplexTypeDecl || elementTypeInfo instanceof XSSimpleTypeDecl) {

            XSObjectList annotations;
            XSTypeDefinition baseType;
            if (elementTypeInfo instanceof XSComplexTypeDecl) {
                XSComplexTypeDecl typeInfo = (XSComplexTypeDecl) elementTypeInfo;
                baseType = typeInfo.getBaseType();
                annotations = typeInfo.getAnnotations();
            } else {
                XSSimpleTypeDecl typeInfo = (XSSimpleTypeDecl) elementTypeInfo;
                baseType = typeInfo.getBaseType();
                annotations = ((XSSimpleTypeDecl) elementTypeInfo).getAnnotations();
            }

            for (int i = 0; i < annotations.getLength(); i++) {
                ta = readTypeInfoAnnotation((XSAnnotation) annotations.item(i));
                if (ta != null) {
                    break;
                }
            }

            if (ta == null && baseType != null) {
                return getValueScopeHandlerType((TypeInfo) baseType);
            }
        }

        ValueScopeHandlerType valueScopeHandlerType;

        if (ta != null) {
            // structured
            String propTypeInfo = ta.getPropertyTypeInfo();
            if ("list".equals(propTypeInfo)) {
                valueScopeHandlerType = ValueScopeHandlerType.LIST;
            } else if ("map".equals(propTypeInfo)) {
                valueScopeHandlerType = ValueScopeHandlerType.MAP;
            } else if ("reference".equals(propTypeInfo)) {
                valueScopeHandlerType = ValueScopeHandlerType.REF;
            } else {
                throw new IllegalArgumentException("Unknown property type " + propTypeInfo +
                        " (for element type " + elementTypeInfo.getTypeName() + ")");
            }
        } else {
            valueScopeHandlerType = ValueScopeHandlerType.SCALAR;
        }
        return valueScopeHandlerType;
    }

    static PropertyTypeInfoAnnotation readTypeInfoAnnotation(XSAnnotation item) {
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document annotDoc = documentBuilder.newDocument();

            item.writeAnnotation(annotDoc, XSAnnotation.W3C_DOM_DOCUMENT);

            NodeList appInfoList = annotDoc.getDocumentElement().getElementsByTagNameNS(XmlConfigParser.W3C_XML_SCHEMA, "appinfo");
            if (appInfoList != null && appInfoList.getLength() == 1) {
                Element appInfoElem = (Element) appInfoList.item(0);
                if ("type_info".equals(appInfoElem.getTextContent())) {
                    NodeList docNodeList = annotDoc.getDocumentElement().getElementsByTagNameNS(XmlConfigParser.W3C_XML_SCHEMA, "documentation");
                    Preconditions.checkNotNull(docNodeList, "No documentation node in the annotation node");
                    Preconditions.checkState(docNodeList.getLength() == 1, "Only one documentation node allowed in the annotation node");

                    String typeInfo = docNodeList.item(0).getTextContent();
                    return new PropertyTypeInfoAnnotation(typeInfo);
                }

            }

            return null;

        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

}
