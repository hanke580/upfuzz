package org.zlab.upfuzz.fuzzingengine.configgen.xml;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class PropertyHandler extends DefaultHandler {

    private boolean nameToParse = false;
    private boolean valueToParse = false;

    // List to hold Users object
    private List<Property> propertyList = null;
    private Property property = null;

    public List<Property> getPropertyList() {
        return propertyList;
    }

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) {

        if (qName.equalsIgnoreCase("Property")) {
            // initialize User object and set attribute
            property = new Property();
            String name = attributes.getValue("name");
            if (name != null) {
                property.setName(name);
            }
            String value = attributes.getValue("value");
            if (value != null) {
                property.setValue(value);
            }

            // initialize list
            if (propertyList == null) {
                propertyList = new ArrayList<>();
            }
        } else if (qName.equalsIgnoreCase("name")) {
            nameToParse = true;
        } else if (qName.equalsIgnoreCase("value")) {
            valueToParse = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (qName.equalsIgnoreCase("Property")) {
            propertyList.add(property);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (property == null) {
            return;
        }

        if (nameToParse) {
            property.setName(new String(ch, start, length));
            nameToParse = false;
        } else if (valueToParse) {
            property.setValue(new String(ch, start, length));
            valueToParse = false;
        }
    }
}
