package etienne.dbpedia_execution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONObject;

public class Starter {
	
    public static void main( String[] args )
    {
    	org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
 
    	String jsonString;
    		
    	jsonString =  "{\"AnswerType\":\"COUNT\","
    		    + "\"Query\":\"SELECT (COUNT (DISTINCT ?X) as ?count) { <http://dbpedia.org/resource/Bill_Clinton> <http://dbpedia.org/property/children> ?X . }\","
    		    + "\"Question\":\"How many children does Bill Clinton have?\","
    		    + "\"DataModel\":{\"instances\":[\"Bill Clinton\", \"children\"],\"values\":[],\"categories\":[],\"relations\":[\"start\"]},"
    		    + "\"QueryType\":\"COUNT\"}";

    	
    //	jsonString = "{\"AnswerType\":\"foaf:Person\",\"Question\":\"Who is the wife of Barack Obama?\",\"Query\":\"PREFIX foaf: <http://xmlns.com/foaf/0.1/> PREFIX dc: <http://purl.org/dc/elements/1.1/> PREFIX dbp: <http://dbpedia.org/property/> PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>  SELECT DISTINCT ?X  WHERE { ?y foaf:name \\\"Barack Obama\\\"@en . ?y dbp:spouse ?X .}\", \"DataModel\":{\"instances\":[\"Barack Obama\",\"Spouse\"],\"values\":[],\"categories\":[],\"relations\":[\"start\"]},\"QueryType\":\"SELECT\"}";
    	//String jsonString = "{\"AnswerType\":\"Actor\",\"Question\":\"Is Natalie Portman an Actress?\",\"Query\":\" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX foaf: <http://xmlns.com/foaf/0.1/> ASK { ?x foaf:name \\\"Natalie Portman\\\"@en ; rdf:type foaf:Person .}\", \"DataModel\":{\"instances\":[\"Actress\",\"Natalie Portman\"],\"values\":[],\"categories\":[],\"relations\":[\"start\"]},\"QueryType\":\"ASK\"}";
    	
//    	
//    	jsonString = "{\"AnswerType\":\"<http://dbpedia.org/ontology/Country>\","
//    		    + "\"Query\":\"SELECT ?X { <http://dbpedia.org/resource/Ganges> <http://dbpedia.org/property/sourceCountry> ?X . }\""
//    		    + ",\"Question\":\"In which country does the Ganges start?\""
//    		    + ",\"DataModel\":{\"instances\":[\"Ganges\", \"country\"],\"values\":[],\"categories\":[],\"relations\":[\"start\"]},"
//    		    + "\"QueryType\":\"SELECT\"}";
    		  
//    		  jsonString = "{\"AnswerType\":\"foaf:Person\","
//    		    + "\"Query\":\"SELECT ?X { <http://dbpedia.org/resource/John_F._Kennedy> <http://dbpedia.org/ontology/vicePresident> ?X . }\""
//    		    + ",\"Question\":\"Who was John F. Kennedy's vice president?\""
//    		    + ",\"DataModel\":{\"instances\":[\"John F. Kennedy\", \"vice president\"],\"values\":[],\"categories\":[],\"relations\":[\"start\"]},"
//    		    + "\"QueryType\":\"SELECT\"}";
//    		  
//    		  jsonString = "{\"AnswerType\":\"foaf:Person\","
//    		    + "\"Query\":\"SELECT ?X WHERE{ <http://dbpedia.org/resource/The_Prodigy> <http://dbpedia.org/ontology/bandMember> ?X . }\""
//    		    + ",\"Question\":\"Give me all members of the Prodigy.\""
//    		    + ",\"DataModel\":{\"instances\":[\"Prodigy\", \"members\"],\"values\":[],\"categories\":[],\"relations\":[\"start\"]},"
//    		    + "\"QueryType\":\"SELECT\"}";
//    		  
//    		  jsonString = "{\"AnswerType\":\"foaf:Person\","
//    		    + "\"Query\":\"SELECT DISTINCT ?X WHERE { ?X <http://dbpedia.org/ontology/occupation> <http://dbpedia.org/resource/Bandleader> . ?X <http://dbpedia.org/ontology/instrument> <http://dbpedia.org/resource/Trumpet> . }\""
//    		    + ",\"Question\":\"Give me a list of all bandleaders that play trumpet.\""
//    		    + ",\"DataModel\":{\"instances\":[\"bandleaders\", \"trmpet\"],\"values\":[],\"categories\":[],\"relations\":[\"start\"]},"
//    		    + "\"QueryType\":\"SELECT\"}";
//    		  
//    		  jsonString = "{\"AnswerType\":\"foaf:Person\","
//    		    + "\"Query\":\"SELECT DISTINCT ?X WHERE { ?X <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/FormulaOneRacer> . ?X <http://dbpedia.org/ontology/races> ?Y . } ORDER BY DESC(?Y) OFFSET 0 LIMIT 1\""
//    		    + ",\"Question\":\"Who is the formula 1 race driver with the most races?\""
//    		    + ",\"DataModel\":{\"instances\":[\"formaul 1\", \"driver\", \"races\"],\"values\":[],\"categories\":[],\"relations\":[\"start\"]},"
//    		    + "\"QueryType\":\"SELECT\"}";
//    	

    	Translator t = new Translator();
    	
    	JSONObject jsonObject = new JSONObject(jsonString);
    	
    	QueryType queryType;
    	
    	// parse JSON 
    	String originalQuestion = jsonObject.getString("Question");
    	String answerType = jsonObject.getString("AnswerType");
    	String queryTypeString = jsonObject.getString("QueryType"); 
     	String queryString = jsonObject.getString("Query");
    	List<String> questionInstances = t.extractListFromJson(jsonString,"DataModel","instances");
    	
    	// parse queryType
    	if(queryTypeString.equals("SELECT"))
    		queryType = QueryType.SELECT;
    	else if(queryTypeString.equals("ASK"))
    		queryType = QueryType.ASK;
    	else if (queryTypeString.equals("COUNT"))
    		queryType = QueryType.COUNT;
    	else 
    		queryType = QueryType.MIN_MAX; 
    
    	// build environment
    	ExecutionEnvironment ex = new ExecutionEnvironment(originalQuestion, queryString, queryType, answerType, questionInstances);    	
    
    	JSONObject result;
    	
    	if(jsonObject.getString("AnswerType").equals("NONE"))
    		result = ex.processEmptyType();
    	else
    		result = ex.processQuery();
    	
    	
    	// print result
    	System.out.println(result.toString());
    }
}