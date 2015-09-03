import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class main {
    
    public static final String regex="^<string lang=\"en\"><\\!\\[CDATA\\[(.*)]\\]></string>$";
    public static final MaxentTagger tagger =  new MaxentTagger("/Users/Dennis/Downloads/stanford-postagger-2015-04-20/models/english-left3words-distsim.tagger");
    
    public static final String Questionfile="/Users/Dennis/Downloads/qald-4_multilingual_train.xml";
        
    public static final String ListQuetionFile = "/Users/Dennis/Downloads/output.txt";
    
    public static void main(String[] args) {
    	JSONObject result = new JSONObject();
    	//MaxentTagger tagger =  new MaxentTagger("C:\\Users\\Tanguy\\Documents\\NetBeansProjects\\QuestionAnalysis\\conf\\models\\english-left3words-distsim.tagger");
    	
    	//String question="Is Natalie Portman an actress?";
    	//String question="Who is the wife of Barack Obama?";
    	//String question="How many children does Bill Clinton have?";
    	//String question="In which country does the Nile start?";
    	////String question="Who was John F. Kennedy's vice president?";
    	//String question="Give me all professional skateboarders of Sweeden?";
    	////String question="Give me a list of all bandleaders that play trompet?";
    	//String question="Who is the Formula 1 race driver with the most races?";
    	//String question="Give me all members of Prodigy";
    	
    	
    	String question="Give me all cars that are produced in Germany.";
    	String tagged = tagger.tagString(question);
    	System.out.println(tagged);
    	result = parseEnglishSentances(question);
    	System.out.println(result.toString());
    	
    	//To load all QALD questions
    	//try {
    	//	result=testQALD(ListQuetionFile);
    	//} catch (IOException e) {
    	//	e.printStackTrace();
    	//}	
    }
        	
	public static void parseXML(String input, String output) throws IOException{
        try (BufferedReader br = new BufferedReader(new FileReader(input)); BufferedWriter bw = new BufferedWriter(new FileWriter(output))) {
            String line;
            Pattern p = Pattern.compile(regex);
            while ((line = br.readLine()) != null){
                Matcher m = p.matcher(line);
                if (m.matches()){
                    bw.write(m.group(1));
                    bw.newLine();
                }
            }
        }
	}
    
	public static JSONObject testQALD(String inputFile) throws IOException{
	    	JSONObject result = new JSONObject();
	    	try (BufferedReader br = new BufferedReader(new FileReader(inputFile))){
	    		String line;
	            while ((line = br.readLine()) != null){
	            	result = parseEnglishSentances(line);
	            	System.out.println(result.toString());
	            }
	    	}
	    return result;
	}
	
	
    public static JSONObject parseEnglishSentances(String question) {
    	JSONObject result = new JSONObject();	
    	result.put("Question", question);
    	List<String> listTag = new ArrayList<>();
        List<String> listWords = new ArrayList<>();
        String tagString = "";
        String wordString = "";
        String tagged = tagger.tagString(question);
        
        //System.out.println(tagged + "\n");
        for(String s : tagged.split(" ")){
            String[] split = s.split("_");
            if(split.length == 2){
                listTag.add(split[1]);
                listWords.add(split[0]);
            }
        }
        
        for(String s : listTag){
            tagString += s + " ";
            //System.out.print(s + "\t");
        }
        
        //System.out.println("");
        for(String s : listWords){
            wordString += s + " ";
            //System.out.print(s + "\t");
        }
        //System.out.println("");

        //Basic named entity recognition
        
        for(int i=0 ; i<listWords.size(); i++){
        	if (i==0 && listTag.get(0).contains("NNP")){
        		listTag.set(i, "NE");
        	}
        	if (i!=0 && !listWords.get(i).equals(listWords.get(i).toLowerCase())){
            	listTag.set(i, "NE");
            	if (i+1<listTag.size() && listTag.get(i+1).contains("CD")){
            		listTag.set(i+1, "NE");
            	}
            }
            //System.out.print(s + "\t");
        }
        
        List<List<Integer>> pos = new ArrayList<>();
        String queryType=(queryTypeDetermination(tagString, wordString));
        result.put("QueryType", queryType);
        String answerType=(answerTypeDetermination(listTag, listWords));
        result.put("AnswerType", answerType);
        //System.out.println(queryType + "\n--");
        
        //Instance extraction
        //System.out.print("Instances:");
        JSONArray instancesArray = new JSONArray();
        List<String> instances = extractRegexWords(listTag, listWords,"(NN.   )?(NNP.  )+(NN.   )?",pos);
        pos.clear();
        for(String s : instances){
            instancesArray.add(s);
        	System.out.print(s + ", ");
        }
        instances = extractRegexWords(listTag, listWords,"((NE    )+(IN    ))?(NE    )+(NN    )*",pos);
        pos.clear();
        for(String s : instances){
            instancesArray.add(s);
        	//System.out.print(s + ", ");
        }
        //System.out.println();
        
        //Relation extraction
        //System.out.print("Relations:");
        JSONArray relationsArray = new JSONArray();
        List<String> test = extractRegexWords(listTag, listWords,"(V...  DT    (NN    )*IN    )|(V...  IN    )|(V...  )",pos); //
        for(String s : test){
        	s=s.toLowerCase().substring(0, s.length()-1);
        	if (s.equals("give") || s.equals("do") || s.equals("does") || s.equals("did") || s.equals("is") || s.equals("are") || s.equals("was") || s.equals("were") || s.equals("have") || s.equals("has") || s.equals("had") || s.equals("been")){
        		test.remove(s);
        	} else{
        		relationsArray.add(s);
        		//System.out.print(s + ", ");
        	}
        }
        test = extractRegexWords(listTag, listWords,"(JJS   NN    )",pos); //
        for(String s : test){
        	String[] split=s.split(" ");
        	relationsArray.add(split[0]);
        		//System.out.print(s + ", ");
        	
        }
        test = extractRegexWords(listTag, listWords,"(CD    NN.   )",pos); //
        for(String s : test){
        	String[] split=s.split(" ");
        	relationsArray.add(split[1]);
        		//System.out.print(s + ", ");
        	
        }
        pos.clear();
        //System.out.println();
        
        //Values
        //System.out.print("Values:");
        JSONArray valuesArray = new JSONArray();
        test = extractRegexWords(listTag, listWords,"(CD    )",pos); 
        for(String s : test){
        	valuesArray.add(s);
            //System.out.print(s + ", ");
        }
        pos.clear();
        //System.out.println();
        
        //Category
        //System.out.print("Category:");
        JSONArray categoriesArray = new JSONArray();
        test = extractRegexWords(listTag, listWords,"(JJ    )*(NN.   )+",pos); //
        for(int i=0; i < test.size() ; i++){
        	if ((pos.get(i).get(0)-1>=0 && listWords.get(pos.get(i).get(0)-1).toLowerCase().equals("which")) ||
        		(pos.get(i).get(0)-1>=0 && listWords.get(pos.get(i).get(0)-1).toLowerCase().equals("most")) ||	
        		(pos.get(i).get(0)-1>=0 && listWords.get(pos.get(i).get(0)-1).toLowerCase().equals("how")) ||
        		(pos.get(i).get(0)-3>=0 && listWords.get(pos.get(i).get(0)-3).toLowerCase().equals("give") && listWords.get(pos.get(i).get(0)-2).toLowerCase().equals("me"))){
        		if (test.get(i).contains("many")){
        			String temp=test.get(i).replace("many", "");
        			test.set(i, temp);
        		}
        		categoriesArray.add(test.get(i));
        		//System.out.print(test.get(i));
        	}
        	//System.out.println(pos.get(i).get(0)  + " " +pos.get(i).get(1) );
            //System.out.print(test.get(i) + ", ");
        }
        test = extractRegexWords(listTag, listWords,"(JJS   NN    )",pos);
        for(String s : test){
        	String[] split=s.split(" ");
        	categoriesArray.add(split[1]);
        	//System.out.print(s + ", ");
        }
        test = extractRegexWords(listTag, listWords,"(DT    NN    )",pos); //
        for(String s : test){
        	String[] split=s.split(" ");
        	if (split[0].contains("a") || split[0].contains("an")){
        		categoriesArray.add(split[1]);
        	}
        		//System.out.print(s + ", ");
        	
        }
        //test = extractRegexWords(listTag, listWords,"(NN    )",pos); //
        //if listWords.get(pos.get(i).get(0)-1)
        //for(String s : test){
        //	System.out.print(s + ", ");
        //}
        //System.out.println();
        
        JSONObject partial = new JSONObject();
        partial.put("instances", instancesArray);
        partial.put("relations", relationsArray);
        partial.put("categories", categoriesArray);
        partial.put("values", valuesArray);
        result.put("DataModel", partial);
        //System.out.println(result.toString());
        return result;
    }
                
	public static String queryTypeDetermination(String tagsInput, String wordsInput){
	    if(wordsInput.toLowerCase().startsWith("how many") || wordsInput.toLowerCase().startsWith("how often")){
	        return "COUNT";
	    }else if(tagsInput.toLowerCase().matches("^vb. .*")){
	        return "ASK";
	    }else if(tagsInput.toLowerCase().contains("jjs") || tagsInput.toLowerCase().contains("rbs")){
	        return "MAX-MIN";
	    }else if(tagsInput.toLowerCase().contains("jjr")){
	        return "FILTER";
	    }else{
	        return "SELECT";
	    }
	}
                
                
    public static String answerTypeDetermination(List<String> listTag, List<String> listWords){
    	if(listWords.get(0).toLowerCase().contains("who")){
            return "AGENT";
        }else if(listWords.get(0).toLowerCase().contains("where")){
            return "PLACE";
        }else if (listWords.get(0).toLowerCase().contains("when")){
            return "DATE";
        }else if(listTag.get(0).toLowerCase().matches("vb.")){
            return "BOOL";
        }else if (listWords.get(0).toLowerCase().contains("how") && (listWords.get(1).toLowerCase().contains("many") || listWords.get(1).toLowerCase().contains("much") || listWords.get(1).toLowerCase().contains("often") || listWords.get(1).toLowerCase().contains("tall") || listWords.get(1).toLowerCase().contains("high") )){
            return "NUMBER";
        } else {
        	List<List<Integer>> pos = new ArrayList<>();
        	List <String> test = extractRegexWords(listTag, listWords,"(JJ    )*(NN.   )+",pos); //
        	for(int i=0; i < test.size() ; i++){
        		if ((pos.get(i).get(0)-1>=0 && listWords.get(pos.get(i).get(0)-1).toLowerCase().equals("which")) ||
        			(pos.get(i).get(0)-3>=0 && listWords.get(pos.get(i).get(0)-3).toLowerCase().equals("give") && listWords.get(pos.get(i).get(0)-2).toLowerCase().equals("me")) ||
        			(pos.get(i).get(0)-4>=0 && listWords.get(pos.get(i).get(0)-4).toLowerCase().equals("give") && listWords.get(pos.get(i).get(0)-3).toLowerCase().equals("me") && listWords.get(pos.get(i).get(0)-2).toLowerCase().equals("a"))&& listWords.get(pos.get(i).get(0)-1).toLowerCase().equals("list")){
        			String[] split=test.get(i).split(" ");
        			return split[split.length-1];
        		}
        	}
        	return "NONE"; 
        }
    }
                
    public static List<String> extractRegexWords(List<String> tagList, List<String> wordList, String inputRegex, List<List<Integer>> range ){
	    String tagString = "";
	    String wordString = "";
	    if (range== null)
	    	range = new ArrayList<>();
	    for(String s : tagList){                     
             if (s.length()==1){
            	 tagString += s + "     ";
             }
             if (s.length()==2){
            	 tagString += s + "    ";
             }
             if (s.length()==3){
            	 tagString += s + "   ";
             }
             if (s.length()==4){
            	 tagString += s + "  ";
             }
             if (s.length()==5){
            	 tagString += s + " ";
             }
         }
                 
        //System.out.println("");
        for(String s : wordList){
            wordString += s + " ";
        }
                            
        Pattern p = Pattern.compile(inputRegex);
        Matcher m = p.matcher(tagString);
        		
		List<String> matches = new ArrayList<>();
		//System.out.println(m.matches());
		while (m.find()){
			//System.out.print("match!!"+m.start());
			String match="";
			for (int i=m.start()/6; i<m.end()/6; i++){
				match+=wordList.get(i)+" ";
				
			}
			matches.add(match);
			List<Integer> pos = new ArrayList<>();
			pos.add(m.start()/6);
			pos.add(m.end()/6);
			range.add(pos);
		}
		return matches;
        }
}