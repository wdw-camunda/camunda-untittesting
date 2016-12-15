package de.wdwelab.camunda.unittesting.eventSubProcess;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineAssertions.assertThat;

import java.util.List;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceModificationBuilder;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author Alexander Glück <alexander.glueck@wdw-elab.de>
 */
@Deployment(resources = { "eventSubProcess/processWithEventSupProcess.bpmn" })
public class EventSubProcessWithNonInterruptionEscalationStartEvent {

    private static final String PROCESS_DEFINITION = "ProcessWithEventSubProcess";
    private static final String BUSINESS_KEY = "businessKey";
    private static final String START_EVENT = "StartEvent";
    private static final String USER_TASK_MAIN_PROCESS = "UserTaskMainProcess";
    private static final String ESCALATION_END_EVENT = "EscalationEndEvent";
    private static final String NON_INTERRUPTING_ESCALATION_START_EVENT = "NonInterruptingEscalationStartEvent";
    private static final String USER_TASK_EVENT_SUB_PROCESS = "UserTaskEventSubProcess";
    private static final String EVENT_SUB_PROCESS_END_EVENT = "EventSubProcessEndEvent";
    private static final String EVENT_SUB_PROCESS = "EventSubProcess";

    @ClassRule
    public static ProcessEngineRule rule = new ProcessEngineRule("eventSubProcess/camunda.cfg.xml");

    @Test
    public void deploy() {
    }

    @Test
    public void happyPath() {
        ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceByKey(PROCESS_DEFINITION,
                BUSINESS_KEY);

        assertThat(processInstance).isWaitingAt(USER_TASK_MAIN_PROCESS);

        completeUserTaskWithName(processInstance.getId(), USER_TASK_MAIN_PROCESS);

        assertThat(processInstance).isWaitingAt(USER_TASK_EVENT_SUB_PROCESS);

        completeUserTaskWithName(processInstance.getId(), USER_TASK_EVENT_SUB_PROCESS);

        assertThat(processInstance).isEnded().hasPassed(USER_TASK_MAIN_PROCESS, USER_TASK_EVENT_SUB_PROCESS);

    }

    @Test
    public void processInstanceModificationInEventSubProcess() {
        ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceByKey(PROCESS_DEFINITION,
                BUSINESS_KEY);

        assertThat(processInstance).isWaitingAt(USER_TASK_MAIN_PROCESS);

        completeUserTaskWithName(processInstance.getId(), USER_TASK_MAIN_PROCESS);

        assertThat(processInstance).isWaitingAt(USER_TASK_EVENT_SUB_PROCESS);

        List<String> activeActivityIds = rule.getRuntimeService().getActiveActivityIds(processInstance.getId());

        assert (activeActivityIds.contains(USER_TASK_EVENT_SUB_PROCESS));

        ProcessInstanceModificationBuilder processInstanceModification = rule.getRuntimeService().createProcessInstanceModification(
                processInstance.getId());
        processInstanceModification.cancelAllForActivity(USER_TASK_EVENT_SUB_PROCESS);
        processInstanceModification.startAfterActivity(USER_TASK_EVENT_SUB_PROCESS);
        processInstanceModification.execute();

        assertThat(processInstance).hasPassedInOrder(START_EVENT,
                USER_TASK_MAIN_PROCESS,
                ESCALATION_END_EVENT,
                NON_INTERRUPTING_ESCALATION_START_EVENT,
                USER_TASK_EVENT_SUB_PROCESS,
                EVENT_SUB_PROCESS_END_EVENT);

        assertThat(processInstance).hasPassed(EVENT_SUB_PROCESS);

        assertThat(processInstance).isEnded();

    }

    private void completeUserTaskWithName(String processInstanceId, String taskDefinitionKey) {
        Task task = rule.getTaskService().createTaskQuery().processInstanceId(processInstanceId).taskDefinitionKey(
                taskDefinitionKey).singleResult();

        rule.getTaskService().complete(task.getId());

    }

}
