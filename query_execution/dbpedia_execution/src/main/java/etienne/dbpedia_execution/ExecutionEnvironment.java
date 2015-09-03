package etienne.dbpedia_execution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.*;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

public class ExecutionEnvironment {

	static Logger log = Logger.getLogger(ExecutionEnvironment.class.getName());

	String originalQuestion, queryString, answerType;
	Translator t;
	QueryType queryType;
	List<String> questionInstanceList;

	public ExecutionEnvironment(String originalQuestion, String queryString,
			QueryType queryType, String answerType,
			List<String> questionInstanceList) {
		this.originalQuestion = originalQuestion;
		this.queryString = queryString;
		this.queryType = queryType;
		this.answerType = answerType;
		this.questionInstanceList = questionInstanceList;

		t = new Translator();

		System.out.println("Input variables: ___________________");
		System.out.println("_Question: " + originalQuestion);
		System.out.println("_Query: " + queryString);
		System.out.println("_Query_Type: " + queryType.toString());
		System.out.println("_Answer_Type: " + answerType);
		System.out.println("____________________________________");
	}

	// special method if result_type is empty
	public JSONObject processEmptyType() 
	{
		JSONObject jsonObject = new JSONObject();

		// (1) Prepare Query
		Query query = QueryFactory.create(queryString);

		QueryExecution qexec = QueryExecutionFactory.sparqlService(
				"http://dbpedia.org/sparql", query);

		try {
			// Analyze results
			ResultSet results;

			results = qexec.execSelect();

			List<String> resultInstances = new ArrayList<String>();

			for (; results.hasNext();) {
				QuerySolution soln = results.nextSolution();

				String instance = soln.get("?X").toString();
			}

			jsonObject.put("Result", t.translate(originalQuestion, queryType,
					resultInstances, this.questionInstanceList));
			jsonObject.put("Instance_URI", resultInstances);

		} catch (Exception e) {

			e.printStackTrace();
		}

		finally {
			qexec.close();
		}
		
		return jsonObject;
	}

	public JSONObject processQuery() {

		JSONObject jsonObject = new JSONObject();

		// (1) Prepare Query
		Query query = QueryFactory.create(queryString);

		QueryExecution qexec = QueryExecutionFactory.sparqlService(
				"http://dbpedia.org/sparql", query);

		// Analyze results

		try {
			ResultSet results;

			switch (queryType) {
			case ASK:
				Boolean queryresult = qexec.execAsk();

				String resultNL = t.translate(originalQuestion, queryType,
						Arrays.asList(queryresult.toString()),
						this.questionInstanceList);

				jsonObject.put("Result", resultNL);
				jsonObject.put("Instances_URI", this.questionInstanceList);

				return jsonObject;
			case SELECT:
				results = qexec.execSelect();

				List<String> resultInstances = new ArrayList();
				List<String> conceptList = new ArrayList<String>();
				List<String> instanceLabels = new ArrayList<String>();

				Boolean resultsFound = false;

				// (1) check if the type of the resulting instances match the
				// answer_type
				for (; results.hasNext();) {

					QuerySolution soln = results.nextSolution();

					String instance = soln.get("?X").toString();

					String instanceLabel = instance.substring(instance
							.lastIndexOf("/") + 1);
					instanceLabel = instanceLabel.replace("_", " ");

					instanceLabels.add(instanceLabel);

					System.out.println("processing instance: " + instance);

					resultInstances.add(instance);

					String validationQuery = "";

					if (instance.contains("http")) {
						validationQuery = "PREFIX foaf: <http://xmlns.com/foaf/0.1/> "
								+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
								+ "ASK "
								+ "{ <"
								+ instance
								+ "> rdf:type "
								+ answerType + " . } ";
					} else {
						instance = instance.substring(0, instance.indexOf("@"));

						validationQuery = "PREFIX foaf: <http://xmlns.com/foaf/0.1/> "
								+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
								+ "ASK "
								+ "{ ?x <http://www.w3.org/2000/01/rdf-schema#label> \""
								+ instance
								+ "\"@en . ?x rdf:type "
								+ answerType + " }";
					}

					// -----------------------------------------------------------------------------------

					query = QueryFactory.create(validationQuery);

					qexec = QueryExecutionFactory.sparqlService(
							"http://dbpedia.org/sparql", validationQuery);

					Boolean validationResult = qexec.execAsk();

					if (validationResult) {
						System.out.println("Result found: < " + instance
								+ "> matches answerType: " + answerType);
						resultsFound = true;
					}
				}

				if (resultsFound) {
					jsonObject.put("Result", t.translate(originalQuestion,
							queryType, instanceLabels,
							this.questionInstanceList));
					jsonObject.put("Instance_URI", resultInstances);

					return jsonObject;
				}

				// (2) search for matching superclasses

				for (String instance : resultInstances) {
					String matchQuery = "PREFIX dbo: <http://dbpedia.org/ontology/> "
							+ "PREFIX res: <http://dbpedia.org/resource/> "
							+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
							+ "SELECT DISTINCT ?superClasses "
							+ "WHERE { "
							+ instance
							+ " rdf:type ?concept . "
							+ "?concept rdfs:subClassOf+ ?superClasses . "
							+ "} ";

					query = QueryFactory.create(matchQuery);

					qexec = QueryExecutionFactory.sparqlService(
							"http://dbpedia.org/sparql", query);

					List<String> superClasses = new ArrayList<String>();

					results = qexec.execSelect();

					for (; results.hasNext();) {
						QuerySolution soln = results.nextSolution();
						String superClass = soln.get("?superClasses")
								.toString();
						superClasses.add(superClass);
					}
					// match result_type with superclasses
					superClasses.contains(this.answerType);

					// (3) check for semantic similarity

					for (String concept : superClasses) {
						if (this.getSemanticSimilarity(concept, answerType) >= 0.7) {
							jsonObject
									.put("Result", t.translate(
											originalQuestion, queryType,
											resultInstances,
											this.questionInstanceList));
							jsonObject.put("Instance_URI", resultInstances);
						}

					}
				}
				break;
			case COUNT:

				results = qexec.execSelect();

				for (; results.hasNext();) {
					QuerySolution soln = results.nextSolution();

					String result = soln.get("?count").toString();

					result = result.substring(0, result.indexOf("^"));

					jsonObject.put("Result", t.translate(originalQuestion,
							queryType, Arrays.asList(result),
							this.questionInstanceList));
					jsonObject.put("Instance_URI", this.questionInstanceList);
				}
				break;

			}

			// System.out.println(soln.get("?name"));
		} catch (Exception e) {

			e.printStackTrace();
		}

		finally {
			qexec.close();
		}
		return jsonObject;
	}

	public double getSemanticSimilarity(String concept, String compareTo) {
		double similarity = -1;

		try {
			// construct URL & open connection
			String urlString = "http://vmdgsit04.deri.ie/relatedness/lsa/en?term="
					+ concept + "&targetSet=" + compareTo;
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();

			// read & parse JSON object
			BufferedReader br = new BufferedReader(new InputStreamReader(
					urlc.getInputStream()));

			String l = null;
			while ((l = br.readLine()) != null) {
				JSONObject obj = new JSONObject(l);
				similarity = obj.getDouble(compareTo);
			}
			br.close();

		} catch (IOException e) {
			System.out.println(e.getMessage());
		}

		return similarity;
	}
}