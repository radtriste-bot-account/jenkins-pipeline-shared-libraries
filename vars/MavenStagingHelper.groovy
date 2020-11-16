def class MavenStagingHelper {

    def steps

    MavenCommand mvnCommand

    String nexusReleaseUrl = 'https://repository.jboss.org/nexus'
    String nexusReleaseRepositoryId = 'jboss-releases-repository'

    // If not defined, will retrieve default from project artifactId & version
    String stagingDescription

    // Will be filled once `stageLocalArtifacts` is called or via the `withStagingRepositoryId` method
    String stagingRepositoryId

    MavenStagingHelper(steps){
        this(steps, new MavenCommand(steps))
    }

    MavenStagingHelper(steps, mvnCommand){
        this.steps = steps
        this.mvnCommand = mvnCommand
    }

    // Return a Map of staging properties
    Map stageLocalArtifacts(String stagingProfileId, String localArtifactsFolder){
        assert stagingProfileId: 'Please provide a stagingProfileId'
        assert localArtifactsFolder: 'Please provide a local folder where artifacts are stored'
        
        getDefaultMavenCommand()
            .withProperty('keepStagingRepositoryOnCloseRuleFailure', true)
            .withProperty('stagingProgressTimeoutMinutes', 10)
            .withProperty('repositoryDirectory', localArtifactsFolder)
            .withProperty('stagingProfileId', stagingProfileId)
            .run('org.sonatype.plugins:nexus-staging-maven-plugin:1.6.5:deploy-staged-repository')

        // Retrieve `stagingRepositoryId` and fill it
        Map stagingProps = retrieveStagingProperties(localArtifactsFolder)
        withStagingRepositoryId(stagingProps['stagingRepository.id'])
        
        return stagingProps
    }

    def promoteStagingRepository(String buildPromotionProfileId) {
        assert this.stagingRepositoryId: 'Please provide a stagingRepositoryId via staging local artifacts or via withStagingRepositoryId method'
        assert buildPromotionProfileId: 'Please provide a buildPromotionProfileId'

        getDefaultMavenCommand()
            .withProperty('buildPromotionProfileId', buildPromotionProfileId)
            .withProperty('stagingRepositoryId', this.stagingRepositoryId)
            .run('org.sonatype.plugins:nexus-staging-maven-plugin:1.6.5:promote')
    }

    MavenCommand getDefaultMavenCommand(){
        String projectName = getProjectArtifactId()
        String projectVersion = getProjectVersion()

        return this.mvnCommand.clone()
            .withOptions(["--projects :${}"])
            .withProperty('nexusUrl', this.nexusReleaseUrl)
            .withProperty('serverId', this.nexusReleaseRepositoryId)
            .withProperty('stagingDescription', stagingDescription ?: "'${projectName} ${projectVersion}'")
    }

    MavenStagingHelper withNexusReleaseUrl(String nexusReleaseUrl) {
        this.nexusReleaseUrl = nexusReleaseUrl
        return this
    }

    MavenStagingHelper withNexusReleaseRepositoryId(String nexusReleaseRepositoryId) {
        this.nexusReleaseRepositoryId = nexusReleaseRepositoryId
        return this
    }

    MavenStagingHelper withStagingDescription(String stagingDescription) {
        this.stagingDescription = stagingDescription
        return this
    }

    MavenStagingHelper withStagingRepositoryId(String stagingRepositoryId) {
        this.stagingRepositoryId = stagingRepositoryId
        return this
    }

    Properties retrieveStagingProperties(String folder){
        String repositoryPropsFile = steps.sh(script: "find ${folder} -name *.properties", returnStdout: true).trim()
        echo "Got staging properties file ${repositoryPropsFile}"
        assert repositoryPropsFile: 'No staging properties file has been found'
        
        return steps.readProperties(file: repositoryPropsFile)
    }

    String getProjectArtifactId() {
        return executeMavenHelpEvaluateCommand('project.artifactId')
    }
    String getProjectVersion() {
        return executeMavenHelpEvaluateCommand('project.version')
    }
    String executeMavenHelpEvaluateCommand(String expression){
        return new MavenCommand(steps)
            .withOptions(['-q'])
            .withProperty('expression', expression)
            .withProperty('forceStdout')
            .returnOutput()
            .run('help:evaluate')
    }


}