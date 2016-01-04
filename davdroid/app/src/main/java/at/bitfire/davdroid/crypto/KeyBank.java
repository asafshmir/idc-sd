package at.bitfire.davdroid.crypto;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import at.bitfire.davdroid.resource.InvalidResourceException;

/**
 * Created by baruch on 1/2/2016.
 */
public class KeyBank {

    public class User {

        public User(String name, String sk, String signature) {
            this.name = name;
            this.sk = sk;
            this.signature = signature;
        }

        public String name;
        public String sk;
        public String signature;
    }

    Set<User> users;

    public KeyBank(String data) throws InvalidResourceException {

        users = new HashSet<User>();

        // Parse user data
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(data));
            Document doc = builder.parse(is);

            Element root = doc.getDocumentElement();
            NodeList children = root.getChildNodes();
            for(int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if(child.getNodeType() == Node.ELEMENT_NODE) {
                    Element echild = (Element) child;
                    users.add(new User(echild.getAttribute("name"),
                            echild.getAttribute("sk"),
                            echild.getAttribute("signature")));
                }
            }

        } catch(Exception e) {
            throw new InvalidResourceException(e.getMessage());
        }
    }

}
