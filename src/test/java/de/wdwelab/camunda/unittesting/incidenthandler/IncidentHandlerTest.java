package de.wdwelab.camunda.unittesting.incidenthandler;

import org.camunda.bpm.engine.externaltask.LockedExternalTask;
import org.camunda.bpm.engine.impl.incident.IncidentContext;
import org.camunda.bpm.engine.impl.incident.IncidentHandler;
import org.camunda.bpm.engine.runtime.Incident;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.junit.*;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Deployment(resources = {"incidenthandler/IncidentHandlerTest.bpmn"})
public class IncidentHandlerTest {

  @ClassRule
  public static ProcessEngineRule rule = new ProcessEngineRule("incidenthandler/camunda.cfg.xml");

  public static IncidentHandler incidentHandler;

  @Test
  public void deploy() {}

  @Test
  public void testRetry() {

    // A new process instance is waiting at an external task and has no incidents

    ProcessInstance pi = startProcessInstance();
    assertThat(pi).isWaitingAt("ExternalTask");

    Incident incident = getIncident();
    assertThat(incident).isNull();

    LockedExternalTask externalTask = getLockedExternalTask();
    assertThat(externalTask).isNotNull();

    // A temporary (retryable) failure does not lead to an incident

    handleTemporaryFailure(externalTask);

    incident = getIncident();
    assertThat(incident).isNull();

    // A permanent (non retryable) failure does generate an incident

    handlePermanentFailure(externalTask);

    incident = getIncident();
    assertThat(incident).isNotNull();

    // Setting the failure temporary (retryable) resolves the incident again ...

    handleTemporaryFailure(externalTask);

    incident = getIncident();
    assertThat(incident).isNull();

    // ... and we can verify that the incident handler was invoked

    verify(incidentHandler, times(1)).handleIncident(any(IncidentContext.class), anyString());
    verify(incidentHandler, times(1)).resolveIncident(any(IncidentContext.class));

  }

  @Test // Currently fails due to https://app.camunda.com/jira/browse/CAM-7134
  public void testComplete() {

    // A new process instance is waiting at an external task and has no incidents

    ProcessInstance pi = startProcessInstance();
    assertThat(pi).isWaitingAt("ExternalTask");

    Incident incident = getIncident();
    assertThat(incident).isNull();

    LockedExternalTask externalTask = getLockedExternalTask();
    assertThat(externalTask).isNotNull();

    // A permanent (non retryable) failure does generate an incident

    handlePermanentFailure(externalTask);

    incident = getIncident();
    assertThat(incident).isNotNull();

    // Completing the external task resolves/removes the incident ...

    completeExternalTask(externalTask);

    incident = getIncident();
    assertThat(incident).isNull();

    // ... but the incident handler was not invoked (neither resolveIncident nor deleteIncident)

    verify(incidentHandler, times(1)).handleIncident(any(IncidentContext.class), anyString());
    // This fails, however it should'nt?
    verify(incidentHandler, times(1)).resolveIncident(any(IncidentContext.class));

  }

  @Before
  public void logger() {
    LogManager.getLogManager().reset();
    SLF4JBridgeHandler.install();
  }

  @Before
  public void resetSpy() {
    reset(incidentHandler);
  }

  @BeforeClass
  public static void initSpy() {
    incidentHandler = spy(rule.getProcessEngineConfiguration().getIncidentHandler("failedExternalTask"));
    Map<String, IncidentHandler> handlers = new HashMap<String, IncidentHandler>();
    handlers.put("failedExternalTask", incidentHandler);
    rule.getProcessEngineConfiguration().setIncidentHandlers(handlers);
  }

  private ProcessInstance startProcessInstance() {
    return rule.getRuntimeService().startProcessInstanceByKey("IncidentHandlerTest");
  }

  private Incident getIncident() {
    return rule.getRuntimeService().createIncidentQuery().singleResult();
  }

  private static final String WORKER_ID = "workerId";
  private static final String TOPIC_ID = "topicId";

  private LockedExternalTask getLockedExternalTask() {
    List<LockedExternalTask> result = rule.getExternalTaskService()
        .fetchAndLock(1, WORKER_ID)
        .topic(TOPIC_ID, Long.MAX_VALUE).execute();
    return result.size() > 0 ? result.get(0): null;
  }

  private void completeExternalTask(LockedExternalTask externalTask) {
    rule.getExternalTaskService().complete(externalTask.getId(), WORKER_ID);
  }

  private void handleTemporaryFailure(LockedExternalTask externalTask) {
    handleFailure(externalTask, false);
  }

  private void handlePermanentFailure(LockedExternalTask externalTask) {
    handleFailure(externalTask, true);
  }

  private void handleFailure(LockedExternalTask externalTask, boolean permanent) {
    rule.getExternalTaskService().handleFailure(externalTask.getId(), WORKER_ID, externalTask.getErrorMessage(), permanent ? 0 : 1, Long.MAX_VALUE);
  }

}
