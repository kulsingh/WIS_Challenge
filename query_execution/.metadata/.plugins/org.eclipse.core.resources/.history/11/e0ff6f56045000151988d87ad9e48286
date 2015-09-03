package etienne.dbpedia_execution;

public class Starter {
	
    public static void main( String[] args )
    {
    	
    	String queryString = "PREFIX foaf:  <http://xmlns.com/foaf/0.1/> "
    		+ "SELECT ?name "
    	    + "WHERE { "
    		+    "?person foaf:name ?name . "
    		+ "} limit 10";

    	
    	DBPediaExecution ex = new DBPediaExecution();
    	
    	ex.executeQuery(queryString);
    
    }

}
