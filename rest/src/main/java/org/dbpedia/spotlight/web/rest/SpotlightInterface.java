/*
 * Copyright 2011 DBpedia Spotlight Development Team
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Check our project website for information on how to acknowledge the authors and how to contribute to the project: http://spotlight.dbpedia.org
 */

package org.dbpedia.spotlight.web.rest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.disambiguate.ParagraphDisambiguatorJ;
import org.dbpedia.spotlight.exceptions.InputException;
import org.dbpedia.spotlight.exceptions.SearchException;
import org.dbpedia.spotlight.exceptions.SpottingException;
import org.dbpedia.spotlight.filter.annotations.CombineAllAnnotationFilters;
import org.dbpedia.spotlight.model.*;
import org.dbpedia.spotlight.spot.Spotter;
import org.dbpedia.spotlight.model.SpotlightConfiguration.DisambiguationPolicy;
import org.dbpedia.spotlight.model.SpotterConfiguration.SpotterPolicy;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller that interfaces between the REST API and the DBpedia Spotlight core.
 * Needs to be constructed from SpotlightInterface.getInstance(Annotator) or SpotlightInterface.getInstance(Disambiguator).
 *
 * @author maxjakob, pablomendes
 */
public class SpotlightInterface  {

    Log LOG = LogFactory.getLog(this.getClass());

    // Name of the REST api so that we can announce it in the log (can be disambiguate, annotate, candidates)
    String apiName;

    private OutputManager outputManager = new OutputManager();

    public SpotlightInterface(String apiName) {
        this.apiName = apiName;
    }

    public List<DBpediaResourceOccurrence> disambiguate(List<SurfaceFormOccurrence> spots, ParagraphDisambiguatorJ disambiguator) throws SearchException, InputException, SpottingException {
        List<DBpediaResourceOccurrence> resources = new ArrayList<DBpediaResourceOccurrence>();
        if (spots.size()==0) return resources; // nothing to disambiguate
        try {
            resources = disambiguator.disambiguate(Factory.paragraph().fromJ(spots));
        } catch (UnsupportedOperationException e) {
            throw new SearchException(e);
        }
        return resources;
    }

    public boolean policyIsBlacklist(String policy) {
        boolean blacklist = false;
        if(policy.trim().equalsIgnoreCase("blacklist")) {
            blacklist = true;
            policy = "blacklist";
        }
        else {
            policy = "whitelist";
        }
        return blacklist;
    }

    public void announce(String textString,
                         double confidence,
                         int support,
                         String ontologyTypesString,
                         String sparqlQuery,
                         String policy,
                         boolean coreferenceResolution,
                         String clientIp,
                         String spotterName,
                         String disambiguatorName
    ) {
        LOG.info("******************************** Parameters ********************************");
        LOG.info("API: " + getApiName());
        LOG.info("client ip: " + clientIp);
        LOG.info("text: " + textString);
        LOG.info("text length in chars: " +textString.length());
        LOG.info("confidence: "+String.valueOf(confidence));
        LOG.info("support: "+String.valueOf(support));
        LOG.info("types: "+ontologyTypesString);
        LOG.info("sparqlQuery: "+ sparqlQuery);
        LOG.info("policy: "+policyIsBlacklist(policy));
        LOG.info("coreferenceResolution: "+String.valueOf(coreferenceResolution));
        LOG.info("spotter: "+spotterName);
        LOG.info("disambiguator: "+disambiguatorName);

    }

    public List<SurfaceFormOccurrence> spot(String spotterName, Text context) throws InputException, SpottingException {
        Spotter spotter = Server.getSpotter(spotterName);
        List<SurfaceFormOccurrence> spots = spotter.extract(context);
        return spots;
    }

    /**
     * Retrieves representation of an instance of org.dbpedia.spotlight.web.Annotation
     * @return an instance of java.lang.String
     */
    public List<DBpediaResourceOccurrence> getOccurrences(String textString,
                                                          double confidence,
                                                          int support,
                                                          String ontologyTypesString,
                                                          String sparqlQuery,
                                                          String policy,
                                                          boolean coreferenceResolution,
                                                          String clientIp,
                                                          String spotterName,
                                                          String disambiguatorName
                                                          ) throws SearchException, InputException, SpottingException {

        boolean blacklist = policyIsBlacklist(policy);

        announce(textString,confidence,support,ontologyTypesString,sparqlQuery,policy,coreferenceResolution,clientIp,spotterName,disambiguatorName);

        // Get input text
        if (textString.trim().equals("")) {
            throw new InputException("No text was specified in the &text parameter.");
        }
        Text context = new Text(textString);

        // Find spots to annotate/disambiguate
        List<SurfaceFormOccurrence> spots = spot(spotterName,context);

        // Call annotation or disambiguation
        int maxLengthForOccurrenceCentric = 1200; //TODO configuration
        if (disambiguatorName==SpotlightConfiguration.DisambiguationPolicy.Default.name()
                && textString.length() > maxLengthForOccurrenceCentric) {
            disambiguatorName = SpotlightConfiguration.DisambiguationPolicy.Document.name();
            LOG.info(String.format("Text length > %d. Using %s to disambiguate.",maxLengthForOccurrenceCentric,disambiguatorName));
        }
        ParagraphDisambiguatorJ disambiguator = Server.getDisambiguator(disambiguatorName);
        List<DBpediaResourceOccurrence> occList = disambiguate(spots, disambiguator);

        // Linking / filtering
        List<OntologyType> ontologyTypes = new ArrayList<OntologyType>();
        String types[] = ontologyTypesString.trim().split(",");
        for (String t : types){
            if (!t.trim().equals("")) ontologyTypes.add(Factory.ontologyType().fromQName(t.trim()));
        }

        // Filter: Old monolithic way
        CombineAllAnnotationFilters annotationFilter = new CombineAllAnnotationFilters(Server.getConfiguration());
        occList = annotationFilter.filter(occList, confidence, support, ontologyTypes, sparqlQuery, blacklist, coreferenceResolution);

        // Filter: TODO run occurrences through a list of annotation filters (which can be passed by parameter)
        // Map<String,AnnotationFilter> annotationFilters = buildFilters(occList, confidence, support, dbpediaTypes, sparqlQuery, blacklist, coreferenceResolution);
        //AnnotationFilter annotationFilter = annotationFilters.get(CombineAllAnnotationFilters.class.getSimpleName());

        if (LOG.isDebugEnabled()) {
            LOG.debug("Shown:");
            for(DBpediaResourceOccurrence occ : occList) {
                LOG.debug(String.format("%s <- %s; score: %s, ctxscore: %3.2f, support: %s, prior: %s", occ.resource(), occ.surfaceForm(), occ.similarityScore(), occ.contextualScore(), occ.resource().support(), occ.resource().prior()));
            }
        }

        return occList;
    }

