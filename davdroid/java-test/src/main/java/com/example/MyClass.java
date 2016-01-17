package com.example;

import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.ec.ECElGamalEncryptor;
import org.spongycastle.crypto.engines.ElGamalEngine;
import org.spongycastle.jcajce.provider.asymmetric.ElGamal;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.spec.ECParameterSpec;
import org.spongycastle.openssl.PEMParser;
import org.spongycastle.openssl.PEMWriter;
import org.spongycastle.util.io.pem.PemWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECPoint;

import javax.crypto.Cipher;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MyClass {

    // TODO for user authentication with asymetric signature use bouncy-castle: elyptic curve, DSA
    // TODO replace with bouncy-castle: elyptic curve, el-gamal, 400 bits

    public static void main(String[] args) throws Exception {

        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);

        ElGamalEngine eg; = new ElGamal();
        eg.


        byte[] input = "ab".getBytes();
        Cipher cipher = null;//Cipher.getInstance("ElGamal/None/NoPadding", "SC");
        KeyPairGenerator generator = KeyPairGenerator.getInstance("ElGamal", "SC");
        SecureRandom random = new SecureRandom();

        generator.initialize(400, random);

        KeyPair pair = generator.generateKeyPair();
        Key pubKey = pair.getPublic();
        Key privKey = pair.getPrivate();
        cipher.init(Cipher.ENCRYPT_MODE, pubKey, random);
        byte[] cipherText = cipher.doFinal(input);
        System.out.println("cipher: " + new String(cipherText));

        cipher.init(Cipher.DECRYPT_MODE, privKey);
        byte[] plainText = cipher.doFinal(cipherText);
        System.out.println("plain : " + new String(plainText));

        /*ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("B-571");
        KeyPairGenerator g = KeyPairGenerator.getInstance("ElGamal", "SC");
        g.initialize(ecSpec, new SecureRandom());
        KeyPair p = g.generateKeyPair();
        //AsymmetricCipherKeyPair p = g.generateKeyPair();

        PEMWriter w = new PEMWriter(new OutputStreamWriter(System.out));
        w.writeObject(p.getPublic());
        w.flush();

        PEMParser parser = new PEMParser(new FileReader("c:\\temp\\key.txt"));
        System.out.println(parser.readObject().getClass());

        //System.out.println(p.getPublic().getEncoded());
        */
    }

   /* public static void main(String[] args) {
        try {

            String data = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<KEY-CONFIG>\n" +
                    "<USER name=\"russo\" sk=\"1234\" signature=\"5678\"/>\n" +
                    "</KEY-CONFIG>";


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
                    System.out.println("Name: " + echild.getAttribute("name"));
                    System.out.println("SK: " + echild.getAttribute("sk"));
                    System.out.println("Signature: " + echild.getAttribute("signature"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/
}
