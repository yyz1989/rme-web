package controllers;

import org.rugco.crc.OntologyControlApi;
import org.rugco.crc.RuleControlApi;
import play.*;
import play.data.*;
import play.mvc.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Yang added:
 * User: yuanzhe
 * Date: 13-12-4
 * Time: 下午1:07
 * To change this template use File | Settings | File Templates.
 */
public class ApplOntology extends Controller{
    private static ApplOntology ourInstance = new ApplOntology();

    public static ApplOntology getInstance() {
        return ourInstance;
    }

    private ApplOntology() {
    }

    public static Result dlQuery() {
        DynamicForm requestData = Form.form().bindFromRequest();
        Map<String,String> map=requestData.data();
        String query=map.get("query");
        boolean spcls=map.containsValue("supercls");
        boolean eqcls=map.containsValue("equivcls");
        boolean sub=map.containsValue("subcls");
        boolean ins=map.containsValue("instance");
        String result=OntologyControlApi.dlQuery(query,spcls,eqcls,sub,ins);
        ArrayList<String> list=new ArrayList<String>();
        ArrayList<String> rules=new ArrayList<String>();
        String state="";
        return showOntology(result,list,state,rules);
    }

    public static Result showOntology(String queryAnswer, ArrayList<String> list, String state, ArrayList<String> rules) {
        String classes="";
        String properties="";
        String individuals="";

        try{
            classes=OntologyControlApi.getClasses();
            properties=OntologyControlApi.getProperties();
            individuals=OntologyControlApi.getIndividuals();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ok(views.html.ontology.render(classes,properties,individuals,queryAnswer,list,state,rules));
    }

    public static Result showOntology(String info) {
        ArrayList<String> list=new ArrayList<String>();
        ArrayList<String> rules=new ArrayList<String>();
        String state="";
        return showOntology(info,list,state,rules);
    }

    public static Result showOntology() {
        return showOntology("");
    }

    public static Result parseRules() {
        DynamicForm requestData = Form.form().bindFromRequest();
        String rule=requestData.get("rule");
        String result=OntologyControlApi.batchRuleGeneration(rule).trim();
        ArrayList<String> list=new ArrayList<String>();
        ArrayList<String> rules=new ArrayList<String>();
        if (result.contains("Encountered")||(!result.contains("$"))) return showOntology(result,list,"",rules);
        StringTokenizer tokenizer=new StringTokenizer(result,"$");
        while(tokenizer.hasMoreTokens()){
            rules.add(tokenizer.nextToken());
        }
        if(rules.size()>0) return showOntology("The list of rules matching description \""+rule+"\" is in the table below",list,"",rules);
        else return showOntology(result,list,"",rules);
    }

    public static Result insertRules() {
        DynamicForm requestData = Form.form().bindFromRequest();
        Map<String, String> map=requestData.data();
        ArrayList<String> rules=new ArrayList<String>();
        for(String key:map.keySet()) {
            if(key.matches("selected\\[\\d+\\]")) rules.add(map.get(key));
        }
        for(String rule:rules) {
            //System.out.println(map.get(rule));
            RuleControlApi.createRuleUnparsed(map.get(rule));
        }
        return redirect(routes.ApplRule.showRules());
    }

    public static Result parseStateChangeCommands() {
        DynamicForm requestData = Form.form().bindFromRequest();
        String variable=requestData.get("variable");
        String state=requestData.get("state");
        String result=OntologyControlApi.getTrimIndividuals(variable);
        ArrayList<String> list=new ArrayList<String>();
        ArrayList<String> rules=new ArrayList<String>();
        if (result.contains("Encountered")) return showOntology(result,list,state,rules);
        StringTokenizer tokenizer=new StringTokenizer(result,"|");
        while(tokenizer.hasMoreTokens()){
            String var=tokenizer.nextToken();
            StringTokenizer tokenizerDot=new StringTokenizer(var,".");
            if(tokenizerDot.countTokens()==2) {
                list.add(var);
            }
        }
        if(list.size()>0) return showOntology("The list of variables which matches the description:\n\n"+result,list,state,rules);
        else return showOntology("No variables are found given the query: "+variable,list,state,rules);
    }

    public static Result executeStateChangeCommands() {
        DynamicForm requestData = Form.form().bindFromRequest();
        String event="update";
        Map<String, String> map=requestData.data();
        ArrayList<String> variables=new ArrayList<String>();
        for(String key:map.keySet()) {
            if(key.matches("selected\\[\\d+\\]")) variables.add(key);
        }
        for(String key:variables) {
            String variable=map.get(key);
            String state=map.get(variable);
            StringTokenizer tokenizer=new StringTokenizer(variable,".");
            RuleControlApi.updateVariable(event,tokenizer.nextToken(),tokenizer.nextToken(),state);
        }
        return redirect(routes.ApplEnvironment.showEnvironment());
    }

    public static Result saveOntology() {
        String result=OntologyControlApi.saveOntology();
        if(result.equals("true")) return showOntology("Ontology saved to file successfully");
        else return showOntology("Unable to save ontology, reason: "+result);
    }

    public static Result rebuildOntology() {
        String result=OntologyControlApi.rebuildOntology();
        if(result.equals("true")) return showOntology("Ontology reinitialized due to user request");
        else return showOntology("Unable to reinitialize ontology, reason: "+result);
    }
}
