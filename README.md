# gradle-codeartifact-login

Gradle plugin to manage AWS CodeArtifact login tokens.

## Getting started

Add the following to your `build.gradle.kts` Kotlin script:

```kotlin
plugins {
    id("com.shinami.codeartifact-login") version "0.1.0"
}

codeArtifact {
    domains {
        register("<codeartifact_domain>") {
            owner.set("<aws_account>")  // optional
            region.set("<aws_region>")  // optional
        }
    }
}

repositories {
    codeArtifact.maven {
        name.set("ca") // or any other name
        domain.set("<codeartifact_domain>")
        repository.set("<codeartifact_repository>")
    }
}
```

or the equivalent `build.gradle` Groovy script:

```groovy
plugins {
    id "com.shinami.codeartifact-login" version "0.1.0"
}

codeArtifact {
    domains {
        "<codeartifact_domain>" {
            owner = "<aws_account>"  // optional
            region = "<aws_region>"  // optional
        }
    }
}

repositories {
    codeArtifact.maven {
        name = "ca" // or any other name
        domain = "<codeartifact_domain>"
        repository = "<codeartifact_repository>"
    }
}
```

Then you can run the `codeArtifactLogin` task to generate login tokens for your declared repositories, under `.codeartifact/` dir (configurable through `codeArtifact.credentialsDir` setting).
The tokens will be valid (and reused by the task) for 12 hours, after which you'll need to run `codeArtifactLogin` again to refresh them.

Note that running `codeArtifactLogin` requires [AWS CLI v2](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html) to be installed and configured.
However, once the tokens are generated (and remain valid), all other Gradle tasks can function without this requirement.
This is designed to support the workflow where tokens are generated on a host machine, and then copied to another environment (e.g. Docker builder) to enable access to the repository.

## Publishing to CodeArtifact repository

You can set the `ca` repository declared above as the publishing repository too, in order to publish to it:

```kotlin
publishing {
    repositories {
        add(project.repositories["ca"])
    }
}
```
