plugins { id("com.shinami.codeartifact-login") }

codeArtifact { domains { register("shinami") { region.set("us-east-1") } } }

repositories {
    codeArtifact.maven {
        name.set("aws")
        domain.set("shinami")
        repository.set("maven-release")
    }
}
