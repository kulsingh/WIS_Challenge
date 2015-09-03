package etienne.dbpedia_execution;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;



public class DBPediaExecution {

	public void executeQuery(String queryString) {
		Query query = QueryFactory.create(queryString);

		QueryExecution qexec = QueryExecutionFactory.sparqlService(
				"http://dbpedia.org/sparql", query);

		try {
			ResultSet results = qexec.execSelect();
			for (; results.hasNext();) {
				QuerySolution soln = results.nextSolution();
				System.out.println(soln.get("?name"));
			}
		} catch (Exception e) {

			e.printStackTrace();
		}

		finally {
			qexec.close();
		}
	}
}