    public String getHTML(String text,
                          double confidence,
                          int support,
                          String dbpediaTypesString,
                          String sparqlQuery,
                          String policy,
                          boolean coreferenceResolution,
                          String clientIp,
                          String spotter,
                          String disambiguator
    ) throws Exception {
        String result;
        try {
            List<DBpediaResourceOccurrence> occs = getOccurrences(text, confidence, support, dbpediaTypesString, sparqlQuery, policy, coreferenceResolution, clientIp, spotter, disambiguator);
            result = outputManager.makeHTML(text, occs);
        }
        catch (InputException e) { //TODO throw exception up to Annotate for WebApplicationException to handle.
            LOG.info("ERROR: "+e.getMessage());
            result = "<html><body><b>ERROR:</b> <i>"+e.getMessage()+"</i></body></html>";
        }
        LOG.info("HTML format");
        LOG.debug("****************************************************************");
        return result;
    }

    public String getRDFa(String text,
                          double confidence,
                          int support,
                          String dbpediaTypesString,
                          String sparqlQuery,
                          String policy,
                          boolean coreferenceResolution,
                          String clientIp,
                          String spotter,
                          String disambiguator
    ) throws Exception {
        String result;
        try {
            List<DBpediaResourceOccurrence> occs = getOccurrences(text, confidence, support, dbpediaTypesString, sparqlQuery, policy, coreferenceResolution, clientIp, spotter, disambiguator);
            result = outputManager.makeRDFa(text, occs);
        }
        catch (InputException e) { //TODO throw exception up to Annotate for WebApplicationException to handle.
            LOG.info("ERROR: "+e.getMessage());
            result = "<html><body><b>ERROR:</b> <i>"+e.getMessage()+"</i></body></html>";
        }
        LOG.info("RDFa format");
        LOG.debug("****************************************************************");
        return result;
    }

    public String getXML(String text,
                         double confidence,
                         int support,
                         String dbpediaTypesString,
                         String sparqlQuery,
                         String policy,
                         boolean coreferenceResolution,
                         String clientIp,
                         String spotter,
                         String disambiguator
   ) throws Exception {
        String result;
//        try {
            List<DBpediaResourceOccurrence> occs = getOccurrences(text, confidence, support, dbpediaTypesString, sparqlQuery, policy, coreferenceResolution, clientIp, spotter,disambiguator);
            result = outputManager.makeXML(text, occs, confidence, support, dbpediaTypesString, sparqlQuery, policy, coreferenceResolution);
//        }
//        catch (Exception e) { //TODO throw exception up to Annotate for WebApplicationException to handle.
//            LOG.info("ERROR: "+e.getMessage());
//            result = outputManager.makeErrorXML(e.getMessage(), text, confidence, support, dbpediaTypesString, spqarlQuery, policy, coreferenceResolution);
//        }
        LOG.info("XML format");
        LOG.debug("****************************************************************");

        return result;
    }

    //FIXME
    public String getCandidateXML(String text,
                         double confidence,
                         int support,
                         String dbpediaTypesString,
                         String sparqlQuery,
                         String policy,
                         boolean coreferenceResolution,
                         String clientIp,
                         String spotter,
                         String disambiguator
   ) throws Exception {
        String result;
        List<DBpediaResourceOccurrence> occs = getOccurrences(text, confidence, support, dbpediaTypesString, sparqlQuery, policy, coreferenceResolution, clientIp, spotter,disambiguator);
        result = outputManager.makeXML(text, occs, confidence, support, dbpediaTypesString, sparqlQuery, policy, coreferenceResolution);
        LOG.info("XML format");
        LOG.debug("****************************************************************");
        return result;
    }

    public String getJSON(String text,
                          double confidence,
                          int support,
                          String dbpediaTypesString,
                          String sparqlQuery,
                          String policy,
                          boolean coreferenceResolution,
                          String clientIp,
                          String spotterName,
                          String disambiguator
    ) throws Exception {
        String result;
        String xml = getXML(text, confidence, support, dbpediaTypesString, sparqlQuery, policy, coreferenceResolution, clientIp, spotterName,disambiguator);
        result = outputManager.xml2json(xml);
        LOG.info("JSON format");
        LOG.debug("****************************************************************");

        return result;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

}