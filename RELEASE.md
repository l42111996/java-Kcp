# Release to Maven Central

This project publishes through Sonatype Central Portal.

## One-time setup

1. Create or sign in to a Sonatype Central Portal account: https://central.sonatype.com/
2. Verify ownership of the namespace `com.github.l42111996`.
3. Generate a Central Portal user token.
4. Install GPG locally and create or import a signing key.
5. Add the Central token to `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>YOUR_CENTRAL_TOKEN_USERNAME</username>
      <password>YOUR_CENTRAL_TOKEN_PASSWORD</password>
    </server>
  </servers>
</settings>
```

## Build checks

```bash
mvn clean verify
```

## Release

Set the project version in the root `pom.xml` and all child module parent versions to the same release version.

```bash
mvn -Prelease clean deploy
```

The release profile creates source jars, javadoc jars, GPG signatures, and uploads the deployment bundle to Central Portal. `autoPublish` is disabled, so review and publish the deployment in the Central Portal UI.

## Modules

The reactor currently includes:

- `kcp-fec`
- `kcp-base`
- `kcp-example`
- `kcp-lockStepSynchronization`

If only the library artifacts should be published, remove example/demo modules from the root `pom.xml` release reactor before deploying.

## Troubleshooting

If Central Portal reports `Namespace 'com.github.l42111996' is not allowed`, the token's account has not been granted publishing rights for that namespace. Add and verify the namespace in Central Portal, then run the release command again.
