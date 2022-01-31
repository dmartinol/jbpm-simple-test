package com.myspace.simple_project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.drools.core.time.impl.PseudoClockScheduler;
import org.jbpm.test.JbpmJUnitBaseTestCase;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.audit.NodeInstanceLog;
import org.kie.api.runtime.process.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestCases extends JbpmJUnitBaseTestCase {
  private static final Logger logger = LoggerFactory.getLogger(TestCases.class);

  public TestCases() {
    super(true, true);
  }

  @Test
  public void TestTimerExecuted() {
    //Enable the PseudoClock using the following system property.
    System.setProperty("drools.clockType", "pseudo");
    createRuntimeManager("com/myspace/simple_project/simple-process.bpmn");
    RuntimeEngine runtimeEngine = getRuntimeEngine();
    KieSession ksession = runtimeEngine.getKieSession();

    ProcessInstance processInstance = ksession.startProcess("simple-project.simple-process");
    Long processInstanceId = processInstance.getId();

    assertProcessInstanceActive(processInstanceId);
    assertNodeActive(processInstanceId, ksession, "Task");
    assertNodeActive(processInstanceId, ksession, "5 min");

    PseudoClockScheduler sessionClock = ksession.getSessionClock();
    // Timer is set to 5 min, so advancing with 10.
    sessionClock.advanceTime(10, TimeUnit.MINUTES);
    assertNodeTriggered(processInstanceId, "5 min");
    assertNodeActive(processInstanceId, ksession, "Task");
    List<NodeInstanceLog> timerExecutedList = getLogService().findNodeInstances(processInstanceId).stream().filter(n -> n.getType() == NodeInstanceLog.TYPE_EXIT && "5 min".equals(n.getNodeName()) ).collect(Collectors.toList());
    assertNotNull(timerExecutedList);
    assertEquals(1, timerExecutedList.size());
    logger.debug("FOUND {} timers executed with name as '5 min'", timerExecutedList.size());

    ksession.abortProcessInstance(processInstanceId);
    disposeRuntimeManager();
  }
}
