package semsim.utilities.webservices;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import semsim.SemSimLibrary;
import semsim.annotation.Ontology;
import semsim.definitions.SemSimConstants;
import semsim.owl.SemSimOWLFactory;


public class BioPortalSearcher {
	private static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper();
	public HashMap<String,String> search(SemSimLibrary lib, String text, String bioportalID, int exactmatch) throws IOException, JDOMException{
		text = text.replace(" ", "+");
		
		boolean exactmatchbool = exactmatch==1; 
		
		URL url;
		if (exactmatch==2) {
			url = new URL(
					"http://data.bioontology.org/search?q=" + bioportalID + ":"
							+ text + "&ontologies="
							+ bioportalID + "&format=xml" + "&include=prefLabel,synonym,definition,notation,cui,semanticType,properties"
							+ "&apikey=" + SemSimConstants.BIOPORTAL_API_KEY + "&also_search_properties=true");

		}
		else {
					url = new URL(
				"http://data.bioontology.org/search?q="
						+ text + "&ontologies="
						+ bioportalID + "&format=xml" + "&exact_match=" + exactmatchbool
						+ "&apikey=" + SemSimConstants.BIOPORTAL_API_KEY);

		}
		
		System.out.println(url);
		URLConnection yc = url.openConnection();
		yc.setRequestProperty("Accept", "application/xml");
		yc.setReadTimeout(60000); // Timeout after a minute
		BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
		Document doc = new SAXBuilder().build(in);
		in.close();
		
		HashMap<String,String> rdflabelsanduris = new HashMap<String,String>();
		// Process XML results from BioPortal REST service
		if (doc!=null) {
			if(doc.getRootElement().getName().equals("nilClass"))
				System.out.println("No matches found for " + text);
			else{
				List<?> resultlist = (List<?>)doc.getRootElement().getChild("collection").getChildren("class");
				Iterator<?> resultsiterator = resultlist.iterator();

				while (resultsiterator.hasNext()) {
					Element nextel = (Element) resultsiterator.next();
					String preferredLabel = nextel.getChildText("prefLabel");
					String uri = nextel.getChildText("id");
	
					// Only collect terms from the queried ontology; don't show terms imported from other ontologies
					Ontology ont = lib.getOntologyfromTermURI(uri);
					
					if(ont.getNickName().equals(bioportalID)){
						rdflabelsanduris.put(preferredLabel, uri);
					}

				}
			}
		}
		return rdflabelsanduris;
	}
	
	public static String getRDFLabelUsingBioPortal(String id, String bioportalontID){
		String label = null;

		try {
			System.out.println("Looking up " + id);
			URL url = new URL(
					"http://data.bioontology.org/ontologies/" + bioportalontID + "/classes/" + id);
			System.out.println(url);
	        HttpURLConnection conn;
	        BufferedReader rd;
	        String line;
	        String result = "";
	        conn = (HttpURLConnection) url.openConnection();
	        conn.setRequestMethod("GET");
	        conn.setRequestProperty("Authorization", "apikey token=" + SemSimConstants.BIOPORTAL_API_KEY);
	        conn.setRequestProperty("Accept", "application/json");
			conn.setReadTimeout(60000); // Timeout after a minute
			
	        rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	        while ((line = rd.readLine()) != null) {
	            result += line;
	        }
	        rd.close();
	        	        
	        // process resulting input stream
            JsonNode root = JSON_OBJECT_MAPPER.readTree(result);
            JsonNode labelnode = root.get("prefLabel");
            if(labelnode!=null)
            	label = labelnode.textValue();
		}
        catch (Exception e) {
			e.printStackTrace();
		}
		return label;
	}
	
	public static String getBioPortalIDfromTermURI(String termuri){
		if(SemSimConstants.ONTOLOGY_NAMESPACES_AND_FULL_NAMES_MAP.containsKey(SemSimOWLFactory.getNamespaceFromIRI(termuri.toString()))){
			String fullname = SemSimConstants.ONTOLOGY_NAMESPACES_AND_FULL_NAMES_MAP.get(SemSimOWLFactory.getNamespaceFromIRI(termuri.toString()));
			if(BioPortalConstants.ONTOLOGY_FULL_NAMES_AND_BIOPORTAL_IDS.containsKey(fullname)){
				return BioPortalConstants.ONTOLOGY_FULL_NAMES_AND_BIOPORTAL_IDS.get(fullname);
			}
		}
		return null;
	}
	
	public static Boolean testBioPortalWebservice(){
		SAXBuilder builder = new SAXBuilder();
		Document doc = null;
		try {
			URL url = new URL(
					"http://data.bioontology.org/ontologies/OPB?q&format=xml&apikey=" + SemSimConstants.BIOPORTAL_API_KEY);
			System.out.println("Testing: " + url);
			URLConnection yc = url.openConnection();
			yc.setRequestProperty("Accept", "application/xml");
			yc.setReadTimeout(60000); // Tiemout after a minute
			BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
			doc = builder.build(in);
			in.close();
		} catch (IOException | JDOMException e) {
			e.printStackTrace();
			return false;
		}

		// Process XML results from BioPortal REST service to see if we're online
		if (doc!=null) {
			if(doc.getRootElement()!=null){
				if(doc.getRootElement().getName().equals("ontology")){
					System.out.println("Received response from BioPortal");
					return true;
				}
			}
		}
		return false;
	}
}
