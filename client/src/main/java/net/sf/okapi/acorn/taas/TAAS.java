package net.sf.okapi.acorn.taas;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.sf.okapi.acorn.common.BaseXLIFFProcessor;
import net.sf.okapi.acorn.common.NSContext;
import net.sf.okapi.acorn.common.Util;
import net.sf.okapi.acorn.xom.Factory;

import org.apache.commons.httpclient.HttpClient;
import org.oasisopen.xliff.om.v1.IExtObject;
import org.oasisopen.xliff.om.v1.IExtObjects;
import org.oasisopen.xliff.om.v1.ISegment;
import org.oasisopen.xliff.om.v1.IUnit;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class TAAS extends BaseXLIFFProcessor {

	private static final String BASEURL = "https://api.taas-project.eu";
	private static final String GLS_URI = Util.NS_XLIFF20_GLOSSARY;

	private final DocumentBuilder docBuilder;
	private final String srcLang = "en";
	private final String trgLang = "fi"; // For demo
	private final String transacUser = "ysavourel";
	
	private HttpClient client;
	private Credentials credentials;
	private List<Entry> terms;

	private class Entry {
		
		String source;
		List<String> targets;

		public Entry () {
			targets = new ArrayList<>();
		}
	}
	
	public TAAS ()
		throws ParserConfigurationException
	{
		client = new HttpClient();
        client.getParams().setParameter("http.useragent", "Okapi-Acorn");
        credentials = new Credentials();

        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(true);
		docBuilder = domFactory.newDocumentBuilder();
	}

	@Override
	public void process (IUnit unit) {
		terms = new ArrayList<>();
		try {
			for ( ISegment seg : unit.getSegments() ) {
				process(seg.getSource().getCodedText());
			}
			addGlossaryEntries(unit);
		}
		catch ( Throwable e ) {
			throw new RuntimeException("Error processing unit id="+unit.getId(), e);
		}
	}

	private void process (String codedText)
		throws MalformedURLException, IOException, XPathExpressionException,
		ParserConfigurationException, SAXException
	{
        StringBuilder r = new StringBuilder();
        r.append(BASEURL).append("/extraction/");
        r.append("?sourceLang=").append(srcLang);
        r.append("&targetLang=").append(trgLang); // Hard-coded for the demo
        r.append("&method=").append("4"); //4
        HttpURLConnection conn;
        conn = (HttpURLConnection) new URL(r.toString()).openConnection();
        conn.setRequestProperty("Authorization", credentials.getBasic());
        String tuk = credentials.getUserKey();
        if ( tuk != null ) {
            conn.setRequestProperty("TaaS-User-Key", tuk);
        }
        conn.setRequestProperty("Accept", "text/xml");
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/plain");
        conn.setDoOutput(true);
        OutputStream out = conn.getOutputStream();
        try {
            out.write(codedText.getBytes(StandardCharsets.UTF_8));
        }
        finally {
            out.close();
        }

        String res = null;
        InputStream in = conn.getInputStream();
        try {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int readBytes;
            while ((readBytes = in.read(b)) > 0) o.write(b, 0, readBytes);
            res = new String(o.toByteArray(), StandardCharsets.UTF_8);
        }
        finally {
            in.close();
        }	        
//        System.out.println(res);
        parseTerms(res);
	}

	private void parseTerms (String res)
		throws ParserConfigurationException, SAXException, IOException, XPathExpressionException
	{
		// Get the TBX chunk
		InputSource is = new InputSource(new StringReader(res));
		Document doc = docBuilder.parse(is);
		XPath xpath = XPathFactory.newInstance().newXPath();
		xpath.setNamespaceContext(new NSContext());
		XPathExpression expr = xpath.compile("/extractionResult/terms");
		Node node = (Node)expr.evaluate(doc, XPathConstants.NODE);
		String data = node.getTextContent();
		
		is = new InputSource(new StringReader(data));
		doc = docBuilder.parse(is);

		// Get the term entries
		expr = xpath.compile("/martif/text/body/termEntry");
		NodeList termEntryList = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
		
		for ( int i=0; i<termEntryList.getLength(); i++ ) {
			// Get the source
			expr = xpath.compile("langSet[@lang='"+srcLang+"' or @xml:lang='"+srcLang+"']/ntig/termGrp/term");
			String source = (String)expr.evaluate(termEntryList.item(i), XPathConstants.STRING);
			
			// Check if it exists already
			boolean exists = false;
			for ( Entry ent : terms ) {
				if ( ent.source.equals(source) ) {
					exists = true;
					break;
				}
			} // If it does exists do not store it twice.
			if ( exists ) continue;
			
			// Add the source
			Entry entry = new Entry();
			entry.source = source;
			
			// Get the target
			expr = xpath.compile("langSet[@lang='"+trgLang+"' or @xml:lang='"+trgLang+"']/"
				+"ntig[transacGrp/transacNote[@type='responsibility']='"+transacUser+"']/termGrp/term");
			NodeList list = (NodeList)expr.evaluate(termEntryList.item(i), XPathConstants.NODESET);
			for ( int j=0; j<list.getLength(); j++ ) {
				entry.targets.add(list.item(j).getTextContent());
//				System.out.println("trg: [["+entry.targets.get(j)+"]]");
			}
			
			if ( !entry.targets.isEmpty() ) {
				terms.add(entry);
			}
		}
	}

	private void addGlossaryEntries (IUnit unit) {
		// Do we have translations?
		if ( terms.isEmpty() ) return;

		// Get or create the glossary element
		IExtObjects eos = unit.getExtObjects();
		IExtObject eo = eos.add(GLS_URI, "glossary");
		// Add entries to the glossary
		for ( Entry ent : terms ) {
			// Add the glossEntry element
			IExtObject entry = Factory.XOM.createExtObject(GLS_URI, "glossEntry");
			eo.getItems().add(entry);
			IExtObject term = Factory.XOM.createExtObject(GLS_URI, "term");
			entry.getItems().add(term);
			term.add(ent.source, false);
			// Add the translation elements
			for ( String tra : ent.targets ) {
				IExtObject trans = Factory.XOM.createExtObject(GLS_URI, "translation");
				entry.getItems().add(trans);
				trans.add(tra, false);
			}
		}
	}
	
}
