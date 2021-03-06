package aaf.base.workflow

import grails.test.spock.*
import grails.test.mixin.TestMixin
import org.apache.shiro.subject.Subject

import aaf.base.identity.Subject
import aaf.base.identity.Role

class WorkflowProcessServiceSpec extends IntegrationSpec {
  static transactional = true
  
  def subject
  def workflowProcessService
  def minimalDefinition
  def savedMetaClasses
  def factoryHolder
  
  def setup() {
    savedMetaClasses = [:]
    
    def role = new Role(name:'allsubjects')
    subject = new Subject(principal:'http://idp.test.com/entity!http://sp.test.com/entity!1234', cn:'test subject', email:'testsubject@test.com', sharedToken:'1234sharedtoken').save()
    role.addToSubjects(subject)
    role.save()

    SpecHelpers.setupShiroEnv(subject)
  }

  def "Create minimal process"() {
    setup:
    def testScript = new WorkflowScript(name:'TestScript', description:'A script used in testing', definition:'return true', creator:subject)
    testScript.save()

    minimalDefinition = new File('test/data/minimal.pr').getText()

    expect:
    !testScript.hasErrors()
    
    when:
    def(created, process_) = workflowProcessService.create(minimalDefinition)
    
    then:
    created
    
    def process = Process.findByName('Minimal Test Process')
    process == process_
    
    process.tasks.get(0).outcomes.get('testoutcome1').start.contains('task2')
    process.tasks.get(0).needsApproval() == true
    process.tasks.get(0).approverRoles.size() == 1
    process.tasks.get(0).approverRoles.contains('{TEST_VAR}')
    process.tasks.get(0).rejections.get('rejection1') != null
    process.tasks.get(0).rejections.get('rejection1').start.size() == 1
    process.tasks.get(0).rejections.get('rejection1').terminate.size() == 0
    process.tasks.get(0).rejections.get('rejection1').start.contains('task6')
    
    process.tasks.get(1).needsApproval() == true
    process.tasks.get(1).approverRoles.size() == 3
    process.tasks.get(1).approverRoles.contains('{TEST_VAR2}')
    process.tasks.get(1).approverRoles.contains('{TEST_VAR3}')
    process.tasks.get(1).approverRoles.contains('TEST_ROLE')
    
    process.tasks.get(3).needsApproval() == false
    process.tasks.get(3).approverRoles.size() == 0
    
    process.tasks.get(4).needsApproval() == false
    process.tasks.get(4).approverRoles.size() == 0
    
    process.creator == workflowProcessService.subject
  }
  
  def "Initiate minimal process"() {
    setup:
    def testScript = new WorkflowScript(name:'TestScript', description:'A script used in testing', definition:'return true', creator:workflowProcessService.subject).save()
    minimalDefinition = new File('test/data/minimal.pr').getText()
    workflowProcessService.create(minimalDefinition)
    def process = Process.findByName('Minimal Test Process')
    def agent = Subject.build(cn: 'Agent CN', email:'user@test.com')

    expect:
    !testScript.hasErrors()
    
    when:   
    def(created, processInstance) = workflowProcessService.initiate(process.name, "Approving XYZ Widget", ProcessPriority.LOW, ['agent': agent, 'TEST_VAR':'VALUE_1', 'TEST_VAR2':'VALUE_2', 'TEST_VAR3':'VALUE_3'])
    
    then:
    created
    !processInstance.hasErrors()
    processInstance.id > 0
    processInstance.process == process
    processInstance.description == "Approving XYZ Widget"

    processInstance.params.get('TEST_VAR').equals('VALUE_1')
    processInstance.params.get('TEST_VAR2').equals('VALUE_2')
    processInstance.params.get('TEST_VAR3').equals('VALUE_3')
    processInstance.params.get('NOSUCH_VAR') == null
  }

  def "Initiate minimal process as public source"() {
    setup:
    def testScript = new WorkflowScript(name:'TestScript', description:'A script used in testing', definition:'return true', creator:workflowProcessService.subject).save()
    minimalDefinition = new File('test/data/minimal.pr').getText()
    workflowProcessService.create(minimalDefinition)
    def process = Process.findByName('Minimal Test Process')

    expect:
    !testScript.hasErrors()
    
    when:   
    def(created, processInstance) = workflowProcessService.initiate(process.name, "Approving XYZ Widget", ProcessPriority.LOW, ['agentCN':'Jim Public', 'agentEmail':'jimpublic@test.edu.au', 'TEST_VAR':'VALUE_1', 'TEST_VAR2':'VALUE_2', 'TEST_VAR3':'VALUE_3'])
    
    then:
    created
    !processInstance.hasErrors()
    processInstance.id > 0
    processInstance.process == process
    processInstance.description == "Approving XYZ Widget"

    processInstance.params.get('TEST_VAR').equals('VALUE_1')
    processInstance.params.get('TEST_VAR2').equals('VALUE_2')
    processInstance.params.get('TEST_VAR3').equals('VALUE_3')
    processInstance.params.get('NOSUCH_VAR') == null
  }
  
