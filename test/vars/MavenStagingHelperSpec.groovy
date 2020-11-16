import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import org.jenkinsci.plugins.workflow.steps.Step
import org.jenkinsci.plugins.workflow.steps.StepContext
import org.jenkinsci.plugins.workflow.steps.StepExecution

class MavenStagingHelperSpec extends JenkinsPipelineSpecification {
    def steps

    def setup() {
        steps = new Step() {
            @Override
            StepExecution start(StepContext stepContext) throws Exception {
                return null
            }
        }
    }

    def "[MavenCommand.groovy] stageLocalArtifacts"() {
        setup:
        def helper = new MavenStagingHelper(steps)
        when:
        helper.stageLocalArtifacts('ID', 'FOLDER')
        then:
        1 * getPipelineMock("sh")([script: 'mvn -B -q help:evaluate -Dexpression=project.artifactId -DforceStdout', returnStdout: true]) >> { return 'NAME' }
        1 * getPipelineMock("sh")([script: 'mvn -B -q help:evaluate -Dexpression=project.version -DforceStdout', returnStdout: true]) >> { return 'VS' }
        1 * getPipelineMock("sh")([script: 'mvn -B --projects NAME org.sonatype.plugins:nexus-staging-maven-plugin:1.6.5:deploy-staged-repository -DnexusUrl=https://repository.jboss.org/nexus -DserverId=jboss-releases-repository -DstagingDescription=\'NAME VS\' -DkeepStagingRepositoryOnCloseRuleFailure=true -DstagingProgressTimeoutMinutes=10 -DrepositoryDirectory=FOLDER -DstagingProfileId=ID', returnStdout: false])
        1 * getPipelineMock("sh")([script: 'find FOLDER -name *.properties', returnStdout: true]) >> { return 'file.properties' }
        1 * getPipelineMock("readProperties")([file: 'file.properties']) >> { return ['stagingRepository.id':'STAGING_ID'] }
        'STAGING_ID' == helper.stagingRepositoryId
    }

    def "[MavenCommand.groovy] stageLocalArtifacts empty stagingProfileId"() {
        setup:
        def helper = new MavenStagingHelper(steps)
        when:
        helper.stageLocalArtifacts('', 'FOLDER')
        then:
        thrown(AssertionError)
    }

    def "[MavenCommand.groovy] stageLocalArtifacts empty artifacts folder"() {
        setup:
        def helper = new MavenStagingHelper(steps)
        when:
        helper.stageLocalArtifacts('ID', '')
        then:
        thrown(AssertionError)
    }   
}
