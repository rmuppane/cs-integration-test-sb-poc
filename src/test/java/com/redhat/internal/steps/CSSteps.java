package com.redhat.internal.steps;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.collections.CollectionUtils;
import org.jbpm.services.api.DeploymentService;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.model.ProcessInstanceDesc;
import org.kie.internal.query.QueryContext;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.services.api.KieServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.redhat.Document;
import com.redhat.GSWrapper;
import com.redhat.internal.cases.CSSharedState;
import com.redhat.internal.util.SharedAssets;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.DataTableType;
import io.cucumber.java.DefaultDataTableCellTransformer;
import io.cucumber.java.DefaultDataTableEntryTransformer;
import io.cucumber.java.DefaultParameterTransformer;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

public class CSSteps {
	
	@Autowired
    private ProcessService processService;
	
	@Autowired
    private RuntimeDataService runtimeDataService;
	
	@Autowired
    private DeploymentService deploymentService;
	
    @Autowired
    private KieServer kieServer;

    private static final Logger LOGGER = LoggerFactory.getLogger(CSSteps.class);

    private static final String CONTAINER_ID = "POC_test-dmn";

    RetryPolicy<Object> retryPolicy = new RetryPolicy<>() //
        .handle(Exception.class) //
        .withDelay(Duration.ofSeconds(1)) // 
        .withMaxRetries(1);

    @Autowired
    private CSSharedState csSharedState;
    
    public final ObjectMapper objectMapper = 
 		   new ObjectMapper().registerModule(new JSR310Module());
    
    @DataTableType
    public Document documentEntry(Map<String, String> entry) {
        String str = entry.get("footNoteCodes");
        List<String> stringList = Arrays.asList(str.toString().split(","));
    	List<BigDecimal> bigDecimalList = new LinkedList<BigDecimal>();
    	for (String value : stringList) {
    	    bigDecimalList.add(new BigDecimal(value));
    	}
    	return new Document(entry.get("docName"),
        		bigDecimalList,
        		entry.get("documentENname"), entry.get("documentDEName"),
        		entry.get("documentITName"), entry.get("documentFRName"),
        		"",
        		new Boolean(entry.get("documentInSourceAndCSLangRequired")) );
    }
    
    @Before
    public void beforTest(Scenario scenario) {
        LOGGER.info("######################### INIT SCENARIO {} #########################", scenario.getName());
        KieContainerResource kieContainer = new KieContainerResource(new ReleaseId("com.redhat", "test-dmn", "1.0.1-SNAPSHOT"));
        kieServer.createContainer(CONTAINER_ID, kieContainer);
    }

    @After
    public void afterTest(Scenario scenario) {
    	List<Integer> states = new ArrayList<Integer>();
    	states.add(0);
    	Collection<ProcessInstanceDesc> activeProcesses = runtimeDataService.getProcessInstancesByDeploymentId(CONTAINER_ID, states, new QueryContext());
    	activeProcesses.stream().forEach(pi -> processService.abortProcessInstance(CONTAINER_ID, pi.getId()));
    	deploymentService.deactivate(CONTAINER_ID);
    	deploymentService.undeploy(deploymentService.getDeployedUnit(CONTAINER_ID).getDeploymentUnit());
        LOGGER.info("######################### END SCENARIO {} #########################", scenario.getName());
    }


    @When("^a request to check for '(.*?)' the generated documents")
    public void startProcessInstance(String processDefinitionId, DataTable table) throws Throwable {
        LOGGER.info("a process instance for definition id '{}' is started$", processDefinitionId);
        final AtomicReference<Long> processId = new AtomicReference<>();
        final Map<String, Object> rows1 = table.asMap(String.class, Object.class);
        final Map<String, Object> rows = translate(rows1);
        Failsafe.with(retryPolicy).run(() -> processId.set(processService.startProcess(CONTAINER_ID, processDefinitionId, rows)));
        csSharedState.setProcessId(processId.get());
    }
    
    private Map<String, Object> translate(Map<String, Object> rows) {
    	Map<String, Object> transRows = new HashMap<String, Object>();
    	GSWrapper wrapper = new GSWrapper();
    	wrapper.setState((String)rows.get("state"));
    	wrapper.setZone((String)rows.get("zone"));
    	wrapper.setDateOfIncorporationMonths(Integer.parseInt((String)rows.get("dateOfIncorporationMonths")));
    	// wrapper.setDateOfIncorporation(LocalDate.parse((String)rows.get("dateOfIncorporation"))); 
    	//TODO : How to pass the date  
    	wrapper.setCompanyTypeEnName((String)rows.get("companyTypeEnName"));
    	wrapper.setCountryCode(Integer.parseInt((String)rows.get("countryCode")));
    	transRows.put("InputPayload", wrapper);
		return transRows;
	}
    
    @Then("^List of documents are")
    public void validate(List<Document> documents) throws Throwable {
        LOGGER.info("validate the documents with the value is {}", documents);
        List<Document> docs = (List<Document>)SharedAssets.sharedMap.get("DecisionOutput");
        System.out.println("Output decision is [" + docs +"]");
        assertNotNull(docs);
        if(CollectionUtils.subtract(docs, documents) .size() == 0 ) {
        	 assertTrue(true);
        }
        //Failsafe.with(retryPolicy).run(() -> processId.set(processServicesClient.startProcess(CONTAINER_ID, processDefinitionId, rows)));
    }
}
