# Contribution Guidelines

## Code Formatting and Javadoc

Before submitting a PR, please run the following commands to ensure proper formatting and Javadoc processing

```
./mvnw spring-javaformat:apply javadoc:javadoc -Pjavadoc
```

The `-Pjavadoc` is a profile that enables Javadoc processing so as to avoid a long build time when developing.

<dl><dt><strong>📌 NOTE</strong></dt><dd>

We use the [Spring JavaFormat](https://github.com/spring-io/spring-javaformat) project to apply code formatting conventions as well as checkstyle rules for many of our code conventions.

The code can also be formatted from your IDE when the formatter plugin [has been installed](https://github.com/spring-projects-experimental/spring-grpc/wiki/Working-with-the-Code#install-the-spring-formatter-plugin).
</dd></dl>

## Contributing a New GRPC Features

To contribute a new feature, adhere to the following steps:

1. **Implement Auto-Configuration and a Spring Boot Starter**: This step involves creating the
necessary auto-configuration and Spring Boot Starter to easily instantiate the new model with
Spring Boot applications.
2. **Write Tests**: All new classes should be accompanied by comprehensive tests.
Existing tests can serve as a useful reference for structuring and implementing your tests.
3. **Document Your Contribution**: Ensure your documentation follows the existing format,
using the `spring-javaformat` plugin to format your code and Javadoc comments.

By following these guidelines, we can greatly expand the framework’s range of supported models
while following a common implementation and documentation pattern.

## How to Contribute

### Discuss

If you have a question, check [StackOverflow](https://stackoverflow.com/tags/spring) using the [grpc-java](https://stackoverflow.com/tags/grpc-java) tag.
Find an existing discussion or start a new one if necessary.

If you suspect an issue, perform a search in the GitHub tracker of the Spring gRPC project, using a few different keywords.
When you find related issues and discussions, prior or current, it helps you to learn and it helps us to make a decision.

### Create a Ticket

Reporting an issue or making a feature request is a great way to contribute.
Your feedback and the conversations that result from it provide a continuous flow of ideas.

Before you create a ticket, please take the time to [research first](#discuss).

If creating a ticket after a discussion on StackOverflow or the Mailing List, please provide a self-sufficient description in the ticket.
We understand this is extra work but the issue tracker is an important place of record for design discussions and decisions that can often be referenced long after the fix version, for example to revisit decisions, to understand the origin of a feature, and so on.

When ready create a ticket in GitHub.

### Ticket Lifecycle

When an issue is first created, it may not be assigned and will not have a fix version.
Within a day or two, the issue is assigned to a specific committer.
The committer will then review the issue, ask for further information if needed, and based on the findings, the issue is either assigned a fix
version or rejected.

When a fix is ready, the issue is marked "Resolved" and may still be re-opened.
Once the fix is released, you will need to create a new, related ticket with a fresh description, if necessary.

## Submit a Pull Request

You can contribute a source code change by submitting a pull request.

1. You must have the right to submit code changes. Spring gRPC follows [Developer Certificate of Origin (DCO)](https://developercertificate.org/) rules. Commit messages must contain a `Signed-off-by` line. Git even has a `-s` command line option to append this automatically to your commit message:

   ```
   This is my commit message

   Signed-off-by: Random J Developer <random@developer.example.org>

   [resolves #1234]
   ```

   You will also be reminded automatically when you submit a pull request.
2. For all but the most trivial of contributions, please [create a ticket](#create-a-ticket).
The purpose of the ticket is to understand and discuss the underlying issue or feature.
We use the GitHub issue tracker as the preferred place of record for conversations and conclusions.
In that sense discussions directly under a PR are more implementation detail oriented and transient in nature.
3. Always check out the `main` branch and submit pull requests against it.
Backports to prior versions will be considered on a case-by-case basis and reflected as the fix version in the issue tracker.
4. Use short branch names, preferably based on the GitHub issue (e.g. `gh-1234`), or otherwise using succinct, lower-case, dash (-) delimited names, such as `fix-warnings`.
5. Choose the granularity of your commits consciously and squash commits that represent multiple edits or corrections of the same logical change.
See [Rewriting History section of Pro Git](https://git-scm.com/book/en/Git-Tools-Rewriting-History) for an overview of streamlining commit history.
6. Format commit messages using 55 characters for the subject line, 72 lines for the description, followed by related issues, e.g. `[resolves #1234]`
See the [Commit Guidelines section of Pro Git](https://git-scm.com/book/en/Distributed-Git-Contributing-to-a-Project#Commit-Guidelines) for best practices around commit messages and use `git log` to see some examples.
7. List the GitHub issue number in the PR description.

If accepted, your contribution may be heavily modified as needed prior to merging.
You will likely retain author attribution for your Git commits granted that the bulk of your changes remain intact.
You may also be asked to rework the submission.

If asked to make corrections, simply push the changes against the same branch, and your pull request will be updated.
In other words, you do not need to create a new pull request when asked to make changes.