  def "Initiate erronous process"() {
    setup:
    def testScript = new WorkflowScript(name:'TestScript', description:'A script used in testing', definition:'return true', creator:workflowProcessService.subject).save()
    minimalDefinition = new File('test/data/minimal-broken.pr').getText()

    expect:
    !testScript.hasErrors()
    
    when:   
    def (created, process) = workflowProcessService.create(minimalDefinition)
    
    then:
    !created
    process.hasErrors()
  }
  
  def "Update minimal process"() {
    setup:
    def testScript = new WorkflowScript(name:'TestScript', description:'A script used in testing', definition:'return true', creator:workflowProcessService.subject).save()
    minimalDefinition = new File('test/data/minimal.pr').getText()
    def updatedDefinition = new File('test/data/minimal2.pr').getText()
    workflowProcessService.create(minimalDefinition)

    expect:
    !testScript.hasErrors()
    
    when:   
    def (updated, process_) = workflowProcessService.update('Minimal Test Process', updatedDefinition)
    
    then:
    updated
    
    def process = Process.findWhere(name: 'Minimal Test Process', active: true)
    process == process_
    
    process.description == 'Minimal test process description mkII'
    process.processVersion == 2
    process.tasks.get(0).description == 'Description of task1 mkII'
    process.tasks.get(0).outcomes.get('testoutcome1 mkII') != null
    process.tasks.size() == 6
    Process.countByName('Minimal Test Process') == 2
  }
  
  def "Multiple updates to minimal process"() {
    setup:
    def testScript = new WorkflowScript(name:'TestScript', description:'A script used in testing', definition:'return true', creator:workflowProcessService.subject).save()
    minimalDefinition = new File('test/data/minimal.pr').getText()
    def updatedDefinition = new File('test/data/minimal2.pr').getText()
    def updatedDefinition2 = new File('test/data/minimal3.pr').getText()
    def updatedDefinition3 = new File('test/data/minimal4.pr').getText()
    workflowProcessService.create(minimalDefinition)

    expect:
    !testScript.hasErrors()
    
    when:   
    workflowProcessService.update('Minimal Test Process', updatedDefinition)
    workflowProcessService.update('Minimal Test Process', updatedDefinition2)
    workflowProcessService.update('Minimal Test Process', updatedDefinition3)
    
    then:
    def process = Process.findWhere(name: 'Minimal Test Process', active: true)
    process.description == 'Minimal test process description mkIV'
    process.processVersion == 4
    process.tasks.get(0).description == 'Description of task1 mkIV'
    process.tasks.get(0).outcomes.get('testoutcome1 mkIV') != null
    process.tasks.size() == 5
    Process.countByName('Minimal Test Process') == 4
  }
  
  def "Ensure initiate always utilizes newest process definition"() {
    setup:
    def testScript = new WorkflowScript(name:'TestScript', description:'A script used in testing', definition:'return true', creator:workflowProcessService.subject).save()
    minimalDefinition = new File('test/data/minimal.pr').getText()
    def updatedDefinition = new File('test/data/minimal2.pr').getText()
    workflowProcessService.create(minimalDefinition)
    def agent = new aaf.base.identity.Subject(principal:'http://test.com!http://sp.test.com!1234', cn:'Fred Bloggs', email:'fred@uni.edu.au', sharedToken:'LGW3wpNaPgwnLoYYsgh', active:true).save()
    def agent2 = new aaf.base.identity.Subject(principal:'http://test.com!http://sp.test.com!5678', cn:'Fredlina Bloggs', email:'fredlina@uni.edu.au', sharedToken:'LGW3wpNaPgwnXXXXX', active:true).save()

    expect:
    !testScript.hasErrors()
    !agent.hasErrors()
    !agent2.hasErrors()
    
    when:   
    def (initiated, processInstance) = workflowProcessService.initiate('Minimal Test Process', "Approving XYZ Widget", ProcessPriority.LOW, ['agent':agent, 'TEST_VAR':'VALUE_1', 'TEST_VAR2':'VALUE_2', 'TEST_VAR3':'VALUE_3'])
    def (updated, newProcess) = workflowProcessService.update('Minimal Test Process', updatedDefinition)
    def (initiated_, processInstance_) = workflowProcessService.initiate('Minimal Test Process', "Approving XYZ Widget", ProcessPriority.LOW, ['agent':agent2, 'TEST_VAR':'VALUE_1', 'TEST_VAR2':'VALUE_2', 'TEST_VAR3':'VALUE_3'])
    
    then:
    initiated
    updated
    initiated_
    
    Process.countByName('Minimal Test Process') == 2
    processInstance.process != processInstance_.process
    processInstance_.process == newProcess
    processInstance.agent != processInstance_.agent
    processInstance.process.description == 'Minimal test process description'
    processInstance_.process.description == 'Minimal test process description mkII'
  }
  
}
